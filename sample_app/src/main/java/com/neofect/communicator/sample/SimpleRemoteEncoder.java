/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample;

import com.neofect.communicator.message.Message;
import com.neofect.communicator.message.MessageEncoder;
import com.neofect.communicator.sample.message.MessageMapper;

/**
 * @author neo.kim@neofect.com
 * @date Feb 15, 2015
 */
public class SimpleRemoteEncoder extends MessageEncoder {

	public SimpleRemoteEncoder() {
		super(new MessageMapper());
	}

	@Override
	public byte[] encodeMessage(Message message) {
		final byte HEADER_BYTE = (byte) 0x9d;

		byte[] payload = message.encodePayload();
		int payloadLength = (payload == null ? 0 : payload.length);
		
		byte[] messageBytes = new byte[2 + payloadLength];
		
		// Header
		messageBytes[0] = HEADER_BYTE;
		
		// Message ID
		byte[] messageId = getMessageId(message.getClass());
		System.arraycopy(messageId, 0, messageBytes, 1, messageId.length);
		
		// Payload
		System.arraycopy(payload, 0, messageBytes, 2, payloadLength);
		
		return messageBytes;
	}

}
