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
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.neofect.communicator.bluetooth.BluetoothConnection;

/**
 * This thread is to read data from connected Bluetooth socket with synchronous manner. The read() function in run() is being blocked until any data received.
 * This class also has a logic for writing data to Bluetooth socket, but it's not done in a read thread. The thread is only for read.
 * 
 * @author neo.kim@neofect.com
 */
class BluetoothSppTransferThread extends Thread {

	private static final String LOG_TAG = BluetoothSppTransferThread.class.getSimpleName();
	private static final int	BUFFER_SIZE	= 1024;
	
	private BluetoothSppConnection connection;
	
	private BluetoothSocket		socket;
	private InputStream			inputStream; 
	private OutputStream		outputStream;

	private byte[]				buffer			= new byte[BUFFER_SIZE];
	private boolean				isSocketClosed	= false;
	
	/**
	 * Constructor for {@link BluetoothSppTransferThread}. This throws IOException when failed to get input / output streams from provided socket.
	 * 
	 * @param connection
	 * @param socket
	 * @throws IOException
	 */
	BluetoothSppTransferThread(BluetoothSppConnection connection, BluetoothSocket socket) throws IOException {
		this.connection = connection;
		this.socket = socket;
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
	}
	
	void	cancel() {
		try {
			synchronized(this) {
				if(!isSocketClosed) {
					socket.close();
					isSocketClosed = true;
				}
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "cancel() Failed to close the socket", e);
		}
	}
	
	void	write(byte[] data) {
		try {
			outputStream.write(data);
			connection.onWroteMessage(data);
		} catch (Exception e) {
			Log.e(LOG_TAG, "write() Failed to write!", e);
			onDisconnected();
		}
	}
	
	private void	onDisconnected() {
		synchronized(this) {
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
		}
		connection.onDisconnected();
	}
	
	@Override
	public void run() {
		setName(this.getClass().getSimpleName());
		
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
