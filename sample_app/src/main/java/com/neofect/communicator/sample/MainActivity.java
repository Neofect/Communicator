package com.neofect.communicator.sample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.neofect.communicator.Communicator;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Controller;
import com.neofect.communicator.dummy.DummyPhysicalDevice;
import com.neofect.communicator.dummy.DummyPhysicalDeviceManager;
import com.neofect.communicator.sample.dummy.DummySimpleRemote;
import com.neofect.communicator.usb.UsbSerialDriverFactory;
import com.neofect.devicescanner.BluetoothScanner;
import com.neofect.devicescanner.DeviceScanner;
import com.neofect.devicescanner.DeviceScanner.DeviceScannerBuilder;
import com.neofect.devicescanner.ScannedDevice;
import com.neofect.devicescanner.UsbScanner;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

	private static final String LOG_TAG = "MainActivity";

	private static final int REQUEST_ENABLE_BLUETOOTH = 1;

	private static final String DEVICE_LIST_ITEM_SEPARATOR = " | ";

	private DeviceScanner scanner;
	private List<ScannedDevice> scannedDeviceList = new ArrayList<>();
	private List<String> scannedDeviceIdentifierList = new ArrayList<>();
	private ArrayAdapter<String> scannedDeviceAdapter;

	/**
	 * A instance of simple remote and a listener to communication events.
	 */
	private SimpleRemote device = null;

	private Communicator.Listener<SimpleRemote> communicatorListener = new Communicator.Listener<SimpleRemote>() {
		@Override
		public void onStartConnecting(Connection connection) {
			updateConnectionStatus("Connecting to '" + connection.getDeviceIdentifier() + "'");
		}

		@Override
		public void onFailedToConnect(Connection connection, Exception cause) {
			Log.e(LOG_TAG, "onFailedToConnect:", cause);
			toggleDisconnectVisibility(false);
			updateConnectionStatus(cause.getMessage());
		}

		@Override
		public void onDeviceConnected(SimpleRemote device, boolean alreadyExisting) {
			MainActivity.this.device = device;
			toggleDisconnectVisibility(true);
			updateConnectionStatus("Connected to '" + device.getConnection().getDeviceIdentifier() + "'");
		}

		@Override
		public void onDeviceDisconnected(SimpleRemote device) {
			toggleDisconnectVisibility(false);
			updateConnectionStatus("Disconnected '" + device.getConnection().getDeviceIdentifier() + "'");
			updateMessage("");
		}

		@Override
		public void onDeviceUpdated(SimpleRemote device) {
			String message = "Last pressed button ID - " + device.getLastPressedButtonId();
			message += "\nLow battery - " + device.isLowBattery();
			updateMessage(message);
		}

	};

	private DeviceScanner.Listener scannerListener = new DeviceScanner.Listener() {
		@Override
		public void onDeviceScanned(ScannedDevice scannedDevice) {
			Log.i(LOG_TAG, "onDeviceScanned: A device '" + scannedDevice.getDescription() + "' is scanned. '");
			addScannedDevice(scannedDevice);
		}

		@Override
		public void onExceptionRaised(Exception exception) {
			Log.e(LOG_TAG, "onExceptionRaised: ", exception);
			showToast(exception.getMessage());
		}

		@Override
		public void onScanFinished() {
			updateScanStatus();
			showToast("Scan finished.");
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initScanner();
		initButtons();
		initDeviceListView();

		checkAndRequestBluetoothEnabled();

		// Add dummy physical device for test
		DummyPhysicalDeviceManager.register(new DummySimpleRemote(
				"DUMMY_IDENTIFIER",
				"DUMMY_SIMPLE_REMOTE"
		));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		scanner.stop();
	}

	private void showToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	private void initScanner() {
		scanner = new DeviceScannerBuilder(getApplicationContext(), scannerListener)
				.addBluetoothType()
				.addUsbType(UsbSerialDriverFactory.getSupportingProducts())
				.build();
	}

	private boolean checkAndRequestBluetoothEnabled() {
		if (BluetoothAdapter.getDefaultAdapter() == null) {
			showToast("Bluetooth is not available!");
			return false;
		}

		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			// Request to enable bluetooth
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
			return false;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
			showToast("Bluetooth enabled!");
		}
	}

	private void initButtons() {
		// Scan
		Button buttonScan = (Button) this.findViewById(R.id.button_scan);
		buttonScan.setOnClickListener((v) -> {
			if (scanner.isScanning()) {
				scanner.stop();
			} else {
				startScan();
			}
		});

		// Disconnect
		Button buttonDisconnect = (Button) MainActivity.this.findViewById(R.id.button_disconnect);
		buttonDisconnect.setVisibility(View.INVISIBLE);
		buttonDisconnect.setOnClickListener((v) -> {
			if (device != null) {
				device.getConnection().disconnect();
			}
		});
	}

	private void initDeviceListView() {
		scannedDeviceAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1);
		ListView scannedDevices = (ListView) findViewById(R.id.listview_scanned_devices);
		scannedDevices.setAdapter(scannedDeviceAdapter);
		scannedDevices.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
			scanner.stop();

			ScannedDevice scannedDevice = scannedDeviceList.get(arg2);
			connectToDevice(scannedDevice);
			showToast(scannedDevice.getDescription());

			clearScannedDeviceList();
		});
	}

	private void updateScanStatus() {
		Button button = (Button) this.findViewById(R.id.button_scan);
		button.setText(scanner.isScanning() ? "Cancel" : "Scan");
	}

	private void clearScannedDeviceList() {
		scannedDeviceAdapter.clear();
		scannedDeviceList.clear();
	}

	private void startScan() {
		if (scanner.isScanning()) {
			showToast("Already scanning!");
			return;
		}

		// Clear device list view
		clearScannedDeviceList();

		// Start scan
		scanner.start();
		updateScanStatus();

		// Add dummy devices
		for (DummyPhysicalDevice device : DummyPhysicalDeviceManager.getDevices()) {
			ScannedDevice dummy = new ScannedDevice(
					device.getDeviceIdentifier(),
					device.getDeviceName(),
					device.getDeviceName(),
					device) {};
			addScannedDevice(dummy);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshUI();
		Communicator.registerListener(communicatorListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		Communicator.unregisterListener(communicatorListener);
	}

	private void addScannedDevice(ScannedDevice scannedDevice) {
		scannedDeviceList.add(scannedDevice);
		String identifier = scannedDevice.getIdentifier() + DEVICE_LIST_ITEM_SEPARATOR + scannedDevice.getDescription();
		scannedDeviceIdentifierList.add(identifier);
		scannedDeviceAdapter.add(identifier);
	}

	private void refreshUI() {
		boolean connected = false;
		if (device != null && device.getConnection() != null && device.getConnection().isConnected()) {
			connected = true;
		}
		toggleDisconnectVisibility(connected);
		updateConnectionStatus("");
		updateMessage("");
	}

	private void toggleDisconnectVisibility(boolean visible) {
		Button buttonDisconnect = (Button) MainActivity.this.findViewById(R.id.button_disconnect);
		buttonDisconnect.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	private void connectToDevice(ScannedDevice scannedDevice) {
		ConnectionType connectionType = getConnectionType(scannedDevice);
		String deviceIdentifier = scannedDevice.getIdentifier();
		Log.d(LOG_TAG, "connectToDevice: connectionType=" + connectionType + ", deviceIdentifier=" + deviceIdentifier);
		Controller controller = new Controller<SimpleRemote>(new SimpleRemoteEncoder(), new SimpleRemoteDecoder()) {};
		boolean result = Communicator.connect(this.getApplicationContext(), connectionType, deviceIdentifier, controller);
		if (!result) {
			showToast("Failed to connect to '" + deviceIdentifier + "'!");
		}
	}

	public static ConnectionType getConnectionType(ScannedDevice scannedDevice) {
		if (scannedDevice.getClass() == BluetoothScanner.BluetoothScannedDevice.class) {
			return ConnectionType.BLUETOOTH_SPP_INSECURE;
		} else if (scannedDevice.getClass() == UsbScanner.UsbScannedDevice.class) {
			return ConnectionType.USB_SERIAL;
		} else if (scannedDevice.getDevice() instanceof DummyPhysicalDevice) {
			return ConnectionType.DUMMY;
		}
		return null;
	}

	private void updateConnectionStatus(String status) {
		EditText connectionStatusText = (EditText) findViewById(R.id.connection_status);
		connectionStatusText.setText(status);
	}

	private void updateMessage(String sensorData) {
		EditText sensorDataText = (EditText) findViewById(R.id.sensor_data);
		sensorDataText.setText(sensorData);
	}

}
