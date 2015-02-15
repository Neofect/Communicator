/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.device;

import com.neofect.communicator.Connection;
import com.neofect.communicator.Device;
import com.neofect.communicator.message.CommunicationMessage;

/**
 * @author neo.kim@neofect.com
 * @date Feb 14, 2015
 */
public class SimpleRobot extends Device {
	
	private short proximitySensorValue = 0;
	
	public SimpleRobot(Connection connection) {
		super(connection);
	}
	
	public short getProximitySensorValue() {
		return proximitySensorValue;
	}
	
	public void setProximitySensorValue(short proximitySensorValue) {
		this.proximitySensorValue = proximitySensorValue;
	}
	
	@Override
	protected boolean processMessage(CommunicationMessage message) {
		return false;
	}

}
