package com.neofect.communicator.dummy;

import android.util.Log;

/**
 * @author neo.kim@neofect.com
 * @date Nov 02, 2017
 */
public abstract class DummyPhysicalDevice {

	private static final String LOG_TAG = "DummyPhysicalDevice";

	private static final int DELAY_BEFORE_CONNECTED = 1;

	protected DummyConnection connection;

	public String getDeviceName() {
		return "DummyPhysicalDevice";
	}

	public String getDeviceIdentifier() {
		return "DummyPhysicalDeviceIdentifier";
	}

	void connect(final DummyConnection connection) {
		this.connection = connection;

		new Thread(new Runnable() {
			@Override public void run() {
				try {
					Thread.sleep(DELAY_BEFORE_CONNECTED * 1000);
					connection.onConnected();
				} catch (Exception e) {
					Log.e(LOG_TAG, "", e);
				}
			}
		}).start();
	}

	void disconnect() {
		Log.d(LOG_TAG, "disconnect: ");
	}

	protected void write(byte[] data) {
		Log.d(LOG_TAG, "write: ");
	}

	protected void read(byte[] data) {
		connection.onRead(data);
	}
}
