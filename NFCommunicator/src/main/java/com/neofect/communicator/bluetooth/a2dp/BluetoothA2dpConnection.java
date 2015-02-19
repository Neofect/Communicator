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
package com.neofect.communicator.bluetooth.a2dp;

import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetoothA2dp;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.neofect.communicator.CommunicationController;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Device;
import com.neofect.communicator.bluetooth.BluetoothConnection;

/**
 * @author neo.kim@neofect.com
 */
public class BluetoothA2dpConnection extends BluetoothConnection {

	private static final String LOG_TAG = BluetoothA2dpConnection.class.getSimpleName();
	
	private enum ConnectState {
		STATE_UNKNOWN		(-1),
		STATE_DISCONNECTED	(0),
		STATE_CONNECTING	(1),
		STATE_CONNECTED		(2),
		STATE_DISCONNECTING	(3),
		STATE_PLAYING		(4),
		;
		private final int value;
		ConnectState(int value) {
			this.value = value;
		}
		public static ConnectState getConnectState(int value) {
			for(ConnectState connectState : ConnectState.values()) {
				if(connectState.value == value)
					return connectState;
			}
			return STATE_UNKNOWN;
		}
	}
	
	private IBluetoothA2dp					a2dpService = null;
	BluetoothA2dpConnectivityCheckThread	connectivityCheckThread;
	
	private void cancelConnectivityCheckThread() {
		connectivityCheckThread.cancel();
		connectivityCheckThread = null;
	}
	
