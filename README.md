# NFCommunicator
NFCommunicator is a framework for simplifying application structure which communicates with remote entity. It wraps all communication related implementation internally so make developers more focus on the business logic. And its modular structure makes application's structure more clearly and easy to test modules independently.

It works on Android runtime.


## Features
* Event driven
  * Processes like connecting, disconnection and packet transferring are done internally and related events are dispatched. It makes application's structure simple.
* Protocol is implemented as a indenpendent module
  * It is easy to test and maintain.
  * It is reusable even if connection type is changed to another.
* Various connection types are applicable.
  * Actual connection process and packet communication is an independent module and pluggable.
  * Currently there are only Bluetooth wireless connection types. But because the framework works on any type of binary serial protocol, it can be applicable for like serial port connection or TCP/IP connection if we implement. Then existing applications can use it without changing other part like protocol implementation.


## How to use it

* [Prerequisites](#prerequisites)
* [Protocol Implementation](#protocol-implementation)
* [Device module representing the real device](#device-module-representing-the-real-device)
* [Communication controller](#communication-controller)
* [Usage](#usage)


### Prerequisites

You would already have a device you want to communicate with and a binary protocol specification for communication.

For tutorial, **_let say we have a simple robot device_** and we want to communicate with it through Bluetooth SPP profile with Android application.

##### Simple Robot

Our simple robot has two wheels under its bottom and a proximity sensor in front. We want to command the robot to move forward, backward or turn left or right. Also want to get the proximity value it detects.

##### Protocol
To communicate with with the robot, we design a simple binary protocol. The protocol has **_two types of messages_** (or called packet) as following. In this protocol, big-endian byte order is used in multi-bytes value.


###### Message for operating wheels
Type - Outgoing message (Host → Device)

Message ID - 0x01

| |Header|Message ID|Left Wheel Speed|Right Wheel Speed|
|:-:|:-:|:-:|:-:|:-:|:-:|
|Length|1 byte|1 byte|1 byte|1 byte|
|Value|0x9D|0x01|Any|Any|


###### Message for reporting proximity sensor value
Type - Incoming message (Device → Host)

Message ID - 0x02


| |Header|Message ID|Proximity Sensor Value|
|:-:|:-:|:-:|:-:|:-:|:-:|
|Length|1 byte|1 byte|1 bytes|
|Value|0x9D|0x02|Any|


*Note : Header byte is '0x9D'*

### Protocol Implementation
To work with the framework, we need to create classes representing the protocol by subclassing framework classes.

For implementing protocol, we need to have following four modules.

* A message decoder
* A message encoder
* Message classes representing messages in protocol specification
* A message class mapper

##### Message decoder
Message decoder parses a binary packet and then creates an instance of message class. Decoder is used for incoming messages. Let's create a message decoder called `SimpleRobotMessageDecoder` subclassing `MessageDecoder`.
	
	public class SimpleRobotMessageDecoder extends MessageDecoder {

		public SimpleRobotMessageDecoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public CommunicationMessage decodeMessage(ByteRingBuffer inputBuffer) {
			return null;
		}

	}

Packet parsing is processed in `decodeMessage()`. The given parameter `inputBuffer` contains raw byte data received from communication channel (for now it's coming from Bluetooth SPP connection.) The method is called by framework once any additional raw data is received.

We write parsing logic which reads raw data from the buffer and creates a message instance. If it succeeds to create a message instance then returns it. In some cases, it could fail because of lack of raw data or corrupted checksum, and so on. Then it returns null. In this case, `decodeMessage()` will be called again later when more raw data is ready in the input buffer.

According to the protocol, the decoder needs to find the header which locates in the very first of a message.

	// Find header byte
	while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != 0x9D)
		inputBuffer.consume(1);
		
	// If failed to find header byte, just return null to try later
	if(inputBuffer.getContentSize() == 0)
		return null;

After header, read the message ID.

	// Get message ID
	if(inputBuffer.getContentSize() < 2)
		return null;
	byte messageId = inputBuffer.peek(1);

Figure out the length of message according to the message ID and make sure we have enough raw data for a message.

	// Figure out the length of message
	int messageLength = 0;
	if(messageId == 0x01)
		messageLength = 4;
	else if(messageId == 0x02)
		messageLength = 3;
	
	// Check if we have enough data for a message
	if(inputBuffer.getContentSize() < messageLength)
		return null;

Read the whole message data from buffer.

	// Get whole message data
	byte[] messageBytes = inputBuffer.read(messageLength);


Parsing process for the metadata like header and length is done here. Message decoder parses only common part of message. The actual payload such as wheel speed and sensor value is processed by each message class.

	// Create a message instance
	byte[] messageIdArray = new byte[] { messageId };
	CommunicationMessage message = decodeMessagePayload(messageIdArray, messageBytes, 2, messageLength - 2);

`decodeMessagePayload()` method is not a kind of things you implement. It resides framework side. You just pass the message ID (as a byte array), message bytes and an index of payload.

The method creates an instance of message class according to the message ID and passes the payload to the instance. These are done internally. The actual process of payload parsing is done by message class. We will look into in message step.

At last, return the message instance.

	// Return the message instance
	return message;

The complete `SimpleRobotMessageDecoder` is following.

	public class SimpleRobotMessageDecoder extends MessageDecoder {

		public SimpleRobotMessageDecoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public CommunicationMessage decodeMessage(ByteRingBuffer inputBuffer) {
			// Find header byte
			while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != 0x9D)
				inputBuffer.consume(1);
				
			// If failed to find header byte, just return null to try later
			if(inputBuffer.getContentSize() == 0)
				return null;
			
			// Get message ID
			if(inputBuffer.getContentSize() < 2)
				return null;
			byte messageId = inputBuffer.peek(1);
			
			// Figure out the length of message
			int messageLength = 0;
			if(messageId == 0x01)
				messageLength = 4;
			else if(messageId == 0x02)
				messageLength = 3;
			
			// Check if we have enough data for a message
			if(inputBuffer.getContentSize() < messageLength)
				return null;
			
			// Get whole message data
			byte[] messageBytes = inputBuffer.read(messageLength);
			
			// Create a message instance
			byte[] messageIdArray = new byte[] { messageId };
			CommunicationMessage message = decodeMessagePayload(messageIdArray, messageBytes, 2, messageLength - 2);
			
			// Return the message instance
			return message;
		}

	}

##### Message encoder
Message encoder builds a binary packet from an instance of message class. Encoder is used for outgoing messages. Let's create a message encoder called `SimpleRobotMessageEncoder` subclassing `MessageEncoder`.
	
	public class SimpleRobotMessageEncoder extends MessageEncoder {

		public SimpleRobotMessageEncoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public byte[] encodeMessage(CommunicationMessage message) {
			return null;
		}

	}

We need to write encoding logic in `encodeMessage()` method. The method receives an instance of `CommunicationMessage` and returns a byte array of encoded result. The actual payload is encoded by message class itself like the way of decoding.

	byte[] payload = message.encodePayload();
	int payloadLength = (payload == null ? 0 : payload.length);

Then create a whole message byte array. And put header byte and message ID.

	byte[] messageBytes = new byte[2 + payloadLength];
	
	// Header
	messageBytes[0] = (byte) 0x9d;
	
	// Message ID
	byte[] messageId = getMessageId(message.getClass());
	System.arraycopy(messageId, 0, messageBytes, 1, messageId.length);
	
You get the message ID by calling `getMessageId()` method. It is provided by the framework and it is related to message class mapper which will be covered later.

Next, copy the encoded payload into the message byte array.

	// Payload
	System.arraycopy(payload, 0, messageBytes, 2, payloadLength);

Then return the encoded result.

	return messageBytes;

The complete `SimpleRobotMessageEncoder` is following.

	public class SimpleRobotMessageEncoder extends MessageEncoder {

		public SimpleRobotMessageEncoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public byte[] encodeMessage(CommunicationMessage message) {
			byte[] payload = message.encodePayload();
			int payloadLength = (payload == null ? 0 : payload.length);
			
			byte[] messageBytes = new byte[2 + payloadLength];
			
			// Header
			messageBytes[0] = (byte) 0x9d;
			
			// Message ID
			byte[] messageId = getMessageId(message.getClass());
			System.arraycopy(messageId, 0, messageBytes, 1, messageId.length);
			
			// Payload
			System.arraycopy(payload, 0, messageBytes, 2, payloadLength);
			
			return messageBytes;
		}

	}



##### Message classes
Message classes representing the messages in protocol specification. We have two messages in protocol so create two message classes, `OperateWheelsMessage` and `ReportProximitySensorMessage` by subclassing `CommunicationMessageImpl`.

###### OperateWheelsMessage
This is outgoing message so it needs to override `encodePayload()` method.

	public class OperateWheelsMessage extends CommunicationMessageImpl {
		
		private int leftWheelSpeed;
		private int rightWheelSpeed;
		
		public OperateWheelsMessage(int leftWheelSpeed, int rightWheelSpeed) {
			this.leftWheelSpeed = leftWheelSpeed;
			this.rightWheelSpeed = rightWheelSpeed;
		}
		
		@Override
		public byte[] encodePayload() {
			byte[] payload = new byte[2];
			payload[0] = (byte) leftWheelSpeed;
			payload[1] = (byte) rightWheelSpeed;
			return payload;
		}
		
	}

###### ReportProximitySensorMessage
This is incoming message so it needs to override `decodePayload()` method.

	public class ReportProximitySensorMessage extends CommunicationMessageImpl {
		
		private int proximitySensorValue;

		public int getProximitySensorValue() {
			return proximitySensorValue;
		}
		
		@Override
		public void decodePayload(byte[] data, int startIndex, int length) {
			proximitySensorValue = data[startIndex];
		}

	}

##### Message class mapper
This class represents a table of message classes. The table is used to figure out which message class is needed for a certain message ID, or to get a message ID from a message class. For now we have only two messages, the table is simple. Let's create a message class mapper called `SimpleRobotMessageClassMapper` implementing `MessageClassMapper` interface.

	public class SimpleRobotMessageClassMapper implements MessageClassMapper {

		@Override
		public Class<? extends CommunicationMessage> getMessageClassById(byte[] messageId) {
			return null;
		}

		@Override
		public byte[] getMessageIdByClass(Class<? extends CommunicationMessage> messageClass) {
			return null;
		}

	}

An enum class is a simple and neat way to define a table.

	private static enum MessageType {
		OPERATE_WHEELS		(OperateWheelsMessage.class,			(byte) 0x01),
		REPORT_PROX_VALUE	(ReportProximitySensorMessage.class,	(byte) 0x02),
		;
		
		public final Class<? extends CommunicationMessage> messageClass;
		public final byte messageId;
		
		private MessageType(Class<? extends CommunicationMessage> messageClass, byte messageId) {
			this.messageClass	= messageClass;
			this.messageId		= messageId;
		}
	}

The complete `SimpleRobotMessageClassMapper` is following.

	public class SimpleRobotMessageClassMapper implements MessageClassMapper {
		
		private static enum MessageType {
			OPERATE_WHEELS		(OperateWheelsMessage.class,			(byte) 0x01),
			REPORT_PROX_VALUE	(ReportProximitySensorMessage.class,	(byte) 0x01),
			;
			
			public final Class<? extends CommunicationMessage> messageClass;
			public final byte messageId;
			
			private MessageType(Class<? extends CommunicationMessage> messageClass, byte messageId) {
				this.messageClass	= messageClass;
				this.messageId		= messageId;
			}
		}
		
		@Override
		public byte[] getMessageIdByClass(Class<? extends CommunicationMessage> messageClass) {
			for(MessageType type : MessageType.values()) {
				if(type.messageClass == messageClass)
					return new byte[] { type.messageId };
			}
			return null;
		}
		
		@Override
		public Class<? extends CommunicationMessage> getMessageClassById(byte[] messageId) {
			for(MessageType type : MessageType.values()) {
				if(type.messageId == messageId[0])
					return type.messageClass;
			}
			return null;
		}

	}

The protocol side implementation is done now. We have only two more steps from now on.

### Device module representing the real device
We are going to have a virtual instance representing the real device. The instance has same attributes as real device, such as sensor values. The instance has same operations as real device, such as operating wheels. Let's create a device class called `SimpleRobot` by subclassing `Device`.

	public class SimpleRobot extends Device {
		
		private short proximitySensorValue = 0;
		
		public SimpleRobot(Connection connection) {
			super(connection);
		}
		
		public short getProximitySensorValue() {
			return proximitySensorValue;
		}
		
		public void operateWheels(int leftWheelSpeed, int rightWheelSpeed) {
			
		}
		
		@Override
		protected boolean processMessage(CommunicationMessage message) {
			return false;
		}

	}

The subclass of `Device` must have a constructor which receives only one parameter of `Connection` class. This constructor is called by the framework during run-time. 

First, implement the operation method.

	public void operateWheels(int leftWheelSpeed, int rightWheelSpeed) {
		OperateWheelsMessage message = new OperateWheelsMessage(leftWheelSpeed, rightWheelSpeed);
		sendMessage(message);
	}
	
The concept is easy and clear. Operation (or command) is done by creating a message and sending it. `sendMessage()` is a framework method.

Next, implement the `processMessage()` method. It handles all incoming messages. In this sample there is only one incoming message.

	@Override
	protected boolean processMessage(CommunicationMessage message) {
		if(message instanceof ReportProximitySensorMessage) {
			ReportProximitySensorMessage msg = (ReportProximitySensorMessage) message;
			proximitySensorValue = msg.getProximitySensorValue();
			return true;
		} else {
			// Error handling
		}
		return false;
	}

You need to look carefully the return value of `processMessage()`. Once a message is processed, and by its result any attributes of the device is updated, it must return true. Sometimes there are messages which don't update the device, then just return false. If device is updated, an event for the update is dispatched by framework.

The complete `SimpleRobot` is following.

	public class SimpleRobot extends Device {
		
		private short proximitySensorValue = 0;
		
		public SimpleRobot(Connection connection) {
			super(connection);
		}
		
		public short getProximitySensorValue() {
			return proximitySensorValue;
		}
		
		public void operateWheels(int leftWheelSpeed, int rightWheelSpeed) {
			OperateWheelsMessage message = new OperateWheelsMessage(leftWheelSpeed, rightWheelSpeed);
			sendMessage(message);
		}
		
		@Override
		protected boolean processMessage(CommunicationMessage message) {
			if(message instanceof ReportProximitySensorMessage) {
				ReportProximitySensorMessage reportMessage = (ReportProximitySensorMessage) message;
				proximitySensorValue = reportMessage.getProximitySensorValue();
				return true;
			} else {
				// Error handling
			}
			return false;
		}

	}

### Communication controller
Only one piece is left, it is communication controller. A communication controller connects the three modules, the encoder, the decoder and the device. Let's create a communication controller `SimpleRobotCommunicationController` by subclassing `CommunicationController`. It is a generic class which receives a subclass of `Device` as type.

	public class SimpleRobotCommunicationController extends CommunicationController<SimpleRobot> {

		public SimpleRobotCommunicationController() {
			super(SimpleRobot.class, new SimpleRobotMessageEncoder(), new SimpleRobotMessageDecoder());
		}

	}

To make it simple, the constructors of the encoder and the decoder are modified to have no parameters. The modified version is in sample project.

In fact, there are more things customizable in `CommunicationController`, but for a simple communication this implementation is enough.

### Usage
To talk with our simple robot, all necessary steps are done. Let's make some Android UI to communicate with our robot.

We call `Communicator.connect()` method to connect to device. It asks for three parameters which are a remote address, a connection type and a communication controller.

The remote address of a device is acquired using Bluetooth discovery. You can refer to the actual implementation in the sample project.

Second, there are three kinds of connection types for now.

* Bluetooth SPP
* Bluetooth insecure SPP
* Bluetooth A2DP

As NFCommunicator is structured to extend connection types, we can implement more connection types later.

Third, put a newly created `SimpleRobotCommunicationController` as parameter.

After `Communicator.connect()` call, we can get notified by any communication events through a listener. Register a listener to the communicator in `onResume()` and unregister it in `onPause()`.

Once a robot is connected, we keep its instance as variable given via  `onDeviceConnected()` and operate wheels by using it whenever we want.

Following is an example activity implementation.

	public class TestActivity extends Activity {
		
		private SimpleRobot robot;
		
		private CommunicationListener<SimpleRobot> listener = new CommunicationListener<SimpleRobot>() {
			
			@Override
			public void onStartConnecting(Connection connection) {
				updateConnectionStatus("Started connecting to '" + connection.getRemoteAddress() + "'");
			}
			
			@Override
			public void onFailedToConnect(Connection connection) {
				updateConnectionStatus("Failed to connect to '" + connection.getRemoteAddress() + "'");
			}

			@Override
			public void onDeviceConnected(SimpleRobot robot, boolean alreadyExisting) {
				TestActivity.this.robot = robot;
				updateConnectionStatus("Connected to '" + robot.getConnection().getRemoteAddress() + "'");
			}

			@Override
			public void onDeviceDisconnected(SimpleRobot robot) {
				updateConnectionStatus("Disconnected from '" + robot.getConnection().getRemoteAddress() + "'");
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
			setContentView(R.layout.activity_test);
			
			Button buttonConnect = (Button) this.findViewById(R.id.button_connect);
			buttonConnect.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String remoteAddress = "BLUETOOTH_MAC_ADDRESS";
					SimpleRobotCommunicationController controller = new SimpleRobotCommunicationController();
					Communicator.connect(remoteAddress, ConnectionType.BLUETOOTH_SPP, controller);
				}
			});
			
			Button buttonOperateWheels = (Button) this.findViewById(R.id.button_operate_wheels);
			buttonOperateWheels.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if(robot == null)
						return;
					robot.operateWheels(10, 10);
				}
			});

		}
		
		@Override
		public void onResume() {
			super.onResume();
			robot = null;
			Communicator.registerListener(listener);
		}
		
		@Override
		public void onPause() {
			super.onPause();
			Communicator.unregisterListener(listener);
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

You can find the sample project in `/SampleProject` folder.
