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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TcpConnection extends Connection {

	private static final String LOG_TAG = "TcpConnection";

	private static final int BUFFER_SIZE = 1024;

	private Executor executor = Executors.newSingleThreadExecutor();
	private String ip;
	private int port;
	private String endpointName;
	private Socket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private byte[] buffer = new byte[BUFFER_SIZE];

	public TcpConnection(String ip, int port, String endpointName, Controller<? extends Device> controller) {
		super(ConnectionType.TCP, controller);
		this.ip = ip;
		this.port = port;
		this.endpointName = endpointName;
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
		executor.execute(() -> {
			if (outputStream == null) {
				Log.e(LOG_TAG, "write: Not connected!");
				return;
			}
			try {
				outputStream.write(data);
			} catch (IOException e) {
				Log.e(LOG_TAG, "write: ", e);
			}
		});
	}

	private void startReadThread() {
		new Thread(() -> {
			while (inputStream != null) {
				try {
					int numberOfReadBytes = inputStream.read(buffer, 0, buffer.length);
					if (numberOfReadBytes < 0) {
						onDisconnected();
						return;
					}
					byte[] readData = new byte[numberOfReadBytes];
					System.arraycopy(buffer, 0, readData, 0, numberOfReadBytes);
					handleReadData(readData);
				} catch (IOException e) {
					Log.d(LOG_TAG, "run: IOException on read() '" + getDeviceIdentifier() + "'", e);
					onDisconnected();
					return;
				}
			}
		}).start();
	}

	private void onDisconnected() {
		clear();
		handleDisconnected();
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
		return endpointName != null ? endpointName : "";
	}

	@Override
	public String getDescription() {
		if (endpointName == null) {
			return getDeviceIdentifier();
		} else {
			return endpointName + "(" + getDeviceIdentifier() + ")";
		}
	}

}
