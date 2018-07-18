package com.neofect.communicator.sample.dummy;

import android.util.Log;

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
	private long endTimeForBeep = 0;

	public DummySimpleRemote(String deviceIdentifier, String deviceName) {
		super(deviceIdentifier, deviceName);
	}

	@Override
	public void start(final TransferDelegate delegate) {
		Log.d(LOG_TAG, "start:");
		super.start(delegate);
		startSenderThread();
	}

	@Override
	public void stop() {
		Log.d(LOG_TAG, "stop:");
		stopSenderThread();
	}

	@Override
	public void put(byte[] data) {
		String message = byteArrayToHex(data);
		Log.d(LOG_TAG, "put: Physical device received data. [" + message + "]");

		try {
			if (message.startsWith("9d 03")) {
				int timeDuration = data[2] << 8 | data[3] & 0x000000ff;
				startBeep(timeDuration);
			} else {
				Log.e(LOG_TAG, "receive: Undefined message!");
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "receive:", e);
		}
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
		delegate.send(message);
	}

	private void sendLowBatteryAlertMessage() {
		byte[] message = new byte[2];
		message[0] = (byte) 0x9d;
		message[1] = (byte) 0x02;
		delegate.send(message);
	}

	private void startBeep(int timeDuration) {
		Log.i(LOG_TAG, "startBeep: timeDuration=" + timeDuration);
		if (endTimeForBeep != 0) {
			Log.i(LOG_TAG, "Start beep sound.");
		}
		endTimeForBeep = System.currentTimeMillis() + (timeDuration * 1000);
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
					if (endTimeForBeep != 0) {
						if (now > endTimeForBeep) {
							Log.i(LOG_TAG, "Stop beep sound.");
							endTimeForBeep = 0;
						} else {
							Log.i(LOG_TAG, "BEEP!");
						}
					}
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
