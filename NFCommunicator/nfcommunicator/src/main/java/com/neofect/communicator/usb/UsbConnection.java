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

import static android.content.ContentValues.TAG;

/**
 * @author neo.kim@neofect.com
 * @date Nov 16, 2016
 */
public class UsbConnection extends Connection {

	private static final String LOG_TAG = UsbConnection.class.getSimpleName();

	private static final String ACTION_USB_PERMISSION = "com.android.permission.USB_PERMISSION";

	private Context context;
	private UsbDevice device;
	private UsbDeviceConnection usbConnection;
	private UsbEndpoint readEndpoint;
	private UsbEndpoint writeEndpoint;
	private UsbSerialDriver driver;

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
				if (UsbConnection.this.device == device) {
					// call your method that cleans up and closes communication with the device
					Log.i(LOG_TAG, "Device is detached! device=" + device);
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
	public void connect() {
		// Ask for permission for USB connection
		registerReceiver();
		PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		Log.d(LOG_TAG, "Requesting permission for USB device '" + getRemoteAddress() + "'...");
		final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		usbManager.requestPermission(device, intent);
	}

	private void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		context.registerReceiver(usbEventReceiver, filter);
	}

	@Override
	public void disconnect() {
		context.unregisterReceiver(usbEventReceiver);
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
		return device.getDeviceName();
	}


	private static final int TIMEOUT_MILLIS = 200;
	private static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
	private static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;

	protected final Object mWriteBufferLock = new Object();
	protected byte[] mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];

	protected final Object mReadBufferLock = new Object();
	protected byte[] mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];

	@Override
	public void write(byte[] src) {
		int offset = 0;

		while (offset < src.length) {
			final int writeLength;
			final int amtWritten;

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

				amtWritten = usbConnection.bulkTransfer(writeEndpoint, writeBuffer, writeLength, TIMEOUT_MILLIS);
			}
			if (amtWritten <= 0) {
//				throw new IOException("Error writing " + writeLength + " bytes at offset " + offset + " length=" + src.length);
				Log.e(LOG_TAG, "Error writing " + writeLength + " bytes at offset " + offset + " length=" + src.length);
				return;
			}

			Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
			offset += amtWritten;
		}
	}

	private void startConnecting() {
		handleConnecting();
		try {
			open();
			driver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error setting up device: " + e.getMessage(), e);
			return;
		}

		startReadThread();
		handleConnected();
	}

	private void startReadThread() {
		new Thread(readRunnable).start();
	}

	private void open() throws IOException {
		if (usbConnection != null) {
			throw new IOException("Already opened.");
		}

		final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		usbConnection = usbManager.openDevice(device);

		driver = UsbSerialDriverFactory.createDriver(device, usbConnection);
		UsbEndpoint[] endpoints = driver.open();
		readEndpoint = endpoints[0];
		writeEndpoint = endpoints[1];
	}

	public void close() throws IOException {
		if (usbConnection == null) {
			throw new IOException("Already closed");
		}
		try {
			driver.close();
		} finally {
			usbConnection = null;
		}
	}

	private Runnable readRunnable = new Runnable() {
		@Override
		public void run() {
			final UsbRequest request = new UsbRequest();
			request.initialize(usbConnection, readEndpoint);
			while (true) {
				try {
					ByteBuffer buf = ByteBuffer.wrap(mReadBuffer);
					if (!request.queue(buf, mReadBuffer.length)) {
						Log.e(LOG_TAG, "Error queueing request.");
						continue;
					}

					final UsbRequest response = usbConnection.requestWait();
					if (response == null) {
						Log.e(LOG_TAG, "Null response");
						continue;
					}

					int nread = buf.position();
					if (nread > 0) {
						byte[] readData = new byte[nread];
						System.arraycopy(mReadBuffer, 0, readData, 0, nread);
						handleReadData(mReadBuffer);
					}
				} catch (Exception e) {
					Log.e(LOG_TAG, "run()", e);
					break;
				}
			}
			request.close();
		}
	};

}
