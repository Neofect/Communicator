package com.neofect.communicator.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;

import java.io.IOException;

/**
 * @author neo.kim@neofect.com
 * @date Nov 29, 2016
 */
public abstract class NeofectUsbSerialDriver {

	/**
	 * 5 data bits.
	 */
	public static final int DATABITS_5 = 5;

	/**
	 * 6 data bits.
	 */
	public static final int DATABITS_6 = 6;

	/**
	 * 7 data bits.
	 */
	public static final int DATABITS_7 = 7;

	/**
	 * 8 data bits.
	 */
	public static final int DATABITS_8 = 8;

	/**
	 * No flow control.
	 */
	public static final int FLOWCONTROL_NONE = 0;

	/**
	 * RTS/CTS input flow control.
	 */
	public static final int FLOWCONTROL_RTSCTS_IN = 1;

	/**
	 * RTS/CTS output flow control.
	 */
	public static final int FLOWCONTROL_RTSCTS_OUT = 2;

	/**
	 * XON/XOFF input flow control.
	 */
	public static final int FLOWCONTROL_XONXOFF_IN = 4;

	/**
	 * XON/XOFF output flow control.
	 */
	public static final int FLOWCONTROL_XONXOFF_OUT = 8;

	/**
	 * No parity.
	 */
	public static final int PARITY_NONE = 0;

	/**
	 * Odd parity.
	 */
	public static final int PARITY_ODD = 1;

	/**
	 * Even parity.
	 */
	public static final int PARITY_EVEN = 2;

	/**
	 * Mark parity.
	 */
	public static final int PARITY_MARK = 3;

	/**
	 * Space parity.
	 */
	public static final int PARITY_SPACE = 4;

	/**
	 * 1 stop bit.
	 */
	public static final int STOPBITS_1 = 1;

	/**
	 * 1.5 stop bits.
	 */
	public static final int STOPBITS_1_5 = 3;

	/**
	 * 2 stop bits.
	 */
	public static final int STOPBITS_2 = 2;

	protected UsbDevice device;
	protected UsbDeviceConnection connection;

	NeofectUsbSerialDriver(UsbDevice device, UsbDeviceConnection connection) {
		this.device = device;
		this.connection = connection;
	}

	public abstract UsbEndpoint[] open() throws IOException;
	public abstract void close() throws IOException;
	public abstract void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException;

}
