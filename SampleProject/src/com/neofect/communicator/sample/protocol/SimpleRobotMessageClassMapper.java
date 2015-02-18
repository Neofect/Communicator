/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.protocol;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.message.MessageClassMapper;
import com.neofect.communicator.sample.protocol.message.OperateWheelsMessage;
import com.neofect.communicator.sample.protocol.message.ReportProximitySensorMessage;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class SimpleRobotMessageClassMapper implements MessageClassMapper {
	
	private static enum MessageType {
		OPERATE_WHEELS		(OperateWheelsMessage.class,			(byte) 0x01),
		REPORT_PROX_VALUE	(ReportProximitySensorMessage.class,	(byte) 0x02),
		;
		
		public final Class<? extends CommunicationMessage> messageClass;
		public final byte messageId;
		
		private MessageType(Class<? extends CommunicationMessage> messageClass, byte messageId) {
			this.messageClass	= messageClass;
			this.messageId		= messageId;
		}
	}
	
	@Override
	public byte[] getMessageIdByClass(Class<? extends CommunicationMessage> messageClass) {
		for(MessageType type : MessageType.values()) {
			if(type.messageClass == messageClass)
				return new byte[] { type.messageId };
		}
		return null;
	}
	
	@Override
	public Class<? extends CommunicationMessage> getMessageClassById(byte[] messageId) {
		for(MessageType type : MessageType.values()) {
			if(type.messageId == messageId[0])
				return type.messageClass;
		}
		return null;
	}

}
