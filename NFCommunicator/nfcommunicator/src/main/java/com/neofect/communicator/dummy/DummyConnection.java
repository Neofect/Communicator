package com.neofect.communicator.dummy;

import android.util.Log;

import com.neofect.communicator.Controller;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Device;

/**
 * @author neo.kim@neofect.com
 * @date Nov 02, 2017
 */
public class DummyConnection extends Connection {

	private static final String LOG_TAG = "DummyConnection";

	private static final int DELAY_BEFORE_CONNECTING = 1;
	private static final int DELAY_BEFORE_DISCONNECTED = 1;

	private DummyPhysicalDevice device;

	public DummyConnection(DummyPhysicalDevice device, Controller<? extends Device> controller) {
		super(ConnectionType.DUMMY, controller);
		this.device = device;
	}

	@Override
	public void connect() {
		if(getStatus() != Status.NOT_CONNECTED) {
			Log.e(LOG_TAG, "connect: '" + getDescription() + "' is not in the status of to connect! Status=" + getStatus());
			return;
		}
		handleConnecting();
		device.connect(this);
	}

	void onConnected() {
		handleConnected();
	}

	@Override
	public void disconnect() {
		device.disconnect();
		handleDisconnected();
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
	public void write(byte[] data) {
		device.receive(data);
	}

	void onRead(byte[] data) {
		handleReadData(data);
	}

}
