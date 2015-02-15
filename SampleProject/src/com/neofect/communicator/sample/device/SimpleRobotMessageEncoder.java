/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.device;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.message.MessageClassMapper;
import com.neofect.communicator.message.MessageEncoder;

/**
 * @author neo.kim@neofect.com
 * @date Feb 15, 2015
 */
public class SimpleRobotMessageEncoder extends MessageEncoder {

	public SimpleRobotMessageEncoder(MessageClassMapper messageClassMapper) {
		super(messageClassMapper);
	}

	@Override
	public byte[] encodeMessage(CommunicationMessage message) {
		return null;
	}

}
