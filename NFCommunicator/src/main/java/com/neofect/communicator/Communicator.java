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

package com.neofect.communicator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.util.Log;

/**
 * @author neo.kim@neofect.com
 * @date 2014. 2. 4.
 */
public class Communicator {
	
	private static final String LOG_TAG = Communicator.class.getSimpleName();
	
	private static Communicator instance = new Communicator();
	
	public static Communicator getInstance() {
		return instance;
	}
	
	/** Aliases for short type name */
	@SuppressWarnings("serial")
	private static class HandlerList extends ArrayList<CommunicationHandler<? extends Device>> {}
	@SuppressWarnings("serial")
	private static class HandlerListMap extends HashMap<Class<? extends Device>, HandlerList> {}
	
	private List<Connection>	connections	= new Vector<Connection>();
	private List<Device>		devices		= new Vector<Device>();
	private HandlerListMap		handlers	= new HandlerListMap();
	
	public static void connect(String remoteAddress, ConnectionType connectionType, CommunicationController<? extends Device> controller) {
		try {
			Connection connection = ConnectionFactory.createConnection(remoteAddress, connectionType, controller);
			connection.connect();
		} catch(Exception e) {
			Log.e(LOG_TAG, "", e);
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
			for(Device device : instance.devices)
				disconnect(device);
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
				if(device.getClass() != deviceClass)
					continue;
				notifyNewListenerOfExistingDevices(handler, device);
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
	
	@SuppressWarnings("unchecked")
	private static <T extends Device> void notifyNewListenerOfExistingDevices(CommunicationHandler<T> handler, Device device) {
		handler.onDeviceConnected((T) device, true);
		if(device.isReady())
			handler.onDeviceReady((T) device, true);
	}
	
	/**
	 * A neat way to get class type of generic.
	 * http://stackoverflow.com/a/3403976/576440
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Device> Class<T> getClassFromGeneric(CommunicationListener<T> listener) {
		try {
			Type superclass = listener.getClass().getGenericSuperclass();
			return (Class<T>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
		} catch(Exception e) {
			throw new IllegalArgumentException("The given CommunicationListener is raw type. It must be parameterized with Device subclass!");
		}
	}
	
	private static int getHandlerIndexByListener(HandlerList handlerList, CommunicationListener<? extends Device> listener) {
		synchronized(handlerList) {
			for(int i = 0; i < handlerList.size(); ++i) {
				if(handlerList.get(i).listener == listener)
					return i;
			}
			return -1;
		}
	}
	
	static <T extends Device> T createDeviceInstance(Connection connection, Class<T> deviceClass) {
		// Create an instance of the device.
		T device = null;
		try {
			device = deviceClass.getDeclaredConstructor(Connection.class).newInstance(connection);
		} catch (Exception e) {
			Log.e(LOG_TAG, "Failed to instantiate an instance of device class!", e);
			return null;
		}
		return device;
	}
	
	synchronized void notifyStartConnecting(Connection connection, Class<? extends Device> deviceClass) {
		connections.add(connection);
		
		if(!handlers.containsKey(deviceClass))
			return;
		for(CommunicationHandler<?> handler : handlers.get(deviceClass))
			handler.onStartConnecting(connection);
	}

	synchronized void notifyFailedToConnect(Connection connection, Class<? extends Device> deviceClass) {
		connections.remove(connection);
		
		if(!handlers.containsKey(deviceClass))
			return;
		for(CommunicationHandler<?> handler : handlers.get(deviceClass))
			handler.onFailedToConnect(connection);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyConnected(Device device) {
		connections.remove(device.getConnection());
		devices.add(device);
		
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass))
			return;
		for(CommunicationHandler handler : handlers.get(deviceClass))
			handler.onDeviceConnected(device, false);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDisconnected(Connection connection, Class<? extends Device> deviceClass) {
		connections.remove(connection);
		
		// Find a device with the disconnected connection
		for(Device device : devices) {
			if(device.getConnection() != connection)
				continue;
			devices.remove(device);
			if(!handlers.containsKey(deviceClass))
				return;
			for(CommunicationHandler handler : handlers.get(deviceClass))
				handler.onDeviceDisconnected(device);
			break;
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceReady(Device device) {
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass))
			return;
		for(CommunicationHandler handler : handlers.get(deviceClass))
			handler.onDeviceReady(device, false);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceMessageProcessed(Device device, CommunicationMessage message) {
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass))
			return;
		for(CommunicationHandler handler : handlers.get(deviceClass))
			handler.onDeviceMessageProcessed(device, message);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceUpdated(Device device) {
		Class<? extends Device> deviceClass = device.getClass();
		if(!handlers.containsKey(deviceClass))
			return;
		for(CommunicationHandler handler : handlers.get(deviceClass))
			handler.onDeviceUpdated(device);
	}
	
}
