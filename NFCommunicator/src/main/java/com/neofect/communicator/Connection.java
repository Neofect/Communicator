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
	
	public Device getDevice() {
		return controller.getDevice();
	}
	
	public Class<? extends Device> getDeviceClass() {
		return controller.getDeviceClass();
	}
	
	public Status getStatus() {
		return status;
	}
	
	public ByteRingBuffer getRingBuffer() {
		return ringBuffer;
	}
	
	public CommunicationController<? extends Device> getController() {
		return controller;
	}
	
	public void	write(byte[] data) {
		Log.e(LOG_TAG, "write() is not implemented for this connection type!");
	}
	
	public void sendMessage(CommunicationMessage message) {
		try {
			write(controller.encodeMessage(message));
		} catch(Exception e) {
			Log.e(LOG_TAG, "Failed send a message!", e);
		}
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
	
	void forceFailedToConnectFromController(Exception cause) {
		disconnect();
		handleFailedToConnect(cause);
	}
	
	protected final void handleFailedToConnect(Exception cause) {
		status = Status.NOT_CONNECTED;
		controller.onFailedToConnect(this, cause);
		Communicator.getInstance().notifyFailedToConnect(this, controller.getDeviceClass(), cause);
	}
	
	protected final void handleConnected() {
		try {
			status = Status.CONNECTED;
			Device device = controller.onConnectedInner(this);
			Communicator.getInstance().notifyConnected(device);
		} catch(Exception e) {
			try {
				this.disconnect();
			} catch(Exception e1) {
				Log.e(LOG_TAG, "Failed to disconnect!", e1);
			}
			handleFailedToConnect(new Exception("Failed to process the connected device!", e));
		}
	}
	
	protected final void handleDisconnected() {
		status = Status.NOT_CONNECTED;
		controller.onDisconnected(this);
		Communicator.getInstance().notifyDisconnected(this, controller.getDeviceClass());
	}
	
}
