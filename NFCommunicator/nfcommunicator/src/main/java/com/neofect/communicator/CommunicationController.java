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

import com.neofect.communicator.exception.InappropriateDeviceException;
import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.message.MessageClassMapper;
import com.neofect.communicator.message.MessageDecoder;
import com.neofect.communicator.message.MessageEncoder;
import com.neofect.communicator.util.ByteArrayConverter;
import com.neofect.communicator.util.ByteRingBuffer;

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
	
	protected void onConnected(T device) {}
	protected void onDisconnected(Connection connection) {}
	/**
	 * This delegate API returns true if the given message must not processed by the device after this method.
	 * 
	 * @param connection
	 * @param message
	 * @return If true returned, the message processing by device will be bypassed. 
	 */
	protected boolean onBeforeDeviceProcessInboundMessage(Connection connection, CommunicationMessage message) { return false; }
	protected void onAfterDeviceProcessInboundMessage(Connection connection, CommunicationMessage message) {}
	
	protected void handleExceptionFromDecodeMessage(Exception exception, Connection connection) {
		Log.e(LOG_TAG, "Failed to decode message!", exception);
	}
	
	protected void handleExceptionFromProcessInboundMessage(Exception exception, Connection connection, CommunicationMessage message) {
		if(exception instanceof InappropriateDeviceException) {
			connection.forceFailedToConnectFromController(exception);
		} else {
			Log.e(LOG_TAG, "Failed to process a message! '" + message.getDescription() + "'", exception);
		}
	}
	
	private static <T extends Device> T createDeviceInstance(Connection connection, Class<T> deviceClass) {
		// Create an instance of the device.
		T device = null;
		try {
			device = deviceClass.getDeclaredConstructor(Connection.class).newInstance(connection);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate an instance of device class!", e);
		}
		return device;
	}
	
	void onConnectedInner(Connection connection) {
		device = createDeviceInstance(connection, deviceClass);
		onConnected(device);
		Communicator.getInstance().notifyConnected(device);
	}

	protected final T getDevice() {
		return device;
	}

	final Class<T> getDeviceClass() {
		return deviceClass;
	}
	
	public final byte[] encodeMessage(CommunicationMessage message) {
		if(encoder == null) {
			Log.e(LOG_TAG, "Message encoder is not set!");
			return null;
		} else if(message == null) {
			Log.e(LOG_TAG, "Given message instance is null!");
			return null;
		}
		
		try {
			return encoder.encodeMessage(message);
		} catch(Exception e) {
			throw new RuntimeException("Failed to encode a message! " + message.getDescription(), e);
		}
	}
	
	/**
	 * This is only called by {@link Connection#handleReadData(byte[])}.
	 *  
	 * @param connection
	 */
	final void decodeRawMessageAndProcess(Connection connection) {
		if(decoder == null) {
			Log.e(LOG_TAG, "Message decoder is not set!");
			return;
		}
		
		while(true) {
			CommunicationMessage message = null;
			try {
				message = decoder.decodeMessage(connection.getRingBuffer());
			} catch(Exception e) {
				printBuffer(connection);
				handleExceptionFromDecodeMessage(e, connection);
			}
			if(message == null) {
				break;
			}
			processInboundMessage(connection, message);
		}
	}

	private static void printBuffer(Connection connection) {
		try {
			ByteRingBuffer buffer = connection.getRingBuffer();
			int length = Math.min(50, buffer.getContentSize());
			byte[] byteArrays = buffer.readWithoutConsume(length);
			ByteArrayConverter.byteArrayToHex(byteArrays);
		} catch (Exception e) {
			Log.e(LOG_TAG, "printBuffer: ", e);
		}
	}

	private void processInboundMessage(Connection connection, CommunicationMessage message) {
		try {
			boolean passMessageProcessingByDevice = onBeforeDeviceProcessInboundMessage(connection, message);
			if(passMessageProcessingByDevice) {
				return;
			}
			
			if(device != null) {
				boolean deviceUpdated = device.processMessage(message);
				Communicator.getInstance().notifyDeviceMessageProcessed(device, message);
				if(deviceUpdated) {
					Communicator.getInstance().notifyDeviceUpdated(device);
				}
			}
			onAfterDeviceProcessInboundMessage(connection, message);
		} catch(Exception e) {
			handleExceptionFromProcessInboundMessage(e, connection, message);
		}
	}
	
	public MessageEncoder getMessageEncoder() {
		return encoder;
	}
	
	public void setMessageEncoder(MessageEncoder encoder) {
		this.encoder = encoder;
	}
	
	public MessageDecoder getMessageDecoder() {
		return decoder;
	}
	
	public void setMessageDecoder(MessageDecoder decoder) {
		this.decoder = decoder;
	}
	
	public void setMessageClassMapper(MessageClassMapper mapper) {
		encoder.setMessageClassMapper(mapper);
		decoder.setMessageClassMapper(mapper);
	}
	
}
