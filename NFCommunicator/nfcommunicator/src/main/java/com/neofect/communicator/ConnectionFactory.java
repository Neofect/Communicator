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

import com.neofect.communicator.bluetooth.BluetoothConnectionFactory;

public class ConnectionFactory {
	
	public static Connection createConnection(String remoteAddress, ConnectionType connectionType, CommunicationController<? extends Device> controller) {
		switch(connectionType) {
			case BLUETOOTH_SPP:
			case BLUETOOTH_SPP_INSECURE:
			case BLUETOOTH_A2DP:
				return BluetoothConnectionFactory.createConnection(remoteAddress, connectionType, controller);
			case BLUETOOTH_LOW_ENERGY:
				/**
				 * 2015.02.10 Neo Kim
				 * Low energy is removed now.
				 */
				break;
		}
		throw new IllegalArgumentException("Undefined connection type! '" + connectionType + "'");
	}
	
}
