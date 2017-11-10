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


public abstract class CommunicationListener<T extends Device> {
	
	public void onStartConnecting(Connection connection) {}
	public void onFailedToConnect(Connection connection, Exception cause) {}
	public void onDeviceConnected(T device, boolean alreadyExisting) {}
	public void onDeviceDisconnected(T device) {}
	public void onDeviceMessageProcessed(T device, Message message) {}
	public void onDeviceUpdated(T device) {}
	
}
