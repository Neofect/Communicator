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

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.neofect.communicator.bluetooth.BluetoothConnectionFactory;
import com.neofect.communicator.message.CommunicationMessage;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * @author neo.kim@neofect.com
 * @date 2014. 2. 4.
 */
public class Communicator {
	
	private static final String LOG_TAG = Communicator.class.getSimpleName();
	
	private static Communicator instance = new Communicator();
	
	static Communicator getInstance() {
		return instance;
	}
	
	/** Aliases for short type names */
	@SuppressWarnings("serial")
	private static class HandlerList extends ArrayList<CommunicationHandler<? extends Device>> {}
	@SuppressWarnings("serial")
	private static class HandlerListMap extends HashMap<Class<? extends Device>, HandlerList> {}
	
	private List<Connection>	connections	= new Vector<Connection>();
	private List<Device>		devices		= new Vector<Device>();
	private HandlerListMap		handlers	= new HandlerListMap();
	
	public static void connect(BluetoothDevice device, ConnectionType connectionType, CommunicationController<? extends Device> controller) {
		Connection connection = null;
		try {
			connection = BluetoothConnectionFactory.createConnection(device, connectionType, controller);
			connection.connect();
		} catch(Exception e) {
			String macAddress = (device == null ? "" : device.getAddress());
			instance.notifyFailedToConnect(connection, controller.getDeviceClass(), new Exception("Failed to connect to '" + macAddress + "'!", e));
		}
	}

	public static void disconnect(Device device) {
		try {
			device.getConnection().disconnect();
		} catch(Exception e) {
			Log.e(LOG_TAG, "", e);
		}
	}
	
	public static void disconnectAllConnections() {
		synchronized(instance) {
			for(Device device : instance.devices) {
				disconnect(device);
			}
			for(Connection connection : instance.connections) {
				try {
					connection.disconnect();
				} catch(Exception e) {
					Log.e(LOG_TAG, "", e);
				}
			}
		}
	}
	
	public static <T extends Device> void registerListener(CommunicationListener<T> listener) {
		synchronized(instance) {
			Class<T> deviceClass = getClassFromGeneric(listener);
			HandlerList handlerList;
			if(instance.handlers.containsKey(deviceClass)) {
				handlerList = instance.handlers.get(deviceClass);
				int handlerIndex = getHandlerIndexByListener(handlerList, listener);
				if(handlerIndex != -1) {
					Log.w(LOG_TAG,  "The listener is already registered!");
					return;
				}
			} else {
				handlerList = new HandlerList();
				instance.handlers.put(deviceClass, handlerList);
			}
			
			CommunicationHandler<T> handler = new CommunicationHandler<T>(listener);
			handlerList.add(handler);
			
			// Notify of already existing devices
			for(Device device : instance.devices) {
				if(device.getClass() == deviceClass) {
					notifyNewListenerOfExistingDevices(handler, device);
				}
			}
		}
	}
	
	public static <T extends Device> void unregisterListener(CommunicationListener<T> listener) {
		synchronized(instance) {
			Class<T> deviceClass = getClassFromGeneric(listener);
			if(!instance.handlers.containsKey(deviceClass)) {
				Log.w(LOG_TAG,  "The listener is not registered!");
				return;
			}
			
			HandlerList handlerList = instance.handlers.get(deviceClass);
			int handlerIndex = getHandlerIndexByListener(handlerList, listener);
			if(handlerIndex == -1) {
				Log.w(LOG_TAG,  "The listener is not registered!");
				return;
			}
			handlerList.remove(handlerIndex);
		}
	}
	
	public static List<Device> getConnectedDevices() {
		return instance.devices;
	}
	
	/**
	 * Returns the number of connected devices by device type. If the input param is null, it returns the number of all connected devices.
	 * 
	 * @param deviceClass
	 * @return
	 */
	public static int getNumberOfConnectedDevices(Class<? extends Device> deviceClass) {
		synchronized (instance) {
			if(deviceClass == null) {
				return instance.devices.size();
			}
			
			int count = 0;
			for(Device device : instance.devices) {
				if(device.getClass() == deviceClass) {
					++count;
				}
			}
			return count;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends Device> void notifyNewListenerOfExistingDevices(CommunicationHandler<T> handler, Device device) {
		handler.onDeviceConnected((T) device, true);
		if(device.isReady()) {
			handler.onDeviceReady((T) device, true);
		}
	}
	
	/**
	 * A neat way to get class type of generic.
	 * http://stackoverflow.com/a/3403976/576440
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Device> Class<T> getClassFromGeneric(CommunicationListener<T> listener) {
		try {
			Type superClass = listener.getClass().getGenericSuperclass();
			return (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
		} catch(Exception e) {
			throw new IllegalArgumentException("Failed to get parameterized class type from the given generic!", e);
		}
	}
	
	private static int getHandlerIndexByListener(HandlerList handlerList, CommunicationListener<? extends Device> listener) {
		synchronized(handlerList) {
			for(int i = 0; i < handlerList.size(); ++i) {
				if(handlerList.get(i).listener == listener) {
					return i;
				}
			}
			return -1;
		}
	}
	
	synchronized void notifyStartConnecting(Connection connection, Class<? extends Device> deviceClass) {
		connections.add(connection);
		
		if(!handlers.containsKey(deviceClass)) {
			return;
		}
		for(CommunicationHandler<?> handler : handlers.get(deviceClass)) {
			handler.onStartConnecting(connection);
		}
	}

	synchronized void notifyFailedToConnect(Connection connection, Class<? extends Device> deviceClass, Exception cause) {
		connections.remove(connection);
		
		if(!handlers.containsKey(deviceClass)) {
			return;
		}
		for(CommunicationHandler<?> handler : handlers.get(deviceClass)) {
			handler.onFailedToConnect(connection, cause);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyConnected(Device device) {
		Log.d(LOG_TAG, "notifyConnected() device=" + device.getConnection().getRemoteAddress());
		connections.remove(device.getConnection());
		devices.add(device);
		
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass)) {
			return;
		}
		for(CommunicationHandler handler : handlers.get(deviceClass)) {
			handler.onDeviceConnected(device, false);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDisconnected(Connection connection, Class<? extends Device> deviceClass) {
		Log.d(LOG_TAG, "notifyDisconnected() connection=" + connection.getDescription());
		connections.remove(connection);
		
		// Find a device with the disconnected connection
		for(Device device : devices) {
			if(device.getConnection() != connection) {
				continue;
			}
			devices.remove(device);
			if(!handlers.containsKey(deviceClass)) {
				return;
			}
			for(CommunicationHandler handler : handlers.get(deviceClass)) {
				handler.onDeviceDisconnected(device);
			}
			break;
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceReady(Device device) {
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass)) {
			return;
		}
		for(CommunicationHandler handler : handlers.get(deviceClass)) {
			handler.onDeviceReady(device, false);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceMessageProcessed(Device device, CommunicationMessage message) {
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass)) {
			return;
		}
		for(CommunicationHandler handler : handlers.get(deviceClass)) {
			handler.onDeviceMessageProcessed(device, message);
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceUpdated(Device device) {
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass)) {
			return;
		}
		for(CommunicationHandler handler : handlers.get(deviceClass)) {
			handler.onDeviceUpdated(device);
		}
	}
	
}
