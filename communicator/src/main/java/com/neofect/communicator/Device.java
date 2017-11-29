/*
 * Copyright 2014-2015 Neofect Co., Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neofect.communicator;

import com.neofect.communicator.message.Message;

/**
 * Subclass must have a constructor which has one parameter of {@link Connection} instance. Because,
 * the subclass will be instantiated via reflection.
 * 
 * @author neo.kim@neofect.com
 * @date 2014. 2. 4.
 */
public abstract class Device {
	
	private String deviceName;
	private Connection connection;

	/**
	 * A subclass must return true if this device is updated by message processing.
	 * If it returns true, an event for device update is dispatched.
	 * 
	 * @param message
	 * @return
	 */
	protected abstract boolean processMessage(Message message);
	
	public Device(Connection connection) {
		this.connection = connection;
	}
	
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
}
