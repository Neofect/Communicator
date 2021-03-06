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
package com.neofect.communicator.bluetooth.spp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.neofect.communicator.ConnectionType;

import java.io.IOException;
import java.util.UUID;

/**
 * @author neo.kim@neofect.com
 */
class BluetoothSppConnectThread extends Thread {
	
	private static final String LOG_TAG = BluetoothSppConnectThread.class.getSimpleName();
	
	private final BluetoothSppConnection connection;
	
	BluetoothSppConnectThread(BluetoothSppConnection connection) {
		super("BluetoothSppConnectThread");
		this.connection = connection;
	}
	
	@Override
	public void run() {
		// Always cancel discovery because it will slow down a connection.
		// As recommended in http://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingAsAClient
		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
		
		BluetoothDevice device = connection.getBluetoothDevice();
		
		// Get a BluetoothSocket for a connection with the given BluetoothDevice.
		BluetoothSocket socket;
		try {
			// UUID for SPP connection
			UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

			if (connection.getConnectionType() == ConnectionType.BLUETOOTH_SPP) {
				socket = device.createRfcommSocketToServiceRecord(sppUuid);
			} else if (connection.getConnectionType() == ConnectionType.BLUETOOTH_SPP_INSECURE) {
				socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid);
			} else {
				connection.onFailedToConnect(new RuntimeException("Unknown bluetooth connection type! '" + connection.getConnectionType() + "'"));
				return;
			}
		} catch (IOException e) {
			connection.onFailedToConnect(new RuntimeException("Failed to create bluetooth socket!", e));
			return;
		}

		// Make a connection to the BluetoothSocket
		try {
			// This is a blocking call and will only return on a successful connection or an exception.
			socket.connect();
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException e1) {
				Log.e(LOG_TAG, "", e1);
			}
			connection.onFailedToConnect(new RuntimeException("Failed to connect to device '" + connection.getDescriptionWithAddress() + "'", e));
			return;
		}

		connection.onSucceededToConnect(socket);
	}

}
