/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample;

import com.neofect.communicator.message.Message;
import com.neofect.communicator.message.MessageDecoder;
import com.neofect.communicator.sample.message.MessageMapper;
import com.neofect.communicator.util.ByteRingBuffer;

/**
 * @author neo.kim@neofect.com
 * @date Feb 16, 2015
 */
public class Decoder extends MessageDecoder {

	public Decoder() {
		super(new MessageMapper());
	}

	@Override
	public Message decodeMessage(ByteRingBuffer inputBuffer) {
		final byte HEADER_BYTE = (byte) 0x9d;

		// Find header byte
		while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != HEADER_BYTE) {
			inputBuffer.consume(1);
		}

		// If failed to find header byte, try it later.
		if (inputBuffer.getContentSize() == 0) {
			return null;
		}

		// Get message ID
		if (inputBuffer.getContentSize() < 2) {
			return null;
		}
		byte messageId = inputBuffer.peek(1);
		
		// Figure out the length of message
		int messageLength = 0;
		if (messageId == 0x01) {
			messageLength = 3;
		} else if (messageId == 0x02) {
			messageLength = 2;
		}
		
		// Check if we have enough data for a message
		if (inputBuffer.getContentSize() < messageLength) {
			return null;
		}
		
		// Get whole message data
		byte[] messageBytes = inputBuffer.read(messageLength);
		
		// Create a message instance
		byte[] messageIdArray = new byte[] { messageId };
		Message message = decodeMessagePayload(messageIdArray, messageBytes, 2, messageLength - 2);
		
		// Return the message instance
		return message;
	}

}
