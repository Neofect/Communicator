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
public class StartBeepMessage extends MessageImpl {
	
	private int timeDuration;

	public StartBeepMessage(int timeDuration) {
		this.timeDuration = timeDuration;
	}
	
	@Override
	public byte[] encodePayload() {
		return new byte[] {
				(byte) ((timeDuration >> 8) & 0xff),
				(byte) (timeDuration & 0xff)
		};
	}
	
}
