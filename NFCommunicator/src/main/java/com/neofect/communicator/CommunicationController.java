/****************************************************************************
Copyright (c) 2014-2015 Neofect Co., Ltd.

http://www.neofect.com

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 ****************************************************************************/

package com.neofect.communicator;

import android.util.Log;

import com.neofect.communicator.message.CommunicationMessage;
import com.neofect.communicator.message.MessageDecoder;
import com.neofect.communicator.message.MessageEncoder;

/**
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public class CommunicationController<T extends Device> {
	
	private static final String LOG_TAG = CommunicationController.class.getSimpleName();
	
	private Class<T> deviceClass;
	private T device;
	
	private MessageEncoder encoder;
	private MessageDecoder decoder;
	
	public CommunicationController(Class<T> deviceClass) {
		this.deviceClass = deviceClass;
	}
	
	public CommunicationController(Class<T> deviceClass, MessageEncoder encoder, MessageDecoder decoder) {
		this.deviceClass = deviceClass;
		this.encoder = encoder;
		this.decoder = decoder;
	}
	
	protected void onStartConnecting(Connection connection) {}
	protected void onFailedToConnect(Connection connection) {}
	protected void onConnected(T device) {}
	protected void onDisconnected(Connection connection) {}
	protected void onBeforeDeviceProcessInboundMessage(Connection connection, CommunicationMessage message) {}
	protected void onAfterDeviceProcessInboundMessage(Connection connection, CommunicationMessage message) {}
	
	/**
	 * This method should be called by subclass when it is determined whether a device is connected AND ready.
	 * @param device
	 */
	protected final void notifyDeviceReady(T device) {
		if(device.isReady()) {
			Log.e(LOG_TAG, "Device is already ready!");
			return;
		}
		device.setReady(true);
		Communicator.getInstance().notifyDeviceReady(device);
	}
	
	final T onConnectedInner(Connection connection) {
		device = Communicator.createDeviceInstance(connection, deviceClass);;
		onConnected(device);
		return device;
	}
	
	protected final T getDevice() {
		return device;
	}
	
	final Class<T> getDeviceClass() {
		return deviceClass;
	}
	
	final byte[] encodeMessage(CommunicationMessage message) {
		if(encoder == null) {
			Log.e(LOG_TAG, "Message parser is not set!");
			return null;
		} else if(message == null) {
			Log.e(LOG_TAG, "Given message instance is null!");
			return null;
		}
		
		byte[] encodedMessage = null;
		try {
			encodedMessage = encoder.encodeMessage(message);
		} catch(Exception e) {
			Log.e(LOG_TAG, "Failed to encode a message! '" + message.getDescription() + "'", e);
		}
		return encodedMessage;
	}
	
	/**
	 * This is only called by {@link Connection#handleReadData(byte[])}.
	 *  
	 * @param connection
	 */
	final void decodeRawMessageAndProcess(Connection connection) {
		if(decoder == null) {
			Log.e(LOG_TAG, "Message parser is not set!");
			return;
		}
		
		while(true) {
			CommunicationMessage message = null;
			try {
				message = decoder.decodeMessage(connection.getRingBuffer());
			} catch(Exception e) {
				Log.e(LOG_TAG, "Failed to parse message!", e);
			}
			if(message == null)
				break;
			processInboundMessage(connection, message);
		}
	}
	
	private void processInboundMessage(Connection connection, CommunicationMessage message) {
		try {
			onBeforeDeviceProcessInboundMessage(connection, message);
			if(device != null) {
				boolean deviceUpdated = device.processMessage(message);
				Communicator.getInstance().notifyDeviceMessageProcessed(device, message);
				if(deviceUpdated)
					Communicator.getInstance().notifyDeviceUpdated(device);
			}
			onAfterDeviceProcessInboundMessage(connection, message);
		} catch(Exception e) {
			Log.e(LOG_TAG, "Failed to process a message! '" + message.getDescription() + "'", e);
		}
	}
	
}
