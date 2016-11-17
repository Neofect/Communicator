package com.neofect.communicator.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
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

	public UsbConnection(Context context, UsbDevice device, CommunicationController<? extends Device> controller) {
		super(ConnectionType.USB_SERIAL, controller);
		this.context = context.getApplicationContext();
		this.device = device;
	}

	private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (UsbConnection.this.device == device) {
					// call your method that cleans up and closes communication with the device
					Log.i(LOG_TAG, "Device is detached! device=" + device);
				}
			}  else if (ACTION_USB_PERMISSION.equals(action)) {
				UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					Log.d(LOG_TAG, "Permission granted for the device " + device);
					if(device != null){
						handleConnecting();
						onPermissionGranted(device);
					}
				}  else {
					Log.d(LOG_TAG, "Permission denied for the device " + device);
				}
			}
		}
	};

	@Override
	public void connect() {
		// Request user for a permission for connecting to the USB.
		registerReceiver();
		PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
		Log.d(LOG_TAG, "Request permission for the device " + device);
		final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		usbManager.requestPermission(device, intent);
	}

	private void registerReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		context.registerReceiver(usbReceiver, filter);
	}

	@Override
	public void disconnect() {
		context.unregisterReceiver(usbReceiver);
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

	private void onPermissionGranted(UsbDevice device) {
		try {
			open(device);
			setParameters(115200, 8, UsbSerialPortConstants.STOPBITS_1, UsbSerialPortConstants.PARITY_NONE);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error setting up device: " + e.getMessage(), e);
//			mTitleTextView.setText("Error opening device: " + e.getMessage());
//			try {
//				sPort.close();
//			} catch (IOException e2) {
//				Log.e(LOG_TAG, "Failed to close the port!", e);
//			}
//			sPort = null;
			return;
		}

		startReadThread();
		handleConnected();
	}

	private void startReadThread() {
		new Thread(readRunnable).start();
	}




	public void open(UsbDevice device) throws IOException {
		if (usbConnection != null) {
			throw new IOException("Already opened.");
		}

		final UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		usbConnection = usbManager.openDevice(device);
		boolean opened = false;
		try {
			for (int i = 0; i < device.getInterfaceCount(); i++) {
				UsbInterface usbIface = device.getInterface(i);
				if (usbConnection.claimInterface(usbIface, true)) {
					Log.d(LOG_TAG, "claimInterface " + i + " SUCCESS");
				} else {
					Log.d(LOG_TAG, "claimInterface " + i + " FAIL");
				}
			}

			UsbInterface dataIface = device.getInterface(device.getInterfaceCount() - 1);
			for (int i = 0; i < dataIface.getEndpointCount(); i++) {
				UsbEndpoint ep = dataIface.getEndpoint(i);
				if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
						readEndpoint = ep;
					} else {
						writeEndpoint = ep;
					}
				}
			}

			setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_ENABLE);
			setConfigSingle(SILABSER_SET_MHS_REQUEST_CODE, MCR_ALL | CONTROL_WRITE_DTR | CONTROL_WRITE_RTS);
			setConfigSingle(SILABSER_SET_BAUDDIV_REQUEST_CODE, BAUD_RATE_GEN_FREQ / DEFAULT_BAUD_RATE);
			opened = true;
		} finally {
			if (!opened) {
				try {
					close();
				} catch (IOException e) {
					// Ignore IOExceptions during close()
				}
			}
		}
	}

	public void close() throws IOException {
		if (usbConnection == null) {
			throw new IOException("Already closed");
		}
		try {
			setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_DISABLE);
			usbConnection.close();
		} finally {
			usbConnection = null;
		}
	}

	private static final int DEFAULT_BAUD_RATE = 9600;
	private static final int REQTYPE_HOST_TO_DEVICE = 0x41;
	private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;
	private static final int UART_ENABLE = 0x0001;
	private static final int UART_DISABLE = 0x0000;
	private static final int BAUD_RATE_GEN_FREQ = 0x384000;
	private static final int SILABSER_IFC_ENABLE_REQUEST_CODE = 0x00;
	private static final int SILABSER_SET_BAUDDIV_REQUEST_CODE = 0x01;
	private static final int SILABSER_SET_LINE_CTL_REQUEST_CODE = 0x03;
	private static final int SILABSER_SET_MHS_REQUEST_CODE = 0x07;
	private static final int SILABSER_SET_BAUDRATE = 0x1E;
	private static final int SILABSER_FLUSH_REQUEST_CODE = 0x12;
	private static final int MCR_ALL = 0x0003;
	private static final int CONTROL_WRITE_DTR = 0x0100;
	private static final int CONTROL_WRITE_RTS = 0x0200;

	private int setConfigSingle(int request, int value) {
		return usbConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value,
				0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
	}

	private void setBaudRate(int baudRate) throws IOException {
		byte[] data = new byte[] {
				(byte) ( baudRate & 0xff),
				(byte) ((baudRate >> 8 ) & 0xff),
				(byte) ((baudRate >> 16) & 0xff),
				(byte) ((baudRate >> 24) & 0xff)
		};
		int ret = usbConnection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_BAUDRATE,
				0, 0, data, 4, USB_WRITE_TIMEOUT_MILLIS);
		if (ret < 0) {
			throw new IOException("Error setting baud rate.");
		}
	}


	private void setParameters(int baudRate, int dataBits, int stopBits, int parity)
			throws IOException {
		setBaudRate(baudRate);

		int configDataBits = 0;
		switch (dataBits) {
			case UsbSerialPortConstants.DATABITS_5:
				configDataBits |= 0x0500;
				break;
			case UsbSerialPortConstants.DATABITS_6:
				configDataBits |= 0x0600;
				break;
			case UsbSerialPortConstants.DATABITS_7:
				configDataBits |= 0x0700;
				break;
			case UsbSerialPortConstants.DATABITS_8:
				configDataBits |= 0x0800;
				break;
			default:
				configDataBits |= 0x0800;
				break;
		}

		switch (parity) {
			case UsbSerialPortConstants.PARITY_ODD:
				configDataBits |= 0x0010;
				break;
			case UsbSerialPortConstants.PARITY_EVEN:
				configDataBits |= 0x0020;
				break;
		}

		switch (stopBits) {
			case UsbSerialPortConstants.STOPBITS_1:
				configDataBits |= 0;
				break;
			case UsbSerialPortConstants.STOPBITS_2:
				configDataBits |= 2;
				break;
		}
		setConfigSingle(SILABSER_SET_LINE_CTL_REQUEST_CODE, configDataBits);
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
