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
package com.neofect.communicator.message;

import android.util.Log;

import com.neofect.communicator.exception.UndefinedMessageIdException;
import com.neofect.communicator.util.ByteArrayConverter;
import com.neofect.communicator.util.ByteRingBuffer;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public abstract class MessageDecoder {

	private static final String LOG_TAG = "MessageDecoder";
	
	private MessageClassMapper messageClassMapper;
	
	public MessageDecoder(MessageClassMapper messageClassMapper) {
		this.messageClassMapper = messageClassMapper;
	}
	
	public MessageClassMapper getMessageClassMapper() {
		return messageClassMapper;
	}
	
	public void setMessageClassMapper(MessageClassMapper messageClassMapper) {
		this.messageClassMapper = messageClassMapper;
	}
	
	private Message createMessage(byte[] messageId) {
		// Get message class from class mapper
		Class<? extends Message> messageClass = messageClassMapper.getMessageClassById(messageId);
		if (messageClass == null) {
			throw new UndefinedMessageIdException(messageId, "Not existing message ID! '0x" + ByteArrayConverter.byteArrayToHexWithoutSpace(messageId) + "'");
		}
		
		try {
			return messageClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate a message class from message bytes! messageId='0x" + ByteArrayConverter.byteArrayToHexWithoutSpace(messageId) + "'", e);
		}
	}
	
	/**
	 * This needs to be called in overridden {@link #decodeMessage}.
	 * 
	 * @param messageId
	 * @param data
	 * @param startIndex
	 * @param length
	 * @return
	 */
	protected final Message decodeMessagePayload(byte[] messageId, byte[] data, int startIndex, int length) {
		if (messageClassMapper == null) {
			Log.e(LOG_TAG, "Message class mapper is not set!");
			return null;
		}
		
		// Create a message instance
		Message message = createMessage(messageId);
		
		// Decode payload data
		try {
			message.decodePayload(data, startIndex, length);
			return message;
		} catch(Exception e) {
			try {
				String payload = ByteArrayConverter.byteArrayToHex(data, startIndex, startIndex + length);
				throw new RuntimeException("Failed to decode message! messageClass=" + message.getClass().getSimpleName() + ", payload=" + payload, e);
			} catch(Exception e2) {
				throw new RuntimeException("Failed to decode message! messageClass=" + message.getClass().getSimpleName(), e);
			}
		}
	}
	
	/**
	 * A subclass must implement this method to create a {@link Message}
	 * from byte data. The input buffer is passed through {@link ByteRingBuffer} class.
	 * If the passed byte data is not long enough to create message, just return null.
	 * This method will be called again once it gets more data from connection.
	 * 
	 * @param inputBuffer
	 * @return
	 */
	public abstract Message decodeMessage(ByteRingBuffer inputBuffer);
	
}
