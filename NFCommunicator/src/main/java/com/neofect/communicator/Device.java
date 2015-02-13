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
import com.neofect.communicator.util.Log;

/**
 * Subclass must have a constructor which has one parameter of {@link Connection} instance.
 * 
 * @author neo.kim@neofect.com
 * @date 2014. 2. 4.
 */
public abstract class Device {
	
	private String		deviceName;
	private Connection	connection;
	private boolean		ready;
	
	/**
	 * A subclass must return true if this device is updated by message processing.
	 * If it returns true, an event for device update is dispatched.
	 * 
	 * @param message
	 * @return
	 */
	protected abstract boolean processMessage(CommunicationMessage message);
	
	public Device(Connection connection) {
		this.connection = connection;
	}
	
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	void setReady(boolean ready) {
		this.ready = ready;
	}
	
	public boolean isReady() {
		return ready;
	}
	
	protected void sendMessage(CommunicationMessage message) {
		if(connection == null) {
			Log.e("Device", "The connection is null!");
			return;
		}
		connection.sendMessage(message);
	}
	
}
