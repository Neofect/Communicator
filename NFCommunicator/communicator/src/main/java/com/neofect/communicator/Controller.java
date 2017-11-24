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
import com.neofect.communicator.message.MessageDecoder;
import com.neofect.communicator.message.MessageEncoder;
import com.neofect.communicator.util.ByteArrayConverter;
import com.neofect.communicator.util.ByteRingBuffer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public abstract class Controller<T extends Device> {
	
	private static final String LOG_TAG = "Controller";

	public interface InboundMessageCallback {
		boolean process(Connection connection, Message message);
	}
	
	private Class<T> deviceClass;
	private T device;
	
	private MessageEncoder encoder;
	private MessageDecoder decoder;

	private List<InboundMessageCallback> beforeCallbacks = new ArrayList<>();
	private List<InboundMessageCallback> afterCallbacks = new ArrayList<>();

	private boolean halted = false;

	public Controller() {
		this.deviceClass = getClassFromGeneric(this);
	}

	public Controller(MessageEncoder encoder, MessageDecoder decoder) {
		this.deviceClass = getClassFromGeneric(this);
		this.encoder = encoder;
		this.decoder = decoder;
	}

	protected void onConnected(Connection connection) {}
	protected void onDisconnected(Connection connection) {}
	protected void onReplaced(Connection connection) {}

	public void addCallbackBeforeProcessInboundMessage(InboundMessageCallback callback) {
		beforeCallbacks.add(callback);
	}

	public void addCallbackBeforeProcessInboundMessageAtFront(InboundMessageCallback callback) {
		beforeCallbacks.add(0, callback);
	}

	public boolean removeCallbackBeforeProcessInboundMessage(InboundMessageCallback callback) {
		return beforeCallbacks.remove(callback);
	}

	public void addCallbackAfterProcessInboundMessage(InboundMessageCallback callback) {
		afterCallbacks.add(callback);
	}

	public void addCallbackAfterProcessInboundMessageAtFront(InboundMessageCallback callback) {
		afterCallbacks.add(0, callback);
	}

	public boolean removeCallbackAfterProcessInboundMessage(InboundMessageCallback callback) {
		return afterCallbacks.remove(callback);
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

	protected void handleExceptionWhenDecodingMessage(Exception exception, Connection connection) {
		Log.e(LOG_TAG, "Failed to decode message!", exception);
	}
	
	protected void handleExceptionWhenProcessingInboundMessage(Exception exception, Connection connection, Message message) {
		Log.e(LOG_TAG, "Failed to process message! '" + message.getDescription() + "'", exception);
	}
	
	private static <T extends Device> T createDeviceInstance(Connection connection, Class<T> deviceClass) {
		// Create an instance of the device.
		try {
			return deviceClass.getDeclaredConstructor(Connection.class).newInstance(connection);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate an instance of device class!", e);
		}
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

	public final T getDevice() {
		return device;
	}

	protected final Class<T> getDeviceClass() {
		return deviceClass;
	}
	
	final byte[] encodeMessage(Message message) {
		if (encoder == null) {
			Log.e(LOG_TAG, "Message encoder is not set!");
			return null;
		} else if (message == null) {
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
		if (decoder == null) {
			Log.e(LOG_TAG, "Message decoder is not set!");
			return;
		}
		
		while(!halted) {
			Message message = null;
			try {
				message = decoder.decodeMessage(connection.getRingBuffer());
			} catch(Exception e) {
				printBuffer(connection);
				handleExceptionWhenDecodingMessage(e, connection);
			}
			if (message == null) {
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

	private void processInboundMessage(Connection connection, Message message) {
		try {
			for (InboundMessageCallback callback : beforeCallbacks) {
				if (callback.process(connection, message)) {
					return;
				}
			}

			if (device != null) {
				boolean deviceUpdated = device.processMessage(message);
				Communicator.getInstance().notifyDeviceMessageProcessed(device, message);
				if (deviceUpdated) {
					Communicator.getInstance().notifyDeviceUpdated(device);
				}
			}

			for (InboundMessageCallback callback : afterCallbacks) {
				if (callback.process(connection, message)) {
					return;
				}
			}
		} catch(Exception e) {
			handleExceptionWhenProcessingInboundMessage(e, connection, message);
		}
	}

	/**
	 * A neat way to get class type of generic.
	 * http://stackoverflow.com/a/3403976/576440
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Device> Class<T> getClassFromGeneric(Controller<T> controller) {
		try {
			Type superClass = controller.getClass().getGenericSuperclass();
			return (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
		} catch(Exception e) {
			throw new IllegalArgumentException("Failed to get parameterized class type from the given generic!", e);
		}
	}

}
