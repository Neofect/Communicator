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

import java.io.IOException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.neofect.communicator.CommunicationController;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Device;
import com.neofect.communicator.bluetooth.BluetoothConnection;

/**
 * This class contains all information regarding to bluetooth connection and
 * related logics to write to and read data from bluetooth connection.
 * 
 * @author neo.kim@neofect.com
 */
public class BluetoothSppConnection extends BluetoothConnection {

	private static final String LOG_TAG = BluetoothSppConnection.class.getSimpleName();
	
	private BluetoothSppTransferThread transferThread;
	
	public BluetoothSppConnection(BluetoothDevice device, CommunicationController<? extends Device> controller, ConnectionType connectionType) {
		super(device, controller, connectionType);
		if(connectionType != ConnectionType.BLUETOOTH_SPP && connectionType != ConnectionType.BLUETOOTH_SPP_INSECURE)
			throw new IllegalArgumentException("Only SPP connection type is allowed!");
	}
	
	private void cancelTransferThread() {
		transferThread.cancel();
		transferThread = null;
	}

	/**
	 * Create a thread which does connect job. Once its job is done, {@link #onSucceededToConnect(BluetoothSocket) or #onFailedToConnect() will be called. 
	 */
	@Override
	protected void connectProcess() {
		handleConnecting();
		
		// Create a thread to try to connect
		BluetoothSppConnectThread connectThread = new BluetoothSppConnectThread(this);
		connectThread.start();
	}
	
	@Override
	protected void disconnectProcess() {
		if(transferThread != null)
			cancelTransferThread();
	}
	
	@Override
	public void write(byte[] data) {
		synchronized(this) {
			if(transferThread == null) {
				Log.e(LOG_TAG, "Couldn't write data! The connection might not be established or already disconnected.");
				return;
			}
			transferThread.write(data);
		}
	}
	
	/**
	 * This is called by {@link BluetoothSppConnectThread} with connected socket instance
	 * once the {@link BluetoothSppConnectThread} succeeded to connect to the device.
	 * 
	 * @param socket
	 */
	void onSucceededToConnect(BluetoothSocket socket) {
		Log.v(LOG_TAG, "onSucceededToConnect() connection=" + getDescription());
		
		// If a transfer thread is alive, cancel the thread.
		if(transferThread != null)
			cancelTransferThread();
		
		// This connection is requested to be disconnected.
		if(isDisconnectRequested()) {
			try {
				socket.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "", e);
			}
			handleDisconnected();
			return;
		}
		
		// Create a thread which reads data from the socket
		try {
			transferThread = new BluetoothSppTransferThread(this, socket);
		} catch (IOException e) {
			Log.e(LOG_TAG, "", e);
			handleFailedToConnect();
			return;
		}
		transferThread.start();
		
		// It is connected!
		handleConnected();
	}
	
	/**
	 * Followings are called by {@link BluetoothSppConnectThread}.
	 */
	void onFailedToConnect() {
		handleFailedToConnect();
	}

	void onReadMessage(byte[] data) {
		handleReadData(data);
	}
	
	void onWroteMessage(byte[] data) {
		// Does nothing
	}
	
	void onDisconnected() {
		handleDisconnected();
	}
	
}
