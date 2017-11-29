/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample;

import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.Device;
import com.neofect.communicator.message.Message;
import com.neofect.communicator.sample.message.ButtonPressedMessage;
import com.neofect.communicator.sample.message.LowBatteryAlertMessage;
import com.neofect.communicator.sample.message.StartBeepMessage;

/**
 * @author neo.kim@neofect.com
 * @date Feb 14, 2015
 */
public class SimpleRemote extends Device {

	private static final String LOG_TAG = "SimpleRemote";

	private boolean lowBattery = false;
	private int lastPressedButtonId = -1;

	public SimpleRemote(Connection connection) {
		super(connection);
	}

	public boolean isLowBattery() {
		return lowBattery;
	}

	public int getLastPressedButtonId() {
		return lastPressedButtonId;
	}

	public void startBeep(int timeDuration) {
		StartBeepMessage message = new StartBeepMessage(timeDuration);
		sendMessage(message);
	}
	
	@Override
	protected boolean processMessage(Message message) {
		if (message instanceof ButtonPressedMessage) {
			lastPressedButtonId = ((ButtonPressedMessage) message).getButtonId();
			onButtonPressed(lastPressedButtonId);
			return true;
		} else if (message instanceof LowBatteryAlertMessage) {
			lowBattery = true;
			return true;
		} else {
			Log.w(LOG_TAG, "processMessage: Unknown message! message=" + message.getDescription());
		}
		return false;
	}

	private void onButtonPressed(int buttonId) {
		Log.i(LOG_TAG, "onButtonPressed: buttonId=" + buttonId);
	}

}
