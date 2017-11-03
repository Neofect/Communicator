package com.neofect.communicator.dummy;

import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Neo on 2017. 11. 3..
 */

public class DummyPhysicalDeviceManager {

	private static final String LOG_TAG = DummyPhysicalDeviceManager.class.getSimpleName();

	private static final Map<String, DummyPhysicalDevice> devices = new LinkedHashMap<>();

	public static void register(DummyPhysicalDevice device) {
		if (devices.containsKey(device.getDeviceIdentifier())) {
			Log.w(LOG_TAG, "register: Same device identifier exists. It will be replaced. '" +
					device.getDeviceIdentifier() + "'");
		}
		devices.put(device.getDeviceIdentifier(), device);
	}

	public static void unregister(DummyPhysicalDevice device) {
		devices.remove(device.getDeviceIdentifier());
	}

	public static DummyPhysicalDevice[] getDevices() {
		Collection<DummyPhysicalDevice> collection = devices.values();
		return collection.toArray(new DummyPhysicalDevice[collection.size()]);
	}

	public static DummyPhysicalDevice getDevice(String deviceIdentifier) {
		return devices.get(deviceIdentifier);
	}

}
