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

import android.util.Log;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.util.ByteRingBuffer;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public abstract class Connection {

	private static final String LOG_TAG = Connection.class.getSimpleName();
	
	public static enum Status {
		NOT_CONNECTED,
		CONNECTING,
		CONNECTED,
	}
	
	public abstract void	connect();
	public abstract void	disconnect();
	public abstract String	getRemoteAddress();
	public abstract String	getDescription();
	
	private final CommunicationController<? extends Device>	controller;
	
	private ConnectionType	connectionType;
	private Status			status = Status.NOT_CONNECTED;
	private ByteRingBuffer	ringBuffer = new ByteRingBuffer();
	
	public Connection(ConnectionType connectionType, CommunicationController<? extends Device> controller) {
		this.connectionType = connectionType;
		this.controller	= controller;
	}
	
	public ConnectionType getConnectionType() {
		return connectionType;
	}
	
	public boolean isConnected() {
		return status == Status.CONNECTED;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public ByteRingBuffer getRingBuffer() {
		return ringBuffer;
	}
	
	public void	write(byte[] data) {
		Log.e(LOG_TAG, "write() is not implemented for this connection type!");
	}
	
	public void sendMessage(CommunicationMessage message) {
		write(controller.encodeMessage(message));
	}
	
	protected final void handleReadData(byte[] data) {
		ringBuffer.put(data);
		
		// Process message
		controller.decodeRawMessageAndProcess(this);
	}
	
	protected final void handleConnecting() {
		status = Status.CONNECTING;
		controller.onStartConnecting(this);
		Communicator.getInstance().notifyStartConnecting(this, controller.getDeviceClass());
	}
	
	protected final void handleFailedToConnect() {
		status = Status.NOT_CONNECTED;
		controller.onFailedToConnect(this);
		Communicator.getInstance().notifyFailedToConnect(this, controller.getDeviceClass());
	}
	
	protected final void handleConnected() {
		status = Status.CONNECTED;
		Device device = controller.onConnectedInner(this);
		Communicator.getInstance().notifyConnected(device);
	}
	
	protected final void handleDisconnected() {
		status = Status.NOT_CONNECTED;
		controller.onDisconnected(this);
		Communicator.getInstance().notifyDisconnected(this, controller.getDeviceClass());
	}
	
}
