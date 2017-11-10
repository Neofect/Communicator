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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public abstract class CommunicationController<T extends Device> {
	
	private static final String LOG_TAG = "CommunicationController";
	
	private Class<T> deviceClass;
	private T device;
	
	private MessageEncoder encoder;
	private MessageDecoder decoder;

	private boolean halted = false;

	public CommunicationController() {
		this.deviceClass = getClassFromGeneric(this);
	}

	public CommunicationController(MessageEncoder encoder, MessageDecoder decoder) {
		this.deviceClass = getClassFromGeneric(this);
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
		try {
			return deviceClass.getDeclaredConstructor(Connection.class).newInstance(connection);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate an instance of device class!", e);
		}
	}

	protected void startControl(Connection connection) {
		initializeDevice(connection);
		onConnected(device);
		decodeRawMessageAndProcess(connection);
	}

	protected void startAfterReplaced(Connection connection) {
		initializeDevice(connection);
		decodeRawMessageAndProcess(connection);
	}

	protected void initializeDevice(Connection connection) {
		if (device != null) {
			throw new IllegalStateException("The device is already initialized!");
		}
		device = createDeviceInstance(connection, deviceClass);
	}

	void halt() {
		String message = "";
		if (device != null) {
			message = " connection=" + device.getConnection().getDescription();
		}
		Log.i(LOG_TAG, "halt: Controller halted." + message);
		halted = true;
	}

	protected boolean isHalted() {
		return halted;
	}

	protected final T getDevice() {
		return device;
	}

	protected final Class<T> getDeviceClass() {
		return deviceClass;
	}
	
	final byte[] encodeMessage(CommunicationMessage message) {
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
		
		while(!halted) {
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
			boolean skipMessageProcessingByDevice = onBeforeDeviceProcessInboundMessage(connection, message);
			if(skipMessageProcessingByDevice) {
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

	/**
	 * A neat way to get class type of generic.
	 * http://stackoverflow.com/a/3403976/576440
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Device> Class<T> getClassFromGeneric(CommunicationController<T> controller) {
		try {
			Type superClass = controller.getClass().getGenericSuperclass();
			return (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
		} catch(Exception e) {
			throw new IllegalArgumentException("Failed to get parameterized class type from the given generic!", e);
		}
	}

}
