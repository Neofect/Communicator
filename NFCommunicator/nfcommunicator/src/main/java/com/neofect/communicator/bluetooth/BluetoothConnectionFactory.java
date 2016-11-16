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
package com.neofect.communicator.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.neofect.communicator.CommunicationController;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Device;
import com.neofect.communicator.bluetooth.a2dp.BluetoothA2dpConnection;
import com.neofect.communicator.bluetooth.spp.BluetoothSppConnection;

/**
 * @author neo.kim@neofect.com
 */
public class BluetoothConnectionFactory {

	public static Connection createConnection(BluetoothDevice device, final ConnectionType connectionType, final CommunicationController<? extends Device> controller) {
		switch(connectionType) {
		case BLUETOOTH_SPP:
		case BLUETOOTH_SPP_INSECURE:
			return new BluetoothSppConnection(device, controller, connectionType);
		case BLUETOOTH_A2DP:
			return new BluetoothA2dpConnection(device, controller);
		default:
			throw new IllegalArgumentException("Undefined bluetooth connection type '" + connectionType + "'");
		}
	}

}
