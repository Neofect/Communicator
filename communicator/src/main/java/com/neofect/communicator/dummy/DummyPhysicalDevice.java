package com.neofect.communicator.dummy;

import android.util.Log;

/**
 * @author neo.kim@neofect.com
 * @date Nov 02, 2017
 */
public abstract class DummyPhysicalDevice {

	private static final String LOG_TAG = "DummyPhysicalDevice";

	public interface TransferDelegate {
		void send(byte[] data);
	}

	protected TransferDelegate delegate;
	protected String deviceIdentifier;
	protected String deviceName;

	public DummyPhysicalDevice(String deviceIdentifier, String deviceName) {
		this.deviceIdentifier = deviceIdentifier;
		this.deviceName = deviceName;
	}

	public String getDeviceIdentifier() {
		return deviceIdentifier;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void start(final TransferDelegate delegate) {
		Log.d(LOG_TAG, "start: ");
		this.delegate = delegate;
	}

	public void stop() {
		Log.d(LOG_TAG, "stop: ");
	}

	public void put(byte[] data) {
		Log.w(LOG_TAG, "put: Unsupported operation.");
	}

	public void setTransferDelegate(TransferDelegate delegate) {
		this.delegate = delegate;
	}

}
