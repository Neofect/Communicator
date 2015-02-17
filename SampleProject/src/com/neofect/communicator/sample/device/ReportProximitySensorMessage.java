/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.device;

import com.neofect.communicator.message.CommunicationMessageImpl;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class ReportProximitySensorMessage extends CommunicationMessageImpl {
	
	private short proximitySensorValue;

	public short getProximitySensorValue() {
		return proximitySensorValue;
	}
	
	@Override
	public void decodePayload(byte[] data, int startIndex, int length) {
		proximitySensorValue = data[startIndex];
	}

}
