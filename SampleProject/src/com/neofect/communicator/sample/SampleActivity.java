package com.neofect.communicator.sample;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.neofect.communicator.CommunicationListener;
import com.neofect.communicator.Communicator;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;

public class SampleActivity extends Activity {
	
	private static final String LOG_TAG = SampleActivity.class.getSimpleName();
	
	private static final int REQUEST_ENABLE_BLUETOOTH		= 1;
	
	private static final String DEVICE_LIST_ITEM_SEPARATOR	= " | ";
	
	private boolean	discovering = false;
    private ArrayAdapter<String>	deviceListAdapter;
    
	/**
	 * A instance of simple robot and a listener to communication events.
	 */
	private SimpleRobot robot = null;
	
	private CommunicationListener<SimpleRobot> listener = new CommunicationListener<SimpleRobot>() {
		@Override
		public void onStartConnecting(Connection connection) {
			updateConnectionStatus("Connecting to '" + connection.getRemoteAddress() + "'");
		}
		
		@Override
		public void onFailedToConnect(Connection connection) {
			toggleButtonVisibility(false);
			updateConnectionStatus("Failed to connect to '" + connection.getRemoteAddress() + "'");
		}

		@Override
		public void onDeviceConnected(SimpleRobot robot, boolean alreadyExisting) {
			SampleActivity.this.robot = robot;
			toggleButtonVisibility(true);
			updateConnectionStatus("Connected to '" + robot.getConnection().getRemoteAddress() + "'");
		}

		@Override
		public void onDeviceDisconnected(SimpleRobot robot) {
			toggleButtonVisibility(false);
			updateConnectionStatus("Disconnected '" + robot.getConnection().getRemoteAddress() + "'");
			updateSensorData("");
		}

		@Override
		public void onDeviceUpdated(SimpleRobot robot) {
			updateSensorData("Proximity sensor - " + robot.getProximitySensorValue());
		}

	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_connection);
		
		initButtons();
		initDeviceListView();
		initReceiver();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		cancelDiscovery();
		this.unregisterReceiver(discoveryReceiver);
	}
	
	private void initButtons() {
		// Scan
		{
			Button buttonScan = (Button) this.findViewById(R.id.button_scan);
			buttonScan.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(discovering)
						cancelDiscovery();
					else
						startDiscovery();
				}
			});
		}
		
		// Disconnect
		{
			Button buttonDisconnect = (Button) SampleActivity.this.findViewById(R.id.button_disconnect);
			buttonDisconnect.setVisibility(View.INVISIBLE);
			buttonDisconnect.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(robot != null)
						robot.getConnection().disconnect();
				}
			});
		}
	}
	
	private void initDeviceListView() {
		deviceListAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
		ListView deviceListView = (ListView) findViewById(R.id.listview_scanned_devices);
		deviceListView.setAdapter(deviceListAdapter);
		deviceListView.setOnItemClickListener(new OnItemClickListener() {
	        @Override
	        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	        	String deviceListItem = deviceListAdapter.getItem(arg2);
	        	try {
	        		String deviceAddress = deviceListItem.split(DEVICE_LIST_ITEM_SEPARATOR)[0];
	        		connectToDevice(deviceAddress);
	        		Toast.makeText(getApplicationContext(), deviceAddress, Toast.LENGTH_SHORT).show();
	        	} catch(Exception e) {
	        		Log.e(LOG_TAG, "", e);
	        		Toast.makeText(getApplicationContext(), "Failed to parse device address!\n'" + deviceListItem + "'", Toast.LENGTH_LONG).show();
	        	}
	        }
		});
	}
	
	private void initReceiver() {
		// Register a receiver for broadcasts
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(discoveryReceiver, filter);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BLUETOOTH)
			startDiscovery();
	}
	
	private boolean checkAndRequestBluetoothEnabled() {
		if (BluetoothAdapter.getDefaultAdapter() == null) {
			Log.e(LOG_TAG, "Bluetooth is not available!");
			Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_LONG).show();
			return false;
		}
		
		if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			// Request to enable bluetooth
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
			return false;
		}
		return true;
	}
	
	private void changeDiscoveryStatus(boolean discovering) {
		Button button = (Button) this.findViewById(R.id.button_scan);
		if(discovering) {
			button.setText("Cancel");
			Log.d(LOG_TAG, "changeDiscoveryStatus() Started");
		}
		else {
			button.setText("Scan");
			Log.d(LOG_TAG, "changeDiscoveryStatus() Stopped");
		}
		this.discovering = discovering;
	}
	
	private void startDiscovery() {
		if(!checkAndRequestBluetoothEnabled())
			return;
		
		cancelDiscovery();
		
		// Clear device list view
		deviceListAdapter.clear();
		
		// request discover from BluetoothAdapter
		BluetoothAdapter.getDefaultAdapter().startDiscovery();
		
		changeDiscoveryStatus(true);
	}
	
	private void cancelDiscovery() {
		// Make sure we're not doing discovery anymore
		if(BluetoothAdapter.getDefaultAdapter() == null)
			return;
		if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
			BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
	}
	
	private void onDeviceDiscovered(BluetoothDevice device) {
		// Check duplicates
		for(int i = 0; i < deviceListAdapter.getCount(); ++i) {
			if(deviceListAdapter.getItem(i).startsWith(device.getAddress()))
				return;
		}
		deviceListAdapter.add(device.getAddress() + DEVICE_LIST_ITEM_SEPARATOR + device.getName());
	}
	
	private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				onDeviceDiscovered(device);
			} else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if(SampleActivity.this.discovering)
					changeDiscoveryStatus(false);
			}
		}
	};
	
	@Override
	public void onResume() {
		super.onResume();
		refreshUI();
		Communicator.registerListener(listener);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		Communicator.unregisterListener(listener);
	}
	
	private void refreshUI() {
		boolean connected = false;
		if(robot != null && robot.getConnection() != null && robot.getConnection().isConnected())
			connected = true;
		toggleButtonVisibility(connected);
		updateConnectionStatus("");
		updateSensorData("");
	}
	
	private void toggleButtonVisibility(boolean visible) {
		int visibility = (visible ? View.VISIBLE : View.INVISIBLE);
		Button buttonDisconnect = (Button) SampleActivity.this.findViewById(R.id.button_disconnect);
		buttonDisconnect.setVisibility(visibility);
	}
	
	private void connectToDevice(String deviceAddress) {
		Log.d(LOG_TAG, "connectToDevice() deviceAddress=" + deviceAddress);
		SimpleRobotCommunicationController controller = new SimpleRobotCommunicationController();
		Communicator.connect(deviceAddress, ConnectionType.BLUETOOTH_SPP, controller);
	}
	
	private void updateConnectionStatus(String status) {
		EditText connectionStatusText = (EditText) findViewById(R.id.connection_status);
		connectionStatusText.setText(status);
	}
	
	private void updateSensorData(String sensorData) {
		EditText sensorDataText = (EditText) findViewById(R.id.sensor_data);
		sensorDataText.setText(sensorData);
	}
	
}