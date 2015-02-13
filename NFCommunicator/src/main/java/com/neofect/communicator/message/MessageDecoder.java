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

package com.neofect.communicator.message;

import com.neofect.communicator.util.ByteArrayConverter;
import com.neofect.communicator.util.ByteRingBuffer;
import com.neofect.communicator.util.Log;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public abstract class MessageDecoder {

	private static final String LOG_TAG = MessageDecoder.class.getSimpleName();
	
	private MessageClassMapper	messageClassMapper;
	
	public MessageDecoder(MessageClassMapper messageClassMapper) {
		this.messageClassMapper = messageClassMapper;
	}
	
	private CommunicationMessage createCommunicationMessageInstance(byte[] messageId) {
		// Get message class from class mapper
		Class<? extends CommunicationMessage> messageClass = messageClassMapper.getMessageClassById(messageId);
		if(messageClass == null)
			throw new IllegalArgumentException("Not existing message ID! '0x" + ByteArrayConverter.byteArrayToHexWithoutSpace(messageId) + "'");
		
		CommunicationMessage message = null;
		try {
			message = (CommunicationMessage) messageClass.newInstance();
		} catch (Exception e) {
			Log.e(LOG_TAG, "", e);
			return null;
		}
		return message;
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
	protected final CommunicationMessage decodeMessagePayload(byte[] messageId, byte[] data, int startIndex, int length) {
		if(messageClassMapper == null) {
			Log.e(LOG_TAG, "Message class mapper is not set!");
			return null;
		}
		
		// Create a message class
		CommunicationMessage message = createCommunicationMessageInstance(messageId);
		if(message == null) {
			Log.e(LOG_TAG, "Failed to instantiate a message class from message bytes! messageId=" + ByteArrayConverter.byteArrayToHex(messageId));
			return null;
		}
		
		// Decode payload data
		try {
			message.decodePayload(data, startIndex, length);
			return message;
		} catch(Exception e) {
			try {
				String payload = ByteArrayConverter.byteArrayToHex(data, startIndex, startIndex + length);
				Log.e(LOG_TAG, "Failed to decode message! messageClass=" + message.getClass().getSimpleName() + ", payload=" + payload, e); 
			} catch(Exception e2) {
				Log.e(LOG_TAG, "Failed to decode message! messageClass=" + message.getClass().getSimpleName(), e2); 
			}
			return null;
		}
	}
	
	/**
	 * A subclass must implement this method to create a {@link CommunicationMessage}
	 * from byte data. The input buffer is passed through {@link ByteRingBuffer} class.
	 * If the passed byte data is not long enough to create message, just return null.
	 * This method will be called again once it gets more data from connection.
	 * 
	 * @param inputBuffer
	 * @return
	 */
	public abstract CommunicationMessage decodeMessage(ByteRingBuffer inputBuffer);
	
}
