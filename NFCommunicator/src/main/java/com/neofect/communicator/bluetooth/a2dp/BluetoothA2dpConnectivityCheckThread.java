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

package com.neofect.communicator.bluetooth.a2dp;

import android.util.Log;

/**
 * @author neo.kim@neofect.com
 */
public class BluetoothA2dpConnectivityCheckThread extends Thread {

	private static final String LOG_TAG = BluetoothA2dpConnectivityCheckThread.class.getSimpleName();
	
	private static final int	CONNECTING_TIMEOUT_IN_SECONDS = 10;
	
	private BluetoothA2dpConnection connection;
	private long	connectingStartTimestamp = 0;
	private boolean	canceled = false;
	
	BluetoothA2dpConnectivityCheckThread(BluetoothA2dpConnection connection) {
		this.connection = connection;
	}
	
	void cancel() {
		Log.v(LOG_TAG, this.getClass().getSimpleName() + " thread is canceled.");
		canceled = true;
	}
	
	boolean isCanceled() {
		return canceled;
	}
	
	@Override
	public void run() {
		setName(this.getClass().getSimpleName());
		
		// Store the started time
		if(connectingStartTimestamp == 0)
			connectingStartTimestamp = System.currentTimeMillis();
			
		while(!canceled) {
			try {
				// Check connectivity
				boolean isConnected = connection.checkIsA2dpConnected();
				switch(connection.getStatus()) {
					case NOT_CONNECTED:
						canceled = true;
						break;
//					case NOT_CONNECTED:
//						Thread.sleep(200);
//						break;
//					case FAILED_TO_CONNECT:
//					case DISCONNECTED:
//						isCanceled = true;
//						break;
					case CONNECTED: {
						// Check connectivity
						if(!isConnected)
							connection.onDisconnected();
						else {
							// Sleep between connectivity check
							Thread.sleep(1000);
						}
						break;
					}
					case CONNECTING: {
						// Check connectivity
						if(isConnected)
							connection.onConnected();
						else if(System.currentTimeMillis() - connectingStartTimestamp > CONNECTING_TIMEOUT_IN_SECONDS * 1000) {
							// Check timeout
							connection.onFailedToConnect();
						} else {
							// Sleep between connectivity check
							Thread.sleep(500);
						}
						break;
					}
				}
			} catch(InterruptedException e) {
				Log.v(LOG_TAG, this.getClass().getSimpleName() + " thread is interrupted.");
			} catch(Exception e) {
				Log.e(LOG_TAG, getName() + " - Exception in run() loop!", e);
			}
		}
		Log.d(LOG_TAG, this.getClass().getSimpleName() + " thread is stopped.");
	}

}
