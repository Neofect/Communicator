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

For tutorial, **_let say we have a simple robot device_** and we want to implement an Android application which communicates with the robot.

##### Simple Robot

The robot has two wheels under its bottom and a proximity sensor in front. We want to command the robot to move forward, backward or turn left or right. Also want to get the proximity value it detects.

##### Protocol
To communicate with with the robot, we design a simple binary protocol. The protocol has **_two types of messages_** (or called packet) as following. In this protocol, big-endian byte order is used in multi-bytes value.


###### `OperateWheels` message
Type - Outgoing message (Host → Device)

Message ID - 0x01

| |Header|Message ID|Left Wheel Speed|Right Wheel Speed|Checksum|
|:-:|:------:|:----------:|:----------------:|:-----------------:|:--------:|
|Length|1 byte|1 byte|1 byte|1 byte|1 byte|


###### `ReportProximitySensorValue` message
Type - Incoming message (Device → Host)

Message ID - 0x02


| |Header|Message ID|Proximity Sensor Value|Checksum|
|:-:|:------:|:----------:|:----------------:|:--------:|
|Length|1 byte|1 byte|2 bytes|1 byte|

### Implementation

##### Message classes
The messages (also called packets)







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

##### Protocol implementation
It's time to start implementing protocol related classes. First, create a message encoder by subclassing `MessageEncoder`.



##### Subclass from `MessageEncoder`
It's time to start implementing protocol related classes. First, create a message encoder by subclassing `MessageEncoder`.

	public class SimpleRobotMessageEncoder extends MessageEncoder {

		public SimpleRobotMessageEncoder(MessageClassMapper messageClassMapper) {
			super(messageClassMapper);
		}

		@Override
		public byte[] encodeMessage(CommunicationMessage message) {
			return null;
		}

	}














