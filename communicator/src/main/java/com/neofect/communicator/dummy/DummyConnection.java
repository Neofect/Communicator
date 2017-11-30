package com.neofect.communicator.dummy;

import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Controller;
import com.neofect.communicator.Device;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author neo.kim@neofect.com
 * @date Nov 02, 2017
 */
public class DummyConnection extends Connection {

	private static final String LOG_TAG = "DummyConnection";

	private Executor executor = Executors.newSingleThreadExecutor();
	private DummyPhysicalDevice device;

	public DummyConnection(DummyPhysicalDevice device, Controller<? extends Device> controller) {
		super(ConnectionType.DUMMY, controller);
		this.device = device;
	}

	@Override
	public void connect() {
		if (getStatus() != Status.NOT_CONNECTED) {
			Log.e(LOG_TAG, "connect: '" + getDescription() + "' is not in the status of to connect! Status=" + getStatus());
			return;
		}
		executor.execute(() -> {
			handleConnecting();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Log.e(LOG_TAG, "", e);
			}
			device.connect(DummyConnection.this);
		});
	}

	void onConnected() {
		executor.execute(this::handleConnected);
	}

	@Override
	public void disconnect() {
		executor.execute(() -> {
			try {
				device.disconnect();
				handleDisconnected();
			} catch (Exception e) {
				Log.e(LOG_TAG, "", e);
			}
		});
	}

	@Override
	public String getDeviceName() {
		return device.getDeviceName();
	}

	@Override
	public String getDeviceIdentifier() {
		return device.getDeviceIdentifier();
	}

	@Override
	public String getDescription() {
		return getDeviceName() + "(" + getDeviceIdentifier() + ")-" + getConnectionType();
	}

	@Override
	public void write(final byte[] data) {
		if (!isConnected()) {
			Log.e(LOG_TAG, "write: Not connected! connection=" + getDescription());
			return;
		}
		executor.execute(() -> device.receive(data));
	}

	void onRead(final byte[] data) {
		if (!isConnected()) {
			Log.e(LOG_TAG, "onRead: Not connected! connection=" + getDescription());
			return;
		}
		executor.execute(() -> handleReadData(data));
	}

}
