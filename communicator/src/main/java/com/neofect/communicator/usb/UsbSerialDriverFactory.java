package com.neofect.communicator.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	private static final int VENDOR_STM = 0x0483;
	private static final int PRODUCT_STM32 = 0x5740;

	public static NeofectUsbSerialDriver createDriver(UsbDevice device, UsbDeviceConnection connection) {
		int vendorId = device.getVendorId();
		int productId = device.getProductId();

		if (vendorId == VENDOR_SILABS) {
			switch (productId) {
				case PRODUCT_SILABS_CP2102:
				case PRODUCT_SILABS_CP2105:
				case PRODUCT_SILABS_CP2108:
				case PRODUCT_SILABS_CP2110:
				    return new NeofectUsbCp21xxSerialDriver(device, connection);
			}
		} else if(vendorId == VENDOR_STM) {
			switch (productId) {
				case PRODUCT_STM32:
					return new NeofectCdcAcmSerialDriver(device, connection);
			}
		}

		throw new IllegalArgumentException("No USB driver for the USB device! vendorId=" + vendorId + ", productId=" + productId);
	}

	public static List<Pair<Integer, Integer>> getSupportingProducts() {
		List<Pair<Integer, Integer>> usbSupportedProducts = new ArrayList<>();
		usbSupportedProducts.add(new Pair<>(VENDOR_SILABS, PRODUCT_SILABS_CP2102));
		usbSupportedProducts.add(new Pair<>(VENDOR_SILABS, PRODUCT_SILABS_CP2105));
		usbSupportedProducts.add(new Pair<>(VENDOR_SILABS, PRODUCT_SILABS_CP2108));
		usbSupportedProducts.add(new Pair<>(VENDOR_SILABS, PRODUCT_SILABS_CP2110));
		return Collections.unmodifiableList(usbSupportedProducts);
	}

}
