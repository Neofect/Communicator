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
import com.neofect.communicator.message.MessageDecoder;
import com.neofect.communicator.message.MessageEncoder;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public class CommunicationController<T extends Device> {
	
	private static final String LOG_TAG = CommunicationController.class.getSimpleName();
	
	private Class<T> deviceClass;
	private T device;
	
	private MessageEncoder encoder;
	private MessageDecoder decoder;
	
	public CommunicationController(Class<T> deviceClass) {
		this.deviceClass = deviceClass;
	}
	
	public CommunicationController(Class<T> deviceClass, MessageEncoder encoder, MessageDecoder decoder) {
		this.deviceClass = deviceClass;
		this.encoder = encoder;
		this.decoder = decoder;
	}
	
	protected void onStartConnecting(Connection connection) {}
	protected void onFailedToConnect(Connection connection) {}
	protected void onConnected(T device) {}
	protected void onDisconnected(Connection connection) {}
	protected void onBeforeDeviceProcessInboundMessage(Connection connection, CommunicationMessage message) {}
	protected void onAfterDeviceProcessInboundMessage(Connection connection, CommunicationMessage message) {}
	
	/**
	 * This method should be called by subclass when it is determined whether a device is connected AND ready.
	 * @param device
	 */
	protected final void notifyDeviceReady(T device) {
		if(device.isReady()) {
			Log.e(LOG_TAG, "Device is already ready!");
			return;
		}
		device.setReady(true);
		Communicator.getInstance().notifyDeviceReady(device);
	}
	
	final T onConnectedInner(Connection connection) {
		device = Communicator.createDeviceInstance(connection, deviceClass);
		onConnected(device);
		return device;
	}
	
	protected final T getDevice() {
		return device;
	}
	
	final Class<T> getDeviceClass() {
		return deviceClass;
	}
	
	final byte[] encodeMessage(CommunicationMessage message) {
		if(encoder == null) {
			Log.e(LOG_TAG, "Message parser is not set!");
			return null;
		} else if(message == null) {
			Log.e(LOG_TAG, "Given message instance is null!");
			return null;
		}
		
		try {
			return encoder.encodeMessage(message);
		} catch(Exception e) {
			throw new RuntimeException("Failed to encode a message! '" + message.getDescription() + "'", e);
		}
	}
	
	/**
	 * This is only called by {@link Connection#handleReadData(byte[])}.
	 *  
	 * @param connection
	 */
	final void decodeRawMessageAndProcess(Connection connection) {
		if(decoder == null) {
			Log.e(LOG_TAG, "Message parser is not set!");
			return;
		}
		
		while(true) {
			CommunicationMessage message = null;
			try {
				message = decoder.decodeMessage(connection.getRingBuffer());
			} catch(Exception e) {
				Log.e(LOG_TAG, "Failed to decode message!", e);
			}
			if(message == null)
				break;
			processInboundMessage(connection, message);
		}
	}
	
	private void processInboundMessage(Connection connection, CommunicationMessage message) {
		try {
			onBeforeDeviceProcessInboundMessage(connection, message);
			if(device != null) {
				boolean deviceUpdated = device.processMessage(message);
				Communicator.getInstance().notifyDeviceMessageProcessed(device, message);
				if(deviceUpdated)
					Communicator.getInstance().notifyDeviceUpdated(device);
			}
			onAfterDeviceProcessInboundMessage(connection, message);
		} catch(Exception e) {
			Log.e(LOG_TAG, "Failed to process a message! '" + message.getDescription() + "'", e);
		}
	}
	
}
