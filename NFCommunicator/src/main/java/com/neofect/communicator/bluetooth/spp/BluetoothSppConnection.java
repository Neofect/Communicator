/****************************************************************************
Copyright (c) 2014-2015 Neofect Co., Ltd.

http://www.neofect.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/

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
