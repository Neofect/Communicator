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
public class InvalidDeviceVersionException extends InappropriateDeviceException {

	private static final long serialVersionUID = 8735673267387047222L;

	private String versionRangeMin;
	private String versionRangeMax;
	private String actualVersion;
	
	public InvalidDeviceVersionException(Class<? extends Device> deviceClass, String message) {
		super(deviceClass, message);
	}
	
	public InvalidDeviceVersionException(Class<? extends Device> deviceClass, String message, Throwable cause) {
		super(deviceClass, message, cause);
	}
	
	public InvalidDeviceVersionException(Class<? extends Device> deviceClass, String versionRangeMin, String versionRangeMax, String actualVersion, String message) {
		super(deviceClass, message);
		this.versionRangeMin = versionRangeMin;
		this.versionRangeMax = versionRangeMax;
		this.actualVersion = actualVersion;
	}

	public InvalidDeviceVersionException(Class<? extends Device> deviceClass, String versionRangeMin, String versionRangeMax, String actualVersion, String message, Throwable cause) {
		super(deviceClass, message, cause);
		this.versionRangeMin = versionRangeMin;
		this.versionRangeMax = versionRangeMax;
		this.actualVersion = actualVersion;
	}

	public String getVersionRangeMin() {
		return versionRangeMin;
	}

	public String getVersionRangeMax() {
		return versionRangeMax;
	}

	public String getActualVersion() {
		return actualVersion;
	}
	
}
