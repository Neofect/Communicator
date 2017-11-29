package com.neofect.communicator.sample.dummy;

import android.util.Log;

import com.neofect.communicator.dummy.DummyConnection;
import com.neofect.communicator.dummy.DummyPhysicalDevice;

import static com.neofect.communicator.util.ByteArrayConverter.byteArrayToHex;

/**
 * Created by Neo on 2017. 11. 28..
 */

public class DummySimpleRemote extends DummyPhysicalDevice {

	private static final String LOG_TAG = "DummySimpleRemote";

	private Thread senderThread;

	private int lastPressedButton = 0;
	private long elapsedTimeForButtonPressed = 0;
	private long elapsedTimeForLowBatteryAlert = 0;

	public DummySimpleRemote(String deviceIdentifier, String deviceName) {
		super(deviceIdentifier, deviceName);
	}

	@Override
	protected void connect(final DummyConnection connection) {
		Log.d(LOG_TAG, "connect:");
		this.connection = connection;

		notifyConnected();
		startSenderThread();
	}

	@Override
	protected void disconnect() {
		Log.d(LOG_TAG, "disconnect:");
		stopSenderThread();
	}

	@Override
	protected void receive(byte[] data) {
		String message = byteArrayToHex(data);
		Log.d(LOG_TAG, "receive: Physical device received data. [" + message + "]");

//		if (message.equals(IDENTIFY_REQUEST)) {
//			processIdentifyRequest(data);
//		} else if (message.startsWith(DATA_CHUNK_REQUEST_HEADER)) {
//			processDataChunkRequest(data);
//		} else if (message.startsWith(DEVICE_INFO_REQUEST_HEADER)) {
//			processDeviceInfoRequest(data);
//		} else if (message.equals(SENSOR_STOP_REQUEST)) {
//			sensorInterval = 0;
//		} else if (message.startsWith(SENSOR_START_HEADER)) {
//			sensorInterval = Math.min(Math.max(10, data[3]), 255);
//			synchronized (senderThread) {
//				senderThread.notify();
//			}
//		}
	}

	private void sendData(long elapsedTime) {
		elapsedTimeForButtonPressed += elapsedTime;
		elapsedTimeForLowBatteryAlert += elapsedTime;

		while (elapsedTimeForButtonPressed > 1000) {
			elapsedTimeForButtonPressed -= 1000;
			sendButtonPressedMessage(lastPressedButton);
			lastPressedButton = (lastPressedButton + 1) % 4;
		}

		while (elapsedTimeForLowBatteryAlert > 3000) {
			elapsedTimeForLowBatteryAlert -= 3000;
			sendLowBatteryAlertMessage();
		}
	}

	private void sendButtonPressedMessage(int lastPressedButton) {
		byte[] message = new byte[3];
		message[0] = (byte) 0x9d;
		message[1] = (byte) 0x01;
		message[2] = (byte) lastPressedButton;
		notifyRead(message);
	}

	private void sendLowBatteryAlertMessage() {
		byte[] message = new byte[2];
		message[0] = (byte) 0x9d;
		message[1] = (byte) 0x02;
		notifyRead(message);
	}

	private void stopSenderThread() {
		if (senderThread != null) {
			senderThread.interrupt();
			senderThread = null;
		}
	}

	private void startSenderThread() {
		stopSenderThread();
		senderThread = new Thread(() -> {
			long timestamp = System.currentTimeMillis();
			while (true) {
				try {
					long now = System.currentTimeMillis();
					sendData(now - timestamp);
					timestamp = now;
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					Log.e(LOG_TAG, "", e);
				}
			}
		});
		senderThread.start();
	}

}
