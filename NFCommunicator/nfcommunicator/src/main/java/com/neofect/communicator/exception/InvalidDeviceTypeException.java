/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.exception;

import com.neofect.communicator.Device;

/**
 * @author neo.kim@neofect.com
 * @date Nov 11, 2015
 */
public class InvalidDeviceTypeException extends InappropriateDeviceException {

	private static final long serialVersionUID = 4081876494904858775L;
	
	private byte expectedDeviceTypeId;
	private byte actualDeviceTypeId;
	
	public InvalidDeviceTypeException(Class<? extends Device> deviceClass, String message) {
		super(deviceClass, message);
	}
	
	public InvalidDeviceTypeException(Class<? extends Device> deviceClass, String message, Throwable cause) {
		super(deviceClass, message, cause);
	}
	
	public InvalidDeviceTypeException(Class<? extends Device> deviceClass, byte expectedDeviceTypeId, byte actualDeviceTypeId, String message) {
		super(deviceClass, message);
		this.expectedDeviceTypeId = expectedDeviceTypeId;
		this.actualDeviceTypeId = actualDeviceTypeId;
	}

	public InvalidDeviceTypeException(Class<? extends Device> deviceClass, byte expectedDeviceTypeId, byte actualDeviceTypeId, String message, Throwable cause) {
		super(deviceClass, message, cause);
		this.expectedDeviceTypeId = expectedDeviceTypeId;
		this.actualDeviceTypeId = actualDeviceTypeId;
	}
	
	public byte getExpectedDeviceTypeId() {
		return expectedDeviceTypeId;
	}

	public byte getActualDeviceTypeId() {
		return actualDeviceTypeId;
	}
	
}
