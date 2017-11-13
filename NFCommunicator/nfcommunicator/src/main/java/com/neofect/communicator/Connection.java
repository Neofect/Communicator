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

import com.neofect.communicator.message.Message;
import com.neofect.communicator.util.ByteRingBuffer;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public abstract class Connection {

	private static final String LOG_TAG = "Connection";
	
	public enum Status {
		NOT_CONNECTED,
		CONNECTING,
		CONNECTED,
	}
	
	public abstract void connect();
	public abstract void disconnect();
	public abstract String getDeviceIdentifier();
	public abstract String getDeviceName();
	public abstract String getDescription();
	
	private Controller<? extends Device> controller;
	
	private ConnectionType connectionType;
	private Status status = Status.NOT_CONNECTED;
	private ByteRingBuffer ringBuffer = new ByteRingBuffer();
	
	public Connection(ConnectionType connectionType, Controller<? extends Device> controller) {
		this.connectionType = connectionType;
		this.controller = controller;
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
	
	public Controller<? extends Device> getController() {
		return controller;
	}
	
	public void write(byte[] data) {
		Log.e(LOG_TAG, "write: is not implemented for this connection type!");
	}

	public void sendMessage(Message message) {
		write(controller.encodeMessage(message));
	}

	protected final void handleReadData(byte[] data) {
		ringBuffer.put(data);

		// Process message
		synchronized (this) {
			controller.decodeRawMessageAndProcess(this);
		}
	}

	protected final void handleConnecting() {
		status = Status.CONNECTING;
		Communicator.getInstance().notifyStartConnecting(this, controller.getDeviceClass());
	}

	protected final void handleFailedToConnect(Exception cause) {
		status = Status.NOT_CONNECTED;
		Communicator.getInstance().notifyFailedToConnect(this, controller.getDeviceClass(), cause);
	}

	protected final void handleConnected() {
		try {
			status = Status.CONNECTED;
			controller.startControl(this);
			Communicator.getInstance().notifyConnected(controller.getDevice());
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

	public void replaceController(Controller<? extends Device> newController) {
		synchronized (this) {
			Controller<? extends Device> oldController = this.controller;
			this.controller = newController;
			oldController.halt();
			if (status == Status.CONNECTED) {
				newController.startAfterReplaced(this);
				Communicator.getInstance().onControllerReplaced(this, oldController, newController);
			}
		}
	}

}
