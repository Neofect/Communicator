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
	
	void onFailedToConnect(final Connection connection) {
		post(new Runnable() {
			public void run() {
				try {
					listener.onFailedToConnect(connection);
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
