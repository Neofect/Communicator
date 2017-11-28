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
public class LowBatteryAlertMessage extends MessageImpl {
	
	@Override
	public void decodePayload(byte[] data, int startIndex, int length) {
		// No implementation needed
	}

}
