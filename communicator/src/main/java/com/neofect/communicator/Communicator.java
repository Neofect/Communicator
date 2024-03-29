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

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.neofect.communicator.message.Message;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author neo.kim@neofect.com
 * @date 2014. 2. 4.
 */
public class Communicator {

	private static final String LOG_TAG = "Communicator";

	public abstract static class Listener<T extends Device> {
		public void onStartConnecting(Connection connection) {}
		public void onFailedToConnect(Connection connection, Exception cause) {}
		public void onDeviceConnected(T device, boolean alreadyExisting) {}
		public void onDeviceDisconnected(T device) {}
		public void onDeviceMessageProcessed(T device, Message message) {}
		public void onDeviceUpdated(T device) {}
	}

	private static final Communicator instance = new Communicator();

	static Communicator getInstance() {
		return instance;
	}

	/** Aliases for short type names */
	@SuppressWarnings("serial")
	private static class HandlerList extends ArrayList<CommunicatorHandler<? extends Device>> {}
	@SuppressWarnings("serial")
	private static class HandlerListMap extends LinkedHashMap<Class<? extends Device>, HandlerList> {}

	private final List<Connection> connections = new ArrayList<>();
	private final HandlerListMap registeredHandlers = new HandlerListMap();
	private final HandlerListMap connectedDeviceHandlers = new HandlerListMap();

	public static boolean connect(Context context, ConnectionType connectionType, String deviceIdentifier, Controller<? extends Device> controller) {
		Log.i(LOG_TAG, "connect: connectionType=" + connectionType + ", deviceIdentifier=" + deviceIdentifier);

		Connection existingConnection = getConnection(connectionType, deviceIdentifier);
		if (existingConnection != null) {
			Exception exception = new Exception("The connection is already in list! status=" + existingConnection.getStatus() + ", connectionType=" + connectionType + ", deviceIdentifier=" + deviceIdentifier);
			instance.notifyFailedToConnect(null, controller.getDeviceClass(), exception);
			return false;
		}

		Connection connection = null;
		try {
			connection = ConnectionFactory.create(context, connectionType, deviceIdentifier, controller);
			Log.d(LOG_TAG, "connect: '" + connection.getClass().getSimpleName() + "' is created.");
		} catch (Exception e) {
			e.printStackTrace();
			instance.notifyFailedToConnect(connection, controller.getDeviceClass(), e);
			return false;
		}
		return connect(connection);
	}

	public static boolean connect(Connection connection) {
		if (connection == null) {
			Log.e(LOG_TAG, "connect: Connection is null!");
			return false;
		}
		try {
			connection.connect();
			Log.i(LOG_TAG, "connect: Try to connect... '" + connection.getClass().getSimpleName() + "'");
			return true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "connect: Failed to connect! " + connection.getDescription(), e);
			Controller<?> controller = connection.getController();
			Class<? extends Device> deviceClass = (controller == null ? null : controller.getDeviceClass());
			instance.notifyFailedToConnect(connection, deviceClass, e);
			return false;
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
		synchronized (instance.connections) {
			for(Connection connection : instance.connections) {
				try {
					connection.disconnect();
				} catch(Exception e) {
					Log.e(LOG_TAG, "", e);
				}
			}
		}
	}

	public static <T extends Device> Handler registerListener(Listener<T> listener) {
		synchronized(instance) {
			Class<T> deviceClass = getClassFromGeneric(listener);
			HandlerList handlerList;
			if (instance.registeredHandlers.containsKey(deviceClass)) {
				handlerList = instance.registeredHandlers.get(deviceClass);
				int handlerIndex = getHandlerIndexByListener(handlerList, listener);
				if (handlerIndex != -1) {
					Log.w(LOG_TAG,  "registerListener: The listener is already registered!");
					return null;
				}
			} else {
				handlerList = new HandlerList();
				instance.registeredHandlers.put(deviceClass, handlerList);
			}

			CommunicatorHandler<T> handler = new CommunicatorHandler<>(listener);
			handlerList.add(handler);
			instance.refreshConnectedDeviceHandlers();

			// Notify of already existing devices
			for(Connection connection : instance.connections) {
				if (!connection.isConnected()) {
					continue;
				}
				Device device = connection.getController().getDevice();
				if (device == null) {
					continue;
				}
				if (isSameOrSuperClassDevice(device.getClass(), deviceClass)) {
					notifyNewListenerOfExistingDevices(handler, device);
				}
			}
			return handler;
		}
	}

	public static <T extends Device> void unregisterListener(Listener<T> listener) {
		synchronized (instance) {
			Class<T> deviceClass = getClassFromGeneric(listener);
			if (!instance.registeredHandlers.containsKey(deviceClass)) {
				Log.w(LOG_TAG,  "unregisterListener: The listener is not registered!");
				return;
			}

			HandlerList handlerList = instance.registeredHandlers.get(deviceClass);
			int handlerIndex = getHandlerIndexByListener(handlerList, listener);
			if (handlerIndex == -1) {
				Log.w(LOG_TAG,  "The listener is not existing!");
				return;
			}
			handlerList.remove(handlerIndex);
			instance.refreshConnectedDeviceHandlers();
		}
	}

	public static Connection getConnection(ConnectionType connectionType, String deviceIdentifier) {
		synchronized (instance.connections) {
			for (Connection connection : instance.connections) {
				if (connection.getConnectionType() == connectionType) {
					if (connection.getDeviceIdentifier().equals(deviceIdentifier)) {
						return connection;
					}
				}
			}
			return null;
		}
	}

	public static List<Device> getConnectedDevices() {
		synchronized (instance.connections) {
			List<Device> devices = new ArrayList<>();
			for (Connection connection : instance.connections) {
				if (!connection.isConnected()) {
					continue;
				}
				devices.add(connection.getController().getDevice());
			}
			return devices;
		}
	}

	public static boolean isConnected(ConnectionType connectionType, String deviceIdentifier) {
		Connection connection = getConnection(connectionType, deviceIdentifier);
		return connection != null && connection.isConnected();
	}

	public static Device findConnectedDevice(ConnectionType connectionType, String deviceIdentifier) {
		synchronized (instance.connections) {
			Connection connection = getConnection(connectionType, deviceIdentifier);
			if (connection != null) {
				return connection.getController().getDevice();
			}
			return null;
		}
	}

	/**
	 * Returns the number of connected devices by device type. If the input param is null, it returns the number of all connected devices.
	 *
	 * @param deviceClass
	 * @return
	 */
	public static int getNumberOfConnections(Class<? extends Device> deviceClass) {
		synchronized (instance.connections) {
			if (deviceClass == null) {
				return instance.connections.size();
			}

			int count = 0;
			for(Connection connection : instance.connections) {
				if (!connection.isConnected()) {
					continue;
				}
				Device device = connection.getController().getDevice();
				if (device.getClass() == deviceClass) {
					++count;
				}
			}
			return count;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Device> void notifyNewListenerOfExistingDevices(CommunicatorHandler<T> handler, Device device) {
		handler.onDeviceConnected((T) device, true);
	}

	/**
	 * A neat way to get class type of generic.
	 * http://stackoverflow.com/a/3403976/576440
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Device> Class<T> getClassFromGeneric(Listener<T> listener) {
		try {
			Type superClass = listener.getClass().getGenericSuperclass();
			return (Class<T>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
		} catch(Exception e) {
			throw new IllegalArgumentException("Failed to get parameterized class type from the given generic!", e);
		}
	}

	private static int getHandlerIndexByListener(HandlerList handlerList, Listener<? extends Device> listener) {
		for(int i = 0; i < handlerList.size(); ++i) {
			if (handlerList.get(i).listener == listener) {
				return i;
			}
		}
		return -1;
	}

	static boolean isSameOrSuperClassDevice(Class<? extends Device> subClass, Class<? extends Device> superClass) {
		try {
			subClass.asSubclass(superClass);
			return true;
		} catch (Exception e) {
			return false;
		}
	}


	synchronized private HandlerList getCorrespondingHandlers(Class<? extends Device> deviceClass) {
		synchronized (registeredHandlers) {
			HandlerList result = new HandlerList();
			List<Class<? extends Device>> handlerDeviceClassList = new ArrayList<>(registeredHandlers.keySet());
			for (int i = 0; i < handlerDeviceClassList.size(); ++i) {
				Class<? extends Device> handlerDeviceClass = handlerDeviceClassList.get(i);
				if (isSameOrSuperClassDevice(deviceClass, handlerDeviceClass)) {
					result.addAll(registeredHandlers.get(handlerDeviceClass));
				}
			}
			return result;
		}
	}

	synchronized private void refreshConnectedDeviceHandlers() {
		synchronized (connectedDeviceHandlers) {
			Log.d(LOG_TAG, "refreshConnectedDeviceHandlers: ");
			connectedDeviceHandlers.clear();
			for(Connection connection : instance.connections) {
				Device device = connection.getController().getDevice();
				if (device == null) {
					continue;
				}
				Class<? extends Device> deviceClass = device.getClass();
				if (connectedDeviceHandlers.get(deviceClass) != null) {
					continue;
				}
				connectedDeviceHandlers.put(deviceClass, getCorrespondingHandlers(deviceClass));
			}
		}
	}

	void onControllerReplaced(Connection connection, Controller<?> oldController, Controller<?> newController) {
		Log.d(LOG_TAG, "onControllerReplaced: ");
		refreshConnectedDeviceHandlers();
	}

	synchronized void notifyStartConnecting(Connection connection, Class<? extends Device> deviceClass) {
		synchronized (connections) {
			connections.add(connection);
		}

		HandlerList handlerList = getCorrespondingHandlers(deviceClass);
		for(CommunicatorHandler handler : handlerList) {
			handler.onStartConnecting(connection);
		}
	}

	synchronized void notifyFailedToConnect(Connection connection, Class<? extends Device> deviceClass, Exception cause) {
		synchronized (connections) {
			connections.remove(connection);
		}

		HandlerList handlerList = getCorrespondingHandlers(deviceClass);
		for(CommunicatorHandler handler : handlerList) {
			handler.onFailedToConnect(connection, cause);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyConnected(Device device) {
		Log.d(LOG_TAG, "notifyConnected: device=" + device.getConnection().getDescription());
		refreshConnectedDeviceHandlers();

		Class<? extends Device> deviceClass = device.getClass();
		HandlerList handlerList = connectedDeviceHandlers.get(deviceClass);
		if (handlerList == null) {
			return;
		}
		for(CommunicatorHandler handler : handlerList) {
			handler.onDeviceConnected(device, false);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDisconnected(Connection connection, Class<? extends Device> deviceClass) {
		Log.d(LOG_TAG, "notifyDisconnected: connection=" + connection.getDescription());
		HandlerList handlerList = connectedDeviceHandlers.get(deviceClass);
		if (handlerList != null) {
			for(CommunicatorHandler handler : handlerList) {
				handler.onDeviceDisconnected(connection.getController().getDevice());
			}
		}
		synchronized (connections) {
			connections.remove(connection);
		}
		refreshConnectedDeviceHandlers();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceMessageProcessed(Device device, Message message) {
		Class<? extends Device> deviceClass = device.getClass();
		HandlerList handlerList = connectedDeviceHandlers.get(deviceClass);
		if (handlerList == null) {
			return;
		}
		for(CommunicatorHandler handler : handlerList) {
			handler.onDeviceMessageProcessed(device, message);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	synchronized void notifyDeviceUpdated(Device device) {
		Class<? extends Device> deviceClass = device.getClass();
		HandlerList handlerList = connectedDeviceHandlers.get(deviceClass);
		if (handlerList == null) {
			return;
		}
		for(CommunicatorHandler handler : handlerList) {
			handler.onDeviceUpdated(device);
		}
	}

}
