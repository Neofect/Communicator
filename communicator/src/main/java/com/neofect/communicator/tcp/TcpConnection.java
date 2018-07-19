package com.neofect.communicator.tcp;

import android.util.Log;

import com.neofect.communicator.Connection;
import com.neofect.communicator.ConnectionType;
import com.neofect.communicator.Controller;
import com.neofect.communicator.Device;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TcpConnection extends Connection {

	private static final String LOG_TAG = "TcpConnection";

	private static final int BUFFER_SIZE = 1024;

	private String ip;
	private int port;
	private Socket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private byte[] buffer = new byte[BUFFER_SIZE];

	public TcpConnection(String ip, int port, Controller<? extends Device> controller) {
		super(ConnectionType.TCP, controller);
		this.ip = ip;
		this.port = port;
	}

	@Override
	protected void connect() {
		handleConnecting();
		new Thread(() -> {
			try {
				socket = new Socket(ip, port);
				outputStream = socket.getOutputStream();
				inputStream = socket.getInputStream();
			} catch (IOException e) {
				clear();
				handleFailedToConnect(new RuntimeException("Failed to connect to '" + getDeviceIdentifier() + "'", e));
				return;
			}
			startReadThread();
			handleConnected();
		}).start();
	}

	@Override
	public void disconnect() {
		clear();
		handleDisconnected();
	}

	@Override
	public void write(byte[] data) {
		if (outputStream == null) {
			Log.e(LOG_TAG, "write: Not connected!");
			return;
		}
		try {
			outputStream.write(data);
		} catch (IOException e) {
			Log.e(LOG_TAG, "write: ", e);
		}
	}

	private void startReadThread() {
		new Thread(() -> {
			while (true) {
				try {
					int numberOfReadBytes = inputStream.read(buffer, 0, buffer.length);
					byte[] readData = new byte[numberOfReadBytes];
					System.arraycopy(buffer, 0, readData, 0, numberOfReadBytes);
					handleReadData(readData);
				} catch (IOException e) {
					Log.d(LOG_TAG, "run: IOException on read() '" + getDeviceIdentifier() + "'", e);
					clear();
					handleDisconnected();
					break;
				}
			}
		}).start();
	}

	private void clear() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e1) {
				Log.e(LOG_TAG, "clear: ", e1);
			}
			socket = null;
		}
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e1) {
				Log.e(LOG_TAG, "clear: ", e1);
			}
			outputStream = null;
		}
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e1) {
				Log.e(LOG_TAG, "clear: ", e1);
			}
			inputStream = null;
		}
	}

	@Override
	public String getDeviceIdentifier() {
		return ip + ":" + port;
	}

	@Override
	public String getDeviceName() {
		return getDeviceIdentifier();
	}

	@Override
	public String getDescription() {
		return getDeviceIdentifier();
	}

}
