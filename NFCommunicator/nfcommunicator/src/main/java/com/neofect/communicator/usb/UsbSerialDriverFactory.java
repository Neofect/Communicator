package com.neofect.communicator.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

/**
 * @author neo.kim@neofect.com
 * @date Nov 29, 2016
 */
public class UsbSerialDriverFactory {

	private static final int VENDOR_SILABS = 0x10c4;
	private static final int PRODUCT_SILABS_CP2102 = 0xea60;
	private static final int PRODUCT_SILABS_CP2105 = 0xea70;
	private static final int PRODUCT_SILABS_CP2108 = 0xea71;
	private static final int PRODUCT_SILABS_CP2110 = 0xea80;

	public static UsbSerialDriver createDriver(UsbDevice device, UsbDeviceConnection connection) {
		int vendorId = device.getVendorId();
		int productId = device.getProductId();

		if (vendorId == VENDOR_SILABS) {
			switch (productId) {
				case PRODUCT_SILABS_CP2102:
				case PRODUCT_SILABS_CP2105:
				case PRODUCT_SILABS_CP2108:
				case PRODUCT_SILABS_CP2110:
				return new UsbCp21xxSerialDriver(device, connection);
			}
		}

		throw new IllegalArgumentException("No USB driver for the USB device! vendorId=" + vendorId + ", productId=" + productId);
	}
}
