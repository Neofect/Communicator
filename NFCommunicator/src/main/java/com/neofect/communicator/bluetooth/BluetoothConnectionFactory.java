/****************************************************************************
Copyright (c) 2014-2015 Neofect Co., Ltd.

http://www.neofect.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/

package com.neofect.communicator.bluetooth;

import android.bluetooth.BluetoothAdapter;
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

	public static Connection createConnection(final String macAddress, final ConnectionType connectionType, final CommunicationController<? extends Device> controller) {
		BluetoothDevice device = retrieveBluetoothDevice(connectionType, macAddress);
		
		// Create a bluetooth connection instance
		return createBluetoothConnection(device, controller, connectionType);
	}
	
	private static BluetoothDevice retrieveBluetoothDevice(final ConnectionType connectionType, final String macAddress) {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter == null)
			throw new IllegalStateException("Failed to retrieve the bluetooth adapter!");
		
		// Retrieve a bluetooth device from adapter
		return bluetoothAdapter.getRemoteDevice(macAddress);
	}
	
	private static BluetoothConnection createBluetoothConnection(BluetoothDevice device, CommunicationController<? extends Device> controller, ConnectionType connectionType) {
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
