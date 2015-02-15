# NFCommunicator

## Overview
===========
* [Features](#features)
* [Tutorial](#tutorial)



## Features
===========
Android communication framework for Bluetooth




## Tutorial
===========

### Prerequisite

You would already have a device you want to communicate with and a binary protocol specification for communication.

For tutorial, **_let say we have a simple robot device_** and we want to communicate with it through Bluetooth SPP profile with Android application.

##### Simple Robot

Our simple robot has two wheels under its bottom and a proximity sensor in front. We want to command the robot to move forward, backward or turn left or right. Also want to get the proximity value it detects.

##### Protocol
To communicate with with the robot, we design a simple binary protocol. The protocol has **_two types of messages_** (or called packet) as following. In this protocol, big-endian byte order is used in multi-bytes value.


###### Message for operating wheels
Type - Outgoing message (Host → Device)

Message ID - 0x01

| |Header|Length|Message ID|Left Wheel Speed|Right Wheel Speed|
|:-:|:-:|:-:|:-:|:-:|:-:|
|Length|1 byte|1 byte|1 byte|1 byte|1 byte|
|Value|0x9D|0x05|0x01|Any|Any|


###### Message for reporting proximity sensor value
Type - Incoming message (Device → Host)

Message ID - 0x02


| |Header|Length|Message ID|Proximity Sensor Value|
|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
|Length|1 byte|1 byte|1 byte|2 bytes|
|Value|0x9D|0x05|0x02|Any|


*Note : Header byte is '0x9D'*

### Protocol Implementation
To work with the framework, we need to create classes representing the protocol by subclassing framework classes.

For implementing protocol, there are four kinds of modules as follows.

* A message decoder
* A message encoder
* Message classes mapped to each defined message
* A communication controller

##### Message decoder
Message decoder parses a binary packet and then creates an instance of message class. Let's create a message encoder called `SimpleRobotMessageDecoder` subclassing `MessageDecoder`.
	
	public class SimpleRobotMessageDecoder extends MessageDecoder {

		public SimpleRobotMessageDecoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public CommunicationMessage decodeMessage(ByteRingBuffer inputBuffer) {
			return null;
		}

	}

Packet parsing is processed in `decodeMessage()`. The given parameter `inputBuffer` contains raw byte data received from communication channel (for now it's coming from Bluetooth SPP connection.)

We write parsing logic which reads raw data from the buffer and creates a message instance. If it succeeds to create a message instance then returns it. In some cases, it fails because of lack of raw data or corrupted checksum, and so on. Then it returns null.

According to the protocol, the decoder needs to find the header which locates in the very first of a message.

	// Find header byte
	while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != 0x9D)
		inputBuffer.consume(1);
		
	// If failed to find header byte, just return null to try later
	if(inputBuffer.getContentSize() == 0)
		return null;

After header, read the length of message and make sure we have enough raw data for a message.

	// Message length
	if(inputBuffer.getContentSize() < 2)
		return null;
	int messageLength = inputBuffer.peek(1);
	
	// Check if we have enough data for a message
	if(inputBuffer.getContentSize() < messageLength)
		return null;
		
	// Get whole message data
	byte[] messageBytes = inputBuffer.readWithoutConsume(messageLength);

Parsing process for the metadata like header and length is done here. Message parser parses only common part of message. The actual payload such as wheel speed and sensor value is processed by each message class.

	// Create a message instance
	byte messageId = messageBytes[2];
	byte[] messageIdArray = new byte[] { messageId };
	CommunicationMessage message = decodeMessagePayload(messageIdArray, messageBytes, 3, messageLength - 3);

`decodeMessagePayload()` method is not a kind of thing you implement. It resides framework side. You just pass the message ID (as a byte array), message bytes and an index of payload.

The method creates an instance of message class according to the message ID and passes the payload to the instance. These are done internally. The actual process of payload parsing is done by message class. We will look into in message step.

At last, remove the used raw data and return the message instance.

	// Consume the used data
	inputBuffer.consume(messageLength);
	return message;

The `SimpleRobotMessageDecoder` results as following.

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
			
			// Message length
			if(inputBuffer.getContentSize() < 2)
				return null;
			int messageLength = inputBuffer.peek(1);
			
			// Check if we have enough data for a message
			if(inputBuffer.getContentSize() < messageLength)
				return null;
			
			// Get whole message data
			byte[] messageBytes = inputBuffer.readWithoutConsume(messageLength);
			
			// Create a message instance
			byte messageId = messageBytes[2];
			byte[] messageIdArray = new byte[] { messageId };
			CommunicationMessage message = decodeMessagePayload(messageIdArray, messageBytes, 3, messageLength - 3);
			
			// Consume the used data
			inputBuffer.consume(messageLength);
			return message;
		}

	}







##### Subclass from `MessageEncoder`
First, create a message encoder by subclassing `MessageEncoder`. The message encoder 


	public class SimpleRobotMessageEncoder extends MessageEncoder {

		public SimpleRobotMessageEncoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public byte[] encodeMessage(CommunicationMessage message) {
			return null;
		}

	}





##### Message classes
The messages in protocol specification are implemented as message class for each.


###### `OperateWheels` message
###### `ReportProximitySensorValue` message





### Create corresponding classes

To work with the framework, we need to create some subclasses from the framework classes.


##### Subclass from `Device`
We need to create a class which represents our virtual simple robot by subclassing `Device`. The simple robot has one attribute of proximity sensor value.

	public class SimpleRobot extends Device {
		
		private short proximitySensorValue = 0;
		
		public SimpleRobot(Connection connection) {
			super(connection);
		}
		
		public short getProximitySensorValue() {
			return proximitySensorValue;
		}
		
		public void setProximitySensorValue(short proximitySensorValue) {
			this.proximitySensorValue = proximitySensorValue;
		}
		
		@Override
		protected boolean processMessage(CommunicationMessage message) {
			return false;
		}

	}

The subclass of `Device` must have a constructor which receives only `Connection`. It is called from framework during run-time. And we will take care of `processMessage()` later.









