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
public class OperateWheelsMessage extends CommunicationMessageImpl {
	
	private int leftWheelSpeed;
	private int rightWheelSpeed;
	
	public OperateWheelsMessage(int leftWheelSpeed, int rightWheelSpeed) {
		this.leftWheelSpeed = leftWheelSpeed;
		this.rightWheelSpeed = rightWheelSpeed;
	}
	
	@Override
	public byte[] encodePayload() {
		byte[] payload = new byte[2];
		payload[0] = (byte) leftWheelSpeed;
		payload[1] = (byte) rightWheelSpeed;
		return payload;
	}
	
}
