package com.neofect.communicator.sample.dummy;

import android.util.Log;

import com.neofect.communicator.Communicator;
import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.dummy.DummyPhysicalDevice;
import com.neofect.communicator.dummy.DummyPhysicalDeviceManager;
import com.neofect.communicator.message.Message;
import com.neofect.communicator.sample.Controller;
import com.neofect.communicator.sample.SimpleRemote;
import com.neofect.communicator.sample.message.LowBatteryAlertMessage;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertTrue;

/**
 * Created by Neo on 2017. 11. 28..
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class DummySimpleRemoteTest {

	private static final String LOG_TAG = "DummySimpleRemoteTest";

	@BeforeClass
	public static void setUpClass() {
		ShadowLog.stream = new TimestampPrintStream();
	}

	@Test
	public void testDummy() throws Exception {
		final boolean[] result = new boolean[] { false };

		DummyPhysicalDevice dummyDevice = new DummySimpleRemote(
				"DUMMY_IDENTIFIER",
				"DUMMY_SIMPLE_REMOTE"
		);
		DummyPhysicalDeviceManager.register(dummyDevice);

		Communicator.registerListener(new Communicator.Listener<SimpleRemote>() {
			@Override
			public void onStartConnecting(Connection connection) {
				Log.i(LOG_TAG, "onStartConnecting: ");
			}

			@Override
			public void onFailedToConnect(Connection connection, Exception cause) {
				Log.i(LOG_TAG, "onFailedToConnect: ");
			}

			@Override
			public void onDeviceConnected(SimpleRemote device, boolean alreadyExisting) {
				Log.i(LOG_TAG, "onDeviceConnected: ");
			}

			@Override
			public void onDeviceDisconnected(SimpleRemote device) {
				Log.i(LOG_TAG, "onDeviceDisconnected: ");
			}

			@Override
			public void onDeviceMessageProcessed(SimpleRemote device, Message message) {
				Log.i(LOG_TAG, "onDeviceMessageProcessed: message=" + message.getDescription());

				// Test condition
				if (message instanceof LowBatteryAlertMessage) {
					result[0] = true;
				}
			}

		});

		Controller controller = new Controller();
		Communicator.connect(null, ConnectionType.DUMMY, dummyDevice.getDeviceIdentifier(), controller);

		final long TIMEOUT = 10000;
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime <= TIMEOUT && !result[0]) {
			Thread.sleep(5);
			ShadowLooper.getShadowMainLooper().idle();
		}
		Communicator.disconnectAllConnections();
		assertTrue(result[0]);
	}


}