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
package com.neofect.communicator.bluetooth.a2dp;

import java.util.concurrent.TimeoutException;

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
		super("BluetoothA2dpConnectivityCheckThread");
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
						if(isConnected) {
							connection.onConnected();
						} else if(System.currentTimeMillis() - connectingStartTimestamp > CONNECTING_TIMEOUT_IN_SECONDS * 1000) {
							// Check timeout
							connection.onFailedToConnect(new TimeoutException("Timeout during connecting to A2DP."));
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
