/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.exception;

import com.neofect.communicator.Device;

/**
 * @author neo.kim@neofect.com
 * @date Jun 4, 2015
 */
public class InappropriateDeviceException extends RuntimeException {

	private static final long serialVersionUID = -3210074935029589278L;
	
	private Class<? extends Device> deviceClass;
	private byte expectedDeviceTypeId;
	private byte actualDeviceTypeId;
	
	public InappropriateDeviceException(Class<? extends Device> deviceClass, String message) {
		super(message);
		this.deviceClass = deviceClass;
	}
	
	public InappropriateDeviceException(Class<? extends Device> deviceClass, String message, Throwable cause) {
		super(message, cause);
		this.deviceClass = deviceClass;
	}
	
	public InappropriateDeviceException(Class<? extends Device> deviceClass, byte expectedDeviceTypeId, byte actualDeviceTypeId, String message) {
		super(message);
		this.deviceClass = deviceClass;
		this.expectedDeviceTypeId = expectedDeviceTypeId;
		this.actualDeviceTypeId = actualDeviceTypeId;
	}

	public InappropriateDeviceException(Class<? extends Device> deviceClass, byte expectedDeviceTypeId, byte actualDeviceTypeId, String message, Throwable cause) {
		super(message, cause);
		this.deviceClass = deviceClass;
		this.expectedDeviceTypeId = expectedDeviceTypeId;
		this.actualDeviceTypeId = actualDeviceTypeId;
	}
	
	public Class<? extends Device> getDeviceClass() {
		return deviceClass;
	}
	
	public byte getExpectedDeviceTypeId() {
		return expectedDeviceTypeId;
	}

	public byte getActualDeviceTypeId() {
		return actualDeviceTypeId;
	}
	
}
