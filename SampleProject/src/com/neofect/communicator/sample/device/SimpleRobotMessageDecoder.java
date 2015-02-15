/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.device;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.message.MessageClassMapper;
import com.neofect.communicator.message.MessageDecoder;
import com.neofect.communicator.util.ByteRingBuffer;
import com.neofect.smartrehab.glove.protocol.SmartGloveMessageConstants;

/**
 * @author neo.kim@neofect.com
 * @date Feb 16, 2015
 */
public class SimpleRobotMessageDecoder extends MessageDecoder {

	public SimpleRobotMessageDecoder(MessageClassMapper messageClassMapper) {
		super(messageClassMapper);
	}

	@Override
	public CommunicationMessage decodeMessage(ByteRingBuffer inputBuffer) {
		// Find header byte
		while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != 0x9D)
			inputBuffer.consume(1);
			
		// If failed to find header byte, just return null to try later
		if(inputBuffer.getContentSize() == 0)
			return null;
		
		// Message length
		if(inputBuffer.getContentSize() < 2)
			return null;
		int messageLength = inputBuffer.peek(1);
		
		// Check if we have enough data for a message
		if(inputBuffer.getContentSize() < messageLength)
			return null;
		
		// Get whole message data
		byte[] messageBytes = inputBuffer.readWithoutConsume(messageLength);
		
		// Create a message instance
		byte messageId = messageBytes[2];
		byte[] messageIdArray = new byte[] { messageId };
		CommunicationMessage message = decodeMessagePayload(messageIdArray, messageBytes, 3, messageLength - 3);
		
		// Consume the used data
		inputBuffer.consume(messageLength);
		return message;
	}

}
