/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.message;

import com.neofect.communicator.message.Message;
import com.neofect.communicator.message.MessageClassMapper;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class MessageMapper implements MessageClassMapper {

	@Override
	public byte[] getMessageIdByClass(Class<? extends Message> messageClass) {
		if (messageClass == ButtonPressedMessage.class) {
			return new byte[] { 0x01 };
		} else if (messageClass == LowBatteryAlertMessage.class) {
			return new byte[] { 0x02 };
		} else if (messageClass == StartBeepMessage.class) {
			return new byte[] { 0x03 };
		}
		return null;
	}
	
	@Override
	public Class<? extends Message> getMessageClassById(byte[] messageId) {
		if (messageId[0] == 0x01) {
			return ButtonPressedMessage.class;
		} else if (messageId[0] == 0x02) {
			return LowBatteryAlertMessage.class;
		} else if (messageId[0] == 0x03) {
			return StartBeepMessage.class;
		}
		return null;
	}

}