	ServiceConnection bluetoothServiceConnectionCallback = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			a2dpService = IBluetoothA2dp.Stub.asInterface(service);
            if(a2dpService != null) {
				Log.d(LOG_TAG, "onServiceConnected() : Bluetooth A2DP service is connected (API17+)");
				startConnect();
            }
            else
            {
            	Log.e(LOG_TAG, "onServiceConnected() : Failed to connect to Bluetooth A2DP service! (API17+)");
    			handleFailedToConnect();
            }
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
            a2dpService = null;
			handleDisconnected();
		}

	};
	
	public BluetoothA2dpConnection(BluetoothDevice device, CommunicationController<? extends Device> controller) {
		super(device, controller, ConnectionType.BLUETOOTH_A2DP);
	}

	@Override
	protected void connectProcess() {
		// Try to get the Bluetooth A2DP service instance
		tryToGetService();
	}
	
	private void tryToGetService() {
		// Get IBluetoothA2dp service instance
		try {
			if(Build.VERSION.SDK_INT < 17)
			{
				Class<?> classServiceManager = Class.forName("android.os.ServiceManager");
				Method methodGetService = classServiceManager.getDeclaredMethod("getService", String.class);
				IBinder binder = (IBinder) methodGetService.invoke(null, "bluetooth_a2dp");

				Class<?> classBluetoothA2dp = Class.forName("android.bluetooth.IBluetoothA2dp");
				Class<?> declaredClass = classBluetoothA2dp.getDeclaredClasses()[0];
				Method methodAsInterface = declaredClass.getDeclaredMethod("asInterface", IBinder.class);
				methodAsInterface.setAccessible(true);

				a2dpService = (IBluetoothA2dp) methodAsInterface.invoke(null, binder);
                if(a2dpService != null)
                	startConnect();
                else
        			handleFailedToConnect();
			}
			else {
				// TODO
				throw new Exception("Following is commented out by Neo Kim 2014.01.23 during refactoring");
//				Intent intent = new Intent(IBluetoothA2dp.class.getName());
//				
//				// bluetooth service connection callback will register iBinder of bluetooth service
//				boolean bindResult = LauncherService.getInstance().getApplicationContext().bindService(intent, bluetoothServiceConnectionCallback, Context.BIND_AUTO_CREATE );
//				if(bindResult == false) {
//					Log.e(LOG_TAG, "Failed to bind Bluetooth A2dp service (SDK > r17)");
//                	setStatus(ConnectionEvent.FAILED_TO_CONNECT);
//				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "", e);
		}
	}
	
	private void startConnect() {
		boolean isSucceededToStartConnect = false;

		if (a2dpService == null)
		{
			Log.e(LOG_TAG, "connect() A2DP service instance is null!");
		}
		else {
			// 2013-09-11 wjchoi modified : to fix lG OPTIMUS G PRO a2dp connection problem
			// try to reconnect several times temporary, when it failed to connect
			int tryLimitCount = 5;
			while(tryLimitCount > 0)
			{
				isSucceededToStartConnect = connectWithService();
				if(true == isSucceededToStartConnect)
					break;
				tryLimitCount--;
				try {
					Thread.sleep(800, 0); // tested for 500, 1000 .. 500 failed
				} catch (InterruptedException e) {
				}
			}
		}
		
		if(connectivityCheckThread != null)
			cancelConnectivityCheckThread();
		
		if(isSucceededToStartConnect) {
			// Start a thread which checks A2DP connectivity regularly.
			connectivityCheckThread = new BluetoothA2dpConnectivityCheckThread(this);
			connectivityCheckThread.start();
			handleConnecting();
		} else {
			handleFailedToConnect();
		}
	}

	@Override
	protected void disconnectProcess() {
		boolean isA2dpConnected = checkIsA2dpConnected();
		Log.d(LOG_TAG, "disconnectProcess() isA2dpConnected=" + isA2dpConnected);
		if(!isA2dpConnected)
			return;
		else if (a2dpService == null)
			Log.e(LOG_TAG, "disconnectProcess() A2DP service instance is null!");
		else
			disconnectWithService();
	}
		
	/**
	 * Checks this connection is still alive.
	 * @return return false if state is one of STATE_DISCONNECTED, STATE_CONNECTING, return true in STATE_CONNECTED, STATE_DISCONNECTING, STATE_PLAYING.
	 * otherwise return false ( ex: exception is occurred )
	 */
	boolean checkIsA2dpConnected() {
		int connectionStateValue = 0;
		try {
			if(Build.VERSION.SDK_INT < 11)
				connectionStateValue = a2dpService.getSinkState(getDevice());
			else
				connectionStateValue = a2dpService.getConnectionState(getDevice());
		} catch (RemoteException e) {
			Log.e(LOG_TAG, "Error on getting A2DP connected sinks", e);
			return false;
		}
		
		switch(ConnectState.getConnectState(connectionStateValue)) {
			case STATE_DISCONNECTED:
			case STATE_CONNECTING:
				return false;
			case STATE_CONNECTED:
			case STATE_DISCONNECTING:
			case STATE_PLAYING:
				return true;
			default:
				return false;
		}
	}
	
	void	onConnected() {
		handleConnected();
	}
	
	void	onDisconnected() {
		handleDisconnected();
		cancelConnectivityCheckThread();
	}
	
	void	onFailedToConnect() {
		handleFailedToConnect();
	}
	
	private boolean connectWithService() {
		try {
            //wjchoi@neofect.com modified : to fix a2dp connection problem for LG optimus g pro 
			//it seems that exception does not include failure of connection, so made it return result value 
            boolean connectResult = false;
			if(Build.VERSION.SDK_INT < 11) {
				connectResult = a2dpService.connectSink(getDevice());
				Log.d(LOG_TAG, "connectProcess() Called a2dpService.connectSink() " + connectResult);
			}
			else {
				connectResult = a2dpService.connect(getDevice());
				Log.d(LOG_TAG, "connectProcess() Called a2dpService.connect() " + connectResult);
			}
			return connectResult;
		} catch (RemoteException e) {
			Log.e(LOG_TAG, "", e);
			return false;
		}
	}
	
	private void disconnectWithService() {
		try {
			if(Build.VERSION.SDK_INT < 11) {
				a2dpService.disconnectSink(getDevice());
				Log.d(LOG_TAG, "disconnectProcess() Called a2dpService.disconnectSink()");
			} else {
				a2dpService.disconnect(getDevice());
				Log.d(LOG_TAG, "disconnectProcess() Called a2dpService.disconnect()");
			}
		} catch (RemoteException e) {
			Log.e(LOG_TAG, "", e);
		}
	}
	
}
