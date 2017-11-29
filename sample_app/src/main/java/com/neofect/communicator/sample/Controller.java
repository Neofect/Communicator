package com.neofect.communicator.sample;

import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.message.Message;

/**
 * @author neo.kim@neofect.com
 * @date Nov 29, 2017
 */
public class Controller extends com.neofect.communicator.Controller<SimpleRemote> {

	private static final String LOG_TAG = "Controller";

	public Controller() {
		super(new Encoder(), new Decoder());

		addCallbackBeforeProcessInboundMessage((connection, message) -> onBeforeProcessMessage(connection, message));
		addCallbackAfterProcessInboundMessage((connection, message) -> onAfterProcessMessage(connection, message));
	}

	@Override
	protected void onConnected(Connection connection) {
		super.onConnected(connection);
	}

	@Override
	protected void onDisconnected(Connection connection) {
		super.onDisconnected(connection);
	}

	private boolean onBeforeProcessMessage(Connection connection, Message message) {
		Log.i(LOG_TAG, "onBeforeProcessMessage: message=" + message.getDescription());
		return false;
	}

	private boolean onAfterProcessMessage(Connection connection, Message message) {
		Log.i(LOG_TAG, "onAfterProcessMessage: message=" + message.getDescription());
		return false;
	}

}
