/*
 * Copyright 2014-2015 Neofect Co., Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neofect.communicator.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Controller;
import com.neofect.communicator.Device;

/**
 * @author neo.kim@neofect.com
 */
public abstract class BluetoothConnection extends Connection {

	private static final String LOG_TAG = BluetoothConnection.class.getSimpleName();
	
	private BluetoothDevice bluetoothDevice;
	private boolean disconnectRequested = false;
	
	public BluetoothConnection(BluetoothDevice device, Controller<? extends Device> controller, ConnectionType connectionType) {
		super(connectionType, controller);
		this.bluetoothDevice = device;
	}
	
	public final BluetoothDevice getBluetoothDevice() {
		return bluetoothDevice;
	}
	
	public final boolean isDisconnectRequested() {
		return disconnectRequested;
	}
	
	@Override
	public String getDescription() {
		return getDescriptionWithAddress();
	}
	
	@Override
	public String getDeviceIdentifier() {
		return bluetoothDevice.getAddress();
	}

	@Override
	public String getDeviceName() {
		String deviceName = null;
		try {
			deviceName = bluetoothDevice.getName();
		} catch(Exception e) {
			Log.e(LOG_TAG, "", e);
		}
		if(deviceName == null) {
			deviceName = "Unknown";
		}
		return deviceName;
	}
	
	public final String getDescriptionWithAddress() {
		return getDeviceName() + "(" + getDeviceIdentifier() + ")-" + getConnectionType();
	}
	
	@Override
	public final void connect() {
		Log.d(LOG_TAG, "connect: device= '" + getDescriptionWithAddress() + "'");
		if(getStatus() == Status.NOT_CONNECTED) {
			disconnectRequested = false;
			connectProcess();
		} else {
			Log.e(LOG_TAG, "connect: '" + getDescriptionWithAddress() + "' is not in the status of to connect! Status=" + getStatus());
			return;
		}
		
	}
	
	@Override
	public final void disconnect() {
		Log.d(LOG_TAG, "disconnect: device='" + getDescriptionWithAddress() + "'");
		disconnectRequested = true;
		
		disconnectProcess();
	}
	
	protected abstract void connectProcess();
	protected abstract void disconnectProcess();

}
