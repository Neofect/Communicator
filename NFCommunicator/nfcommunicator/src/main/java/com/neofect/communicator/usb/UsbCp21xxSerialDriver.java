package com.neofect.communicator.usb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Following class is copied and modified from UsbSerialForAndroid project. (https://github.com/mik3y/usb-serial-for-android)
 *
 * Original source :
 * https://github.com/mik3y/usb-serial-for-android/blob/4ccaff47b16047375bcb77e67954ff95e314b612/usbSerialForAndroid/src/main/java/com/hoho/android/usbserial/driver/Cp21xxSerialDriver.java
 *
 * @author neo.kim@neofect.com
 * @date Nov 29, 2016
 */
public class UsbCp21xxSerialDriver extends UsbSerialDriver {

	private static final int DEFAULT_BAUD_RATE = 9600;

	private static final int USB_WRITE_TIMEOUT_MILLIS = 5000;

	/*
	 * Configuration Request Types
	 */
	private static final int REQTYPE_HOST_TO_DEVICE = 0x41;

	/*
	 * Configuration Request Codes
	 */
	private static final int SILABSER_IFC_ENABLE_REQUEST_CODE = 0x00;
	private static final int SILABSER_SET_BAUDDIV_REQUEST_CODE = 0x01;
	private static final int SILABSER_SET_LINE_CTL_REQUEST_CODE = 0x03;
	private static final int SILABSER_SET_MHS_REQUEST_CODE = 0x07;
	private static final int SILABSER_SET_BAUDRATE = 0x1E;
	private static final int SILABSER_FLUSH_REQUEST_CODE = 0x12;

	private static final int FLUSH_READ_CODE = 0x0a;
	private static final int FLUSH_WRITE_CODE = 0x05;

	/*
	 * SILABSER_IFC_ENABLE_REQUEST_CODE
	 */
	private static final int UART_ENABLE = 0x0001;
	private static final int UART_DISABLE = 0x0000;

	/*
	 * SILABSER_SET_BAUDDIV_REQUEST_CODE
	 */
	private static final int BAUD_RATE_GEN_FREQ = 0x384000;

	/*
	 * SILABSER_SET_MHS_REQUEST_CODE
	 */
	private static final int MCR_DTR = 0x0001;
	private static final int MCR_RTS = 0x0002;
	private static final int MCR_ALL = 0x0003;

	private static final int CONTROL_WRITE_DTR = 0x0100;
	private static final int CONTROL_WRITE_RTS = 0x0200;

	UsbCp21xxSerialDriver(UsbDevice device, UsbDeviceConnection connection) {
		super(device, connection);
	}

	private int setConfigSingle(int request, int value) {
		return connection.controlTransfer(REQTYPE_HOST_TO_DEVICE, request, value,
				0, null, 0, USB_WRITE_TIMEOUT_MILLIS);
	}

	@Override
	public UsbEndpoint[] open() throws IOException {
		if (device.getInterfaceCount() == 0) {
			throw new RuntimeException("The device has no interface for USB!");
		}

		UsbEndpoint[] endpoints = new UsbEndpoint[2];

		boolean opened = false;
		try {
			for (int i = 0; i < device.getInterfaceCount(); i++) {
				UsbInterface usbIface = device.getInterface(i);
				if (connection.claimInterface(usbIface, true)) {
					Log.d(TAG, "claimInterface " + i + " SUCCESS");
				} else {
					Log.d(TAG, "claimInterface " + i + " FAIL");
				}
			}

			UsbInterface dataIface = device.getInterface(device.getInterfaceCount() - 1);
			for (int i = 0; i < dataIface.getEndpointCount(); i++) {
				UsbEndpoint ep = dataIface.getEndpoint(i);
				if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
						endpoints[0] = ep;
					} else {
						endpoints[1] = ep;
					}
				}
			}

			setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_ENABLE);
			setConfigSingle(SILABSER_SET_MHS_REQUEST_CODE, MCR_ALL | CONTROL_WRITE_DTR | CONTROL_WRITE_RTS);
			setConfigSingle(SILABSER_SET_BAUDDIV_REQUEST_CODE, BAUD_RATE_GEN_FREQ / DEFAULT_BAUD_RATE);
			opened = true;
		} catch (Exception e) {
			throw new IOException("Failed to open endpoints!", e);
		} finally {
			if (!opened) {
				try {
					close();
				} catch (IOException e) {
					// Ignore IOExceptions during close()
				}
			}
		}
		return endpoints;
	}

	@Override
	public void close() throws IOException {
		if (connection == null) {
			throw new IOException("Already closed");
		}
		setConfigSingle(SILABSER_IFC_ENABLE_REQUEST_CODE, UART_DISABLE);
		connection.close();
	}

	private void setBaudRate(int baudRate) throws IOException {
		byte[] data = new byte[] {
				(byte) (baudRate & 0xff),
				(byte) ((baudRate >> 8) & 0xff),
				(byte) ((baudRate >> 16) & 0xff),
				(byte) ((baudRate >> 24) & 0xff)
		};
		int ret = connection.controlTransfer(REQTYPE_HOST_TO_DEVICE, SILABSER_SET_BAUDRATE,
				0, 0, data, 4, USB_WRITE_TIMEOUT_MILLIS);
		if (ret < 0) {
			throw new IOException("Error setting baud rate.");
		}
	}

	@Override
	public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
		setBaudRate(baudRate);

		int configDataBits = 0;
		switch (dataBits) {
			case DATABITS_5:
				configDataBits |= 0x0500;
				break;
			case DATABITS_6:
				configDataBits |= 0x0600;
				break;
			case DATABITS_7:
				configDataBits |= 0x0700;
				break;
			case DATABITS_8:
				configDataBits |= 0x0800;
				break;
			default:
				configDataBits |= 0x0800;
				break;
		}

		switch (parity) {
			case PARITY_ODD:
				configDataBits |= 0x0010;
				break;
			case PARITY_EVEN:
				configDataBits |= 0x0020;
				break;
		}

		switch (stopBits) {
			case STOPBITS_1:
				configDataBits |= 0;
				break;
			case STOPBITS_2:
				configDataBits |= 2;
				break;
		}
		setConfigSingle(SILABSER_SET_LINE_CTL_REQUEST_CODE, configDataBits);
	}

}
