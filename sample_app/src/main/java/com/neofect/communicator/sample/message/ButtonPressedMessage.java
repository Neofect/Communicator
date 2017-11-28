/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.message;

import com.neofect.communicator.message.MessageImpl;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class ButtonPressedMessage extends MessageImpl {
	
	private byte buttonId;

	public byte getButtonId() {
		return buttonId;
	}

	@Override
	public void decodePayload(byte[] data, int startIndex, int length) {
		buttonId = data[startIndex];
	}

}
