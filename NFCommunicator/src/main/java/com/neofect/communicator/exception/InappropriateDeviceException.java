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
	
	public InappropriateDeviceException(Class<? extends Device> deviceClass, String message) {
		super(message);
		this.deviceClass = deviceClass;
	}
	
	public InappropriateDeviceException(Class<? extends Device> deviceClass, String message, Throwable cause) {
		super(message, cause);
		this.deviceClass = deviceClass;
	}
	
	public Class<? extends Device> getDeviceClass() {
		return deviceClass;
	}
	
}
