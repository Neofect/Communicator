package com.neofect.communicator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import com.neofect.communicator.bluetooth.le.BluetoothLeConnection;
import com.neofect.communicator.bluetooth.spp.BluetoothSppConnection;
import com.neofect.communicator.dummy.DummyConnection;
import com.neofect.communicator.dummy.DummyPhysicalDevice;
import com.neofect.communicator.dummy.DummyPhysicalDeviceManager;
import com.neofect.communicator.usb.UsbConnection;

/**
 * Created by Neo on 2018/03/06.
 */

class ConnectionFactory {

	static Connection create(Context context, ConnectionType connectionType, String deviceIdentifier, Controller<? extends Device> controller) throws Exception {
		switch (connectionType) {
			case BLUETOOTH_SPP:
			case BLUETOOTH_SPP_INSECURE: {
				BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceIdentifier);
				return new BluetoothSppConnection(device, controller, connectionType);
			}

			case BLUETOOTH_LE: {
				throw new Exception("Bluetooth LE connection cannot be created here. Use BluetoothLeConnection.Builder instead.");
			}

			case USB_SERIAL: {
				UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
				UsbDevice device = usbManager.getDeviceList().get(deviceIdentifier);
				if (device == null) {
					throw new Exception("Not existing USB device! deviceIdentifier=" + deviceIdentifier);
				}
				return new UsbConnection(context, device, controller);
			}

			case DUMMY: {
				DummyPhysicalDevice device = DummyPhysicalDeviceManager.getDevice(deviceIdentifier);
				if (device == null) {
					throw new Exception("Not existing Dummy physical device! identifier=" + deviceIdentifier);
				}
				return new DummyConnection(device, controller);
			}

			default: {
				throw new Exception("Unsupported connection type! '" + connectionType + "'");
			}
		}
	}

}
