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
package com.neofect.communicator;

import com.neofect.communicator.message.CommunicationMessage;

import android.os.Handler;
import android.util.Log;

/**
 * @author neo.kim@neofect.com
 * @date 2014. 2. 4.
 */
class CommunicationHandler<T extends Device> extends Handler {
	
	CommunicationListener<T> listener;
	
	CommunicationHandler(CommunicationListener<T> listener) {
		if(listener == null)
			throw new IllegalArgumentException("Listener must not be null!");
		this.listener = listener;
	}

	void onStartConnecting(final Connection connection) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onStartConnecting(connection);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
	void onFailedToConnect(final Connection connection, final Exception cause) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onFailedToConnect(connection, cause);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
	void onDeviceConnected(final T device, final boolean alreadyExisting) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onDeviceConnected(device, alreadyExisting);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
	void onDeviceDisconnected(final T device) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onDeviceDisconnected(device);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
	void onDeviceReady(final T device, final boolean alreadyExisting) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onDeviceReady(device, alreadyExisting);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
	void onDeviceMessageProcessed(final T device, final CommunicationMessage message) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onDeviceMessageProcessed(device, message);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
	void onDeviceUpdated(final T device) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onDeviceUpdated(device);
				} catch(Exception e) {
					Log.e(CommunicationHandler.class.getSimpleName(), "", e);
				}
			}
		});
	}
	
}
