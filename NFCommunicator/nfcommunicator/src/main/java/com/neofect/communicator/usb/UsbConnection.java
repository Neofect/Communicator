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
	private BroadcastReceiver usbEventReceiver;

	public UsbConnection(Context context, UsbDevice device, CommunicationController<? extends Device> controller) {
		super(ConnectionType.USB_SERIAL, controller);
		this.context = context.getApplicationContext();
		this.device = device;
	}

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
		return getDeviceName() + "(" + getRemoteAddress() + ")";
	}

	@Override
	public void connect() {
		Log.d(LOG_TAG, "connect:");
		if (deviceConnection != null) {
			Log.e(LOG_TAG, "connect: Already connected!");
			return;
		} else if (device.getInterfaceCount() == 0) {
			// 2016.12.17 neo.kim@neofect.com
			// If you got here and you don't know why, the following link might be helpful.
			// https://code.google.com/p/android/issues/detail?id=159529
			throw new UsbDeviceHasNoInterfaceException("The USB device(" + device.getDeviceName() + ") has no USB interface!");
		}
		registerReceiver();

		final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		if (usbManager.hasPermission(device)) {
			startConnecting();
		} else {
			// Ask permission for USB connection
			PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
			Log.d(LOG_TAG, "Requesting permission for USB device '" + device.getDeviceName() + "'...");
			usbManager.requestPermission(device, intent);
		}
	}

	@Override
	public void disconnect() {
		Log.d(LOG_TAG, "disconnect:");
		if (deviceConnection == null) {
			Log.e(LOG_TAG, "disconnect: Already disconnected");
			return;
		}
		cleanUp();
		handleDisconnected();
	}

	private void cleanUp() {
		String message = "cleanUp:";
		if (device != null) {
			message += " device=" + getDescription();
		}
		Log.d(LOG_TAG, message);
		if (readThread != null) {
			readThread.interrupt();
			readThread = null;
		}

		if (usbEventReceiver != null) {
			synchronized (usbEventReceiver) {
				try {
					context.unregisterReceiver(usbEventReceiver);
					Log.d(LOG_TAG, "cleanUp: Receiver unregistered.");
				} catch (IllegalArgumentException e) {
					Log.e(LOG_TAG, "cleanUp: Failed to unregister the receiver!", e);
				}
				usbEventReceiver = null;
			}
		}

		try {
			driver.close();
		} catch (Exception e) {
			Log.e(LOG_TAG, "", e);
		} finally {
			driver = null;
			deviceConnection = null;
		}
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
		} catch (Exception e) {
			Log.e(LOG_TAG, "Failed to connect to USB device(" + device.getDeviceName() + "). cause=" + e.getMessage());
			cleanUp();
			handleFailedToConnect(e);
			return;
		}

		// Start a thread for reading
		readThread = new UsbReadThread();
		readThread.start();

		Log.i(LOG_TAG, "USB device is connected! connection=" + getDescription());
		handleConnected();
	}

	private synchronized void registerReceiver() {
		if (usbEventReceiver != null) {
			Log.e(LOG_TAG, "USB event receiver is already registered!");
			return;
		}
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		usbEventReceiver = createReceiver();
		context.registerReceiver(usbEventReceiver, filter);
		Log.d(LOG_TAG, "USB event receiver is registered.");
	}

	private BroadcastReceiver createReceiver() {
		return new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (device == null || !UsbConnection.this.device.equals(device)) {
					return;
				}
				String action = intent.getAction();
				Log.d(LOG_TAG, "USB event is received. action=" + action + ", device=" + UsbConnection.this.getDescription());

				if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
					Log.i(LOG_TAG, "USB device is detached. device=" + device);
					disconnect();
				} else if (ACTION_USB_PERMISSION.equals(action)) {
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						Log.d(LOG_TAG, "Permission granted for the device " + device);
						startConnecting();
					}  else {
						Log.d(LOG_TAG, "Permission denied for the device " + device);
					}
				}
			}
		};
	}

	@Override
	public void write(byte[] src) {
		if (!isConnected()) {
			Log.e(LOG_TAG, "write: Not connected!");
			return;
		}

		try {
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
				Log.d(LOG_TAG, "UsbConnection: write: numberOfWrittenBytes=" + numberOfWrittenBytes + " attempted=" + writeLength);
				offset += numberOfWrittenBytes;
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "write()", e);
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
						Log.e(LOG_TAG, "UsbReadThread: run: Failed queueing request!");
						disconnect();
						break;
					}

					final UsbRequest response = deviceConnection.requestWait();
					if (response == null) {
						Log.e(LOG_TAG, "UsbReadThread: run: requestWait() returned null!");
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
				Log.e(LOG_TAG, "UsbReadThread: run:", e);
				disconnect();
			}
			request.close();
		}
	}

}
