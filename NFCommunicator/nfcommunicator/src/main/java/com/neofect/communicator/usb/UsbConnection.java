package com.neofect.communicator.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.util.Log;

import com.neofect.communicator.CommunicationController;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Device;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author neo.kim@neofect.com
 * @date Nov 16, 2016
 */
public class UsbConnection extends Connection {

	private static final String LOG_TAG = UsbConnection.class.getSimpleName();

	private static final String ACTION_USB_PERMISSION = "com.neofect.communicator.USB_PERMISSION";

	private static final int WRITE_TIMEOUT_MILLIS = 200;
	private static final int READ_BUFFER_SIZE = 16 * 1024;
	private static final int WRITE_BUFFER_SIZE = 16 * 1024;

	private final Object mWriteBufferLock = new Object();
	private byte[] mWriteBuffer = new byte[WRITE_BUFFER_SIZE];

	private Context context;
	private UsbDevice device;
	private UsbDeviceConnection deviceConnection;
	private UsbEndpoint readEndpoint;
	private UsbEndpoint writeEndpoint;
	private UsbSerialDriver driver;
	private Thread readThread;
	private boolean receiverRegistered = false;

	public UsbConnection(Context context, UsbDevice device, CommunicationController<? extends Device> controller) {
		super(ConnectionType.USB_SERIAL, controller);
		this.context = context.getApplicationContext();
		this.device = device;
	}

	private final BroadcastReceiver usbEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (UsbConnection.this.device.equals(device)) {
					Log.i(LOG_TAG, "USB device is detached. device=" + device);
					if (isConnected()) {
						disconnect();
					}
				}
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					Log.d(LOG_TAG, "Permission granted for the device " + device);
					startConnecting();
				}  else {
					Log.d(LOG_TAG, "Permission denied for the device " + device);
				}
			}
		}
	};

	@Override
	public String getDeviceName() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return device.getProductName();
		} else {
			return device.getDeviceName();
		}
	}

	@Override
	public String getRemoteAddress() {
		return device.getDeviceName();
	}

	@Override
	public String getDescription() {
		return getDeviceName();
	}

	@Override
	public void connect() {
		if (deviceConnection != null) {
			Log.e(LOG_TAG, "connect() Already connected!");
			return;
		}

		// Ask permission for USB connection
		registerReceiver();
		PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		Log.d(LOG_TAG, "Requesting permission for USB device '" + device.getDeviceName() + "'...");
		final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		usbManager.requestPermission(device, intent);
	}

	@Override
	public void disconnect() {
		if (deviceConnection == null) {
			Log.e(LOG_TAG, "disconnect() Already disconnected");
			return;
		}

		if (readThread != null) {
			readThread.interrupt();
			readThread = null;
		}

		if (receiverRegistered) {
			context.unregisterReceiver(usbEventReceiver);
			receiverRegistered = false;
		}

		try {
			driver.close();
		} catch (Exception e) {
			Log.e(LOG_TAG, "", e);
		} finally {
			driver = null;
			deviceConnection = null;
		}

		handleDisconnected();
	}

	private void startConnecting() {
		handleConnecting();
		try {
			UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			deviceConnection = usbManager.openDevice(device);

			driver = UsbSerialDriverFactory.createDriver(device, deviceConnection);
			UsbEndpoint[] endpoints = driver.open();
			readEndpoint = endpoints[0];
			writeEndpoint = endpoints[1];

			driver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error setting up device: " + e.getMessage(), e);
			return;
		}

		// Start a thread for reading
		readThread = new UsbReadThread();
		readThread.start();

		handleConnected();
	}

	private void registerReceiver() {
		if (receiverRegistered) {
			Log.e(LOG_TAG, "registerReceiver() Already registered!");
			return;
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		context.registerReceiver(usbEventReceiver, filter);
		receiverRegistered = true;
	}

	@Override
	public void write(byte[] src) {
		int offset = 0;
		while (offset < src.length) {
			final int writeLength;
			final int numberOfWrittenBytes;

			synchronized (mWriteBufferLock) {
				final byte[] writeBuffer;

				writeLength = Math.min(src.length - offset, mWriteBuffer.length);
				if (offset == 0) {
					writeBuffer = src;
				} else {
					// bulkTransfer does not support offsets, make a copy.
					System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
					writeBuffer = mWriteBuffer;
				}

				numberOfWrittenBytes = deviceConnection.bulkTransfer(writeEndpoint, writeBuffer, writeLength, WRITE_TIMEOUT_MILLIS);
			}
			if (numberOfWrittenBytes <= 0) {
				Log.e(LOG_TAG, "Error writing " + writeLength + " bytes at offset " + offset + " length=" + src.length);
				return;
			}
			Log.d(LOG_TAG, "UsbConnection.write() numberOfWrittenBytes=" + numberOfWrittenBytes + " attempted=" + writeLength);
			offset += numberOfWrittenBytes;
		}
	}

	private class UsbReadThread extends Thread {
		@Override
		public void run() {
			byte[] buffer = new byte[READ_BUFFER_SIZE];
			final UsbRequest request = new UsbRequest();
			request.initialize(deviceConnection, readEndpoint);
			try {
				while (UsbConnection.this.isConnected()) {
					ByteBuffer buf = ByteBuffer.wrap(buffer);
					if (!request.queue(buf, buffer.length)) {
						Log.e(LOG_TAG, "UsbReadThread.run() Failed to queueing request!");
						disconnect();
						break;
					}

					final UsbRequest response = deviceConnection.requestWait();
					if (response == null) {
						Log.e(LOG_TAG, "UsbReadThread.run() requestWait() returned null!");
						disconnect();
						break;
					}

					int numberOfBytes = buf.position();
					if (numberOfBytes > 0) {
						byte[] readData = new byte[numberOfBytes];
						System.arraycopy(buffer, 0, readData, 0, numberOfBytes);
						handleReadData(readData);
					}
				}
			} catch (Exception e) {
				Log.e(LOG_TAG, "run()", e);
				disconnect();
			}
			request.close();
		}
	}

}