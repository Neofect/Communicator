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

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.neofect.communicator.bluetooth.BluetoothConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This thread is to read data from connected Bluetooth socket with synchronous manner. The read() function in run() is being blocked until any data received.
 * This class also has a logic for writing data to Bluetooth socket, but it's not done in a read thread. The thread is only for read.
 * 
 * @author neo.kim@neofect.com
 */
class BluetoothSppTransferThread extends Thread {

	private static final String LOG_TAG = BluetoothSppTransferThread.class.getSimpleName();
	private static final int BUFFER_SIZE	= 1024;
	
	private BluetoothSppConnection connection;
	
	private BluetoothSocket socket;
	private InputStream inputStream; 
	private OutputStream outputStream;

	private byte[] buffer = new byte[BUFFER_SIZE];
	private boolean socketClosed = false;
	
	/**
	 * Constructor for {@link BluetoothSppTransferThread}. This throws IOException when failed to get input / output streams from provided socket.
	 * 
	 * @param connection
	 * @param socket
	 * @throws IOException
	 */
	BluetoothSppTransferThread(BluetoothSppConnection connection, BluetoothSocket socket) throws IOException {
		super("BluetoothSppTransferThread");
		this.connection = connection;
		this.socket = socket;
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
	}
	
	void cancel() {
		try {
			synchronized(this) {
				if(!socketClosed) {
					socket.close();
					socketClosed = true;
				}
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "cancel() Failed to close the socket", e);
		}
	}
	
	void write(byte[] data) {
		synchronized(this) {
			if(!connection.isConnected()) {
				Log.e(LOG_TAG, "write() Connection is closed!");
				return;
			}
			try {
				outputStream.write(data);
				connection.onWroteMessage(data);
			} catch (Exception e) {
				Log.e(LOG_TAG, "write() Failed to write!", e);
				onDisconnected();
			}
		}
	}
	
	private void onDisconnected() {
		synchronized(this) {
			if(!connection.isConnected()) {
				return;
			}
			try {
				inputStream.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "", e);
			}
			try {
				outputStream.close();
			} catch (IOException e) {
				Log.e(LOG_TAG, "", e);
			}
			cancel();
			connection.onDisconnected();
		}
	}
	
	@Override
	public void run() {
		// If it is still trying to connect, wait some time.
		while(connection.getStatus() == BluetoothConnection.Status.CONNECTING) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Log.e(LOG_TAG, "", e);
			}
		}
		
		// Read loop
		while (connection.isConnected()) {
			try {
				// Read data from the input stream
				int numberOfReadBytes = inputStream.read(buffer, 0, buffer.length);
				
				byte[] readData = new byte[numberOfReadBytes];
				System.arraycopy(buffer, 0, readData, 0, numberOfReadBytes);
				connection.onReadMessage(readData);
			} catch (IOException e) {
				Log.d(LOG_TAG, "run() IOException on read(), device=" + connection.getDescriptionWithAddress());
				onDisconnected();
			}
		}
	}
	
}
