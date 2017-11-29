package com.neofect.communicator.sample;

import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.Controller;
import com.neofect.communicator.message.Message;

/**
 * @author neo.kim@neofect.com
 * @date Nov 29, 2017
 */
public class SimpleRemoteController extends Controller<SimpleRemote> {

	private static final String LOG_TAG = "SimpleRemoteController";

	public SimpleRemoteController() {
		super(new SimpleRemoteEncoder(), new SimpleRemoteDecoder());

		addCallbackBeforeProcessInboundMessage((connection, message) -> onBeforeProcessMessage(connection, message));
		addCallbackAfterProcessInboundMessage((connection, message) -> onAfterProcessMessage(connection, message));
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
