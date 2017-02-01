package com.neofect.communicator.usb;

/**
 * @author neo.kim@neofect.com
 * @date Feb 01, 2017
 */
public class UsbDeviceHasNoInterfaceException extends RuntimeException {

	public UsbDeviceHasNoInterfaceException() {}

	public UsbDeviceHasNoInterfaceException(String message) {
		super(message);
	}

	public UsbDeviceHasNoInterfaceException(String message, Throwable cause) {
		super(message, cause);
	}

	public UsbDeviceHasNoInterfaceException(Throwable cause) {
		super(cause);
	}

}
