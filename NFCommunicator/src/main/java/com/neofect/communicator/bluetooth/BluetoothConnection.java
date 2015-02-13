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

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.neofect.communicator.CommunicationController;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Device;

/**
 * @author neo.kim@neofect.com
 */
public abstract class BluetoothConnection extends Connection {

	private static final String LOG_TAG = BluetoothConnection.class.getSimpleName();
	
	private BluetoothDevice	device;
	private	boolean			disconnectRequested = false;
	
	public BluetoothConnection(BluetoothDevice device, CommunicationController<? extends Device> controller, ConnectionType connectionType) {
		super(connectionType, controller);
		this.device = device;
	}
	
	public final BluetoothDevice getDevice() {
		return device;
	}
	
	public final boolean isDisconnectRequested() {
		return disconnectRequested;
	}
	
	@Override
	public String getDescription() {
		return getDescriptionWithAddress();
	}
	
	@Override
	public String getRemoteAddress() {
		return getDevice().getAddress();
	}
	
	public String getDeviceName() {
		String deviceName = null;
		try {
			deviceName = getDevice().getName();
		} catch(Exception e) {
			Log.e(LOG_TAG, "", e);
		}
		if(deviceName == null)
			deviceName = "Unknown";
		return deviceName;
	}
	
	public final String	getDescriptionWithAddress() {
		return getDeviceName() + "(" + getRemoteAddress() + ")-" + getConnectionType();
	}
	
	@Override
	public final void connect() {
		Log.d(LOG_TAG, "connect() device= '" + getDescriptionWithAddress() + "'");
		if(getStatus() == Status.NOT_CONNECTED) {
			disconnectRequested = false;
			connectProcess();
		} else {
			Log.e(LOG_TAG, "connect() '" + getDescriptionWithAddress() + "' is not in the status of to connect! Status=" + getStatus());
			return;
		}
		
	}
	
	@Override
	public final void disconnect() {
		Log.d(LOG_TAG, "disconnect() device='" + getDescriptionWithAddress() + "'");
		disconnectRequested = true;
		
		disconnectProcess();
	}
	
	protected abstract void	connectProcess();
	protected abstract void	disconnectProcess();

}
