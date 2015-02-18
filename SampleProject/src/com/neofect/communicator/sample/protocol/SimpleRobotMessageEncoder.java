/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.protocol;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.message.MessageEncoder;

/**
 * @author neo.kim@neofect.com
 * @date Feb 15, 2015
 */
public class SimpleRobotMessageEncoder extends MessageEncoder {

	public SimpleRobotMessageEncoder() {
		super(new SimpleRobotMessageClassMapper());
	}

	@Override
	public byte[] encodeMessage(CommunicationMessage message) {
		byte[] payload = message.encodePayload();
		int payloadLength = (payload == null ? 0 : payload.length);
		
		byte[] messageBytes = new byte[2 + payloadLength];
		
		// Header
		messageBytes[0] = (byte) 0x9d;
		
		// Message ID
		byte[] messageId = getMessageId(message.getClass());
		System.arraycopy(messageId, 0, messageBytes, 1, messageId.length);
		
		// Payload
		System.arraycopy(payload, 0, messageBytes, 2, payloadLength);
		
		return messageBytes;
	}

}
