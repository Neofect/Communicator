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
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.neofect.communicator.ConnectionType;

/**
 * @author neo.kim@neofect.com
 */
class BluetoothSppConnectThread extends Thread {
	
	private static final String LOG_TAG = BluetoothSppConnectThread.class.getSimpleName();
	
	private final BluetoothSppConnection bluetoothConnection;
	
	BluetoothSppConnectThread(BluetoothSppConnection bluetoothConnection) {
		this.bluetoothConnection = bluetoothConnection;
	}
	
	@Override
	public void run() {
		setName(this.getClass().getSimpleName());

		// Always cancel discovery because it will slow down a connection.
		// As recommended in http://developer.android.com/guide/topics/connectivity/bluetooth.html#ConnectingAsAClient
		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
		
		BluetoothDevice device = bluetoothConnection.getDevice();
		
		// Get a BluetoothSocket for a connection with the given BluetoothDevice.
		BluetoothSocket socket = null;
		try {
			// UUID for SPP connection
			UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

			if(bluetoothConnection.getConnectionType() == ConnectionType.BLUETOOTH_SPP) 
				socket = device.createRfcommSocketToServiceRecord(sppUuid);
			else if(bluetoothConnection.getConnectionType() == ConnectionType.BLUETOOTH_SPP_INSECURE) 
				socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid);
			else
				Log.e(LOG_TAG, "Unknown bluetooth connection type! '" + bluetoothConnection.getConnectionType() + "'");
		} catch (IOException e) {
			Log.e(LOG_TAG, "Failed to create bluetooth socket!", e);
			bluetoothConnection.onFailedToConnect();
			return;
		}

		// Make a connection to the BluetoothSocket
		try {
			// This is a blocking call and will only return on a successful connection or an exception.
			socket.connect();
		} catch (IOException e) {
			Log.e(LOG_TAG, "Failed to connect to device '" + bluetoothConnection.getDescriptionWithAddress() + "'", e);
			bluetoothConnection.onFailedToConnect();
			return;
		}

		bluetoothConnection.onSucceededToConnect(socket);
	}

}
