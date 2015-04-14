/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample;

import com.neofect.communicator.Connection;
import com.neofect.communicator.Device;
import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.sample.protocol.message.OperateWheelsMessage;
import com.neofect.communicator.sample.protocol.message.ReportProximitySensorMessage;

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
	
	public void operateWheels(int leftWheelSpeed, int rightWheelSpeed) {
		OperateWheelsMessage message = new OperateWheelsMessage(leftWheelSpeed, rightWheelSpeed);
		sendMessage(message);
	}
	
	@Override
	protected boolean processMessage(CommunicationMessage message) {
		if(message instanceof ReportProximitySensorMessage) {
			ReportProximitySensorMessage reportMessage = (ReportProximitySensorMessage) message;
			proximitySensorValue = reportMessage.getProximitySensorValue();
			return true;
		} else {
			// Error handling
		}
		return false;
	}

}