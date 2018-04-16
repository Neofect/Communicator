package com.neofect.communicator.bluetooth.le;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Controller;
import com.neofect.communicator.Device;
import com.neofect.communicator.bluetooth.BluetoothConnection;

import java.util.List;
import java.util.UUID;

/**
 * Created by Neo on 2018/03/06.
 */

public class BluetoothLeConnection extends BluetoothConnection {

	private static final String LOG_TAG = "BluetoothLeConnection";

	private final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

	private Context context;
	private UUID serviceUuid;
	private UUID writeCharacteristicUuid;
	private UUID readCharacteristicUuid;

	private BluetoothGatt bluetoothGatt;
	public  BluetoothGattCharacteristic writeCharacteristic;
	private int rssi;
	private Exception causeOfConnectionFailure;

	private BluetoothLeConnection(Context context, BluetoothDevice device, Controller<? extends Device> controller, ConnectionType connectionType) {
		super(device, controller, connectionType);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
			new Exception("Bluetooth LE is not supported for Android version " + Build.VERSION.SDK_INT);
		}
		this.context = context;
	}

	public int getRssi() {
		return rssi;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	protected void connectProcess() {
		Log.d(LOG_TAG, "connectProcess: ");
		bluetoothGatt = getBluetoothDevice().connectGatt(context, false, gattCallback);
		handleConnecting();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	protected void disconnectProcess() {
		Log.d(LOG_TAG, "disconnectProcess: ");
		if (bluetoothGatt == null) {
			Log.w(LOG_TAG, "disconnectProcess: GATT service is not connected!");
			return;
		}
		bluetoothGatt.disconnect();
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			handleAvailableData(characteristic);
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			Log.d(LOG_TAG, "onCharacteristicRead: status=" + status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				handleAvailableData(characteristic);
			} else {
				Log.e(LOG_TAG, "onCharacteristicRead: The status is not success! status=" + status);
			}
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Log.d(LOG_TAG, "onConnectionStateChange: newState=STATE_CONNECTED");
				bluetoothGatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Log.d(LOG_TAG, "onConnectionStateChange: newState=STATE_DISCONNECTED");
				cleanUp();
				if (getStatus() == Status.CONNECTING) {
					handleFailedToConnect(causeOfConnectionFailure);
				} else {
					handleDisconnected();
				}
			} else if (newState == BluetoothProfile.STATE_CONNECTING) {
				Log.d(LOG_TAG, "onConnectionStateChange: newState=STATE_CONNECTING");
			} else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
				Log.d(LOG_TAG, "onConnectionStateChange: newState=STATE_DISCONNECTING");
			} else {
				Log.d(LOG_TAG, "onConnectionStateChange: newState=" + newState);
			}
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status != BluetoothGatt.GATT_SUCCESS) {
				Log.e(LOG_TAG, "onServicesDiscovered: The status is not success! status=" + status);
				processFailedToConnect(new Exception("onServicesDiscovered: The status is not success! status=" + status));
				return;
			}
			Log.d(LOG_TAG, "onServicesDiscovered: ");

//			///////////
//			List<BluetoothGattService> services = gatt.getServices();
//			for (BluetoothGattService service : services) {
//				Log.e(LOG_TAG, "onServicesDiscovered: serviceUuid=" + service.getUuid());
//				for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
//					Log.e(LOG_TAG, "onServicesDiscovered: characteristic=" + characteristic.getUuid());
//				}
//			}
//			///////////

			BluetoothGattService service = gatt.getService(serviceUuid);
			if (service == null) {
				processFailedToConnect(new Exception("onServicesDiscovered: No GATT service of UUID '" + serviceUuid + "'!"));
				return;
			}

			writeCharacteristic = service.getCharacteristic(writeCharacteristicUuid);
			if (writeCharacteristic == null) {
				processFailedToConnect(new Exception("onServicesDiscovered: No characteristic for write! '" + writeCharacteristicUuid + "'"));
				return;
			}

			BluetoothGattCharacteristic readCharacteristic = service.getCharacteristic(readCharacteristicUuid);
			if (readCharacteristic == null) {
				processFailedToConnect(new Exception("onServicesDiscovered: No characteristic for read! '" + readCharacteristicUuid + "'"));
				return;
			}
			bluetoothGatt.setCharacteristicNotification(readCharacteristic, true);
			enableCccdNotification(readCharacteristic);
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d(LOG_TAG, "onDescriptorWrite: Success");
				handleConnected();
			} else {
				Log.e(LOG_TAG, "onDescriptorWrite: The status is not success! status=" + status);
			}
		}

	};

	private void processFailedToConnect(Exception cause) {
		Log.e(LOG_TAG, "processFailedToConnect: ", cause);
		causeOfConnectionFailure = cause;
		disconnectProcess();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void enableCccdNotification(BluetoothGattCharacteristic characteristic) {
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		bluetoothGatt.writeDescriptor(descriptor);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void write(byte[] data) {
		if (bluetoothGatt == null) {
			Log.w(LOG_TAG, "write: GATT service is not connected!");
			return;
		}
		writeCharacteristic.setValue(data);
		bluetoothGatt.writeCharacteristic(writeCharacteristic);
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void handleAvailableData(BluetoothGattCharacteristic characteristic) {
		if (readCharacteristicUuid.equals(characteristic.getUuid())) {
			final byte[] data = characteristic.getValue();
			if (data != null && data.length > 0) {
				handleReadData(data);
			}
		} else {
			Log.i(LOG_TAG, "handleAvailableData: UUID mismatched! UUID=" + characteristic.getUuid());
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void cleanUp() {
		if (bluetoothGatt == null) {
			return;
		}
		bluetoothGatt.close();
		bluetoothGatt = null;
	}

	public static class Builder {

		private Controller<? extends Device> controller;
		private Context context;
		private String deviceIdentifier;
		private String serviceUuid;
		private String writeCharacteristicUuid;
		private String readCharacteristicUuid;

		public Builder(Controller<? extends Device> controller, Context context) {
			this.controller = controller;
			this.context = context;
		}

		public Builder deviceIdentifier(String deviceIdentifier) {
			this.deviceIdentifier = deviceIdentifier;
			return this;
		}

		public Builder serviceUuid(String serviceUuid) {
			this.serviceUuid = serviceUuid;
			return this;
		}

		public Builder writeCharacteristicUuid(String writeCharacteristicUuid) {
			this.writeCharacteristicUuid = writeCharacteristicUuid;
			return this;
		}

		public Builder readCharacteristicUuid(String readCharacteristicUuid) {
			this.readCharacteristicUuid = readCharacteristicUuid;
			return this;
		}

		public BluetoothLeConnection build() {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceIdentifier);
			BluetoothLeConnection connection = new BluetoothLeConnection(context, device, controller, ConnectionType.BLUETOOTH_LE);

			connection.serviceUuid = UUID.fromString(this.serviceUuid);
			connection.writeCharacteristicUuid = UUID.fromString(this.writeCharacteristicUuid);
			connection.readCharacteristicUuid = UUID.fromString(this.readCharacteristicUuid);
			return connection;
		}
	}

}
