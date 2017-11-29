# Communicator

Communicator is a framework for message-based binary communication on Android.

It separates connection-related logic and protocol implementation so that user can focus on the protocol implementation and business logic for application.

## Gradle

```gradle
dependencies {
    compile 'com.neofect.communicator:communicator:2.0'
}
```

## Features
* Independent protocol implementation
  * No concern for low level operations on connection
  * Reusable even though the type of connection is changed to another.
* Event driven
  * Processes like connecting, disconnection and packet transferring are done internally and related events are dispatched. It makes the structure of application simple.
* Various connection types are applicable.
  * Currently there are Bluetooth SPP and USB connection supported.
  * Bluetooth LE (BLE) connection is coming.

## How to use it

* [Counterpart](#counterpart)
* [Protocol Definition](#protocol-definition)
* [Protocol Implementation](#protocol-implementation)
* [Device Instance](#device-instance)
* [Controller](#controller)
* [Usage](#usage)


### Counterpart

Let say that we have a physical device as a counterpart to communicate with. The device is a tiny remote control having 4 buttons, which sends a button ID to Android app remotely via Bluetooth when any button pressed. It also alerts when battery is low. And our app can command the device to beep for some time.

### Protocol definition
To communicate with the device, we design a simple binary protocol. The protocol has three types of messages (also called as packet) as following.


#### ButtonPressedMessage
Incoming message (Device → App)

| |Header|Message ID|Button ID|
|:-:|:-:|:-:|:-:|
|Length|1 byte|1 byte|1 byte|
|Value|`0x9d`|`0x01`|From 0 to 3|


#### LowBatteryAlertMessage
Incoming message (Device → App)


| |Header|Message ID|
|:-:|:-:|:-:|
|Length|1 byte|1 byte|
|Value|`0x9d`|`0x02`|


#### StartBeepMessage
Outgoing message (App → Device)

| |Header|Message ID|Time duration|
|:-:|:-:|:-:|:-:|
|Length|1 byte|1 byte|2 bytes|
|Value|`0x9d`|`0x03`|Big-endian integer|


* Note : Any value can be picked as message header. Uncommon value is preferrable.

### Protocol Implementation

For implementing protocol, we need to have following four modules.

* A message decoder
* A message encoder
* Message classes
* A message class mapper

#### Message decoder
Message decoder parses a binary packet and then creates an instance of message class. Decoder is used for incoming messages. Let's create a message decoder called `SimpleRemoteDecoder` subclassing `MessageDecoder`.

(All source code can be found in `sample_app` in project)

```java
public class SimpleRemoteDecoder extends MessageDecoder {

    public SimpleRemoteDecoder() {
        super(new MessageMapper());
    }

    @Override
    public Message decodeMessage(ByteRingBuffer inputBuffer) {
        return null;
    }

}
```

Packet parsing is processed in `decodeMessage()`. The given parameter `inputBuffer` contains raw byte data received from communication channel like Bluetooth SPP connection. The method is called when any byte data is received.

We write parsing logic which reads raw data from the buffer and creates a message instance. If it succeeds to create a message instance then returns it. In some cases, it could fail because of lack of raw data or corrupted checksum, and so on. Then it returns null. In this case, `decodeMessage()` will be called again later when more raw data is ready in the input buffer.

According to the protocol, the decoder needs to find the header which locates in the very first of a message.

```java
// Find header byte
while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != 0x9d) {
    inputBuffer.consume(1);
}

// If failed to find header byte, just return null to try later
if(inputBuffer.getContentSize() == 0) {
    return null;
}
```

After header, read the message ID.

```java
if (inputBuffer.getContentSize() < 2) {
    return null;
}
byte messageId = inputBuffer.peek(1);
```

Figure out the length of message according to the message ID and make sure we have enough raw data for a message. Then read the whole message data from buffer.

```java
int messageLength = 0;
if (messageId == 0x01) {
    messageLength = 3;
} else if (messageId == 0x02) {
    messageLength = 2;
}

if (inputBuffer.getContentSize() < messageLength) {
    return null;
}

byte[] messageBytes = inputBuffer.read(messageLength);
```

Parsing process for the metadata like header and length is done here. Message decoder parses only common part of message. The actual payload such as button ID is processed by each message class. So the decoder passes the payload to the message class.

```java
// Create a message instance
byte[] messageIdArray = new byte[] { messageId };
CommunicationMessage message = decodeMessagePayload(messageIdArray, messageBytes, 2, messageLength - 2);
```

`decodeMessagePayload()` is a built-in method. Just pass the message ID (as a byte array) and payload data.

The method creates an instance of message class according to the message ID and passes the payload to the message instance. These are done internally. The actual payload parsing is done by message class itself. We will look into that in message section.

At last, return the message instance at the end of `decodeMessage()`.

Here is the complete `SimpleRemoteDecoder` source.

```java
public class SimpleRemoteDecoder extends MessageDecoder {

    public SimpleRemoteDecoder() {
        super(new MessageMapper());
    }

    @Override
    public Message decodeMessage(ByteRingBuffer inputBuffer) {
        final byte HEADER_BYTE = (byte) 0x9d;

        // Find header byte
        while(inputBuffer.getContentSize() > 0 && inputBuffer.peek(0) != HEADER_BYTE) {
            inputBuffer.consume(1);
        }

        // If failed to find header byte, just return null to try later
        if (inputBuffer.getContentSize() == 0) {
            return null;
        }

        // Get message ID
        if (inputBuffer.getContentSize() < 2) {
            return null;
        }
        byte messageId = inputBuffer.peek(1);

        // Figure out the length of message
        int messageLength = 0;
        if (messageId == 0x01) {
            messageLength = 3;
        } else if (messageId == 0x02) {
            messageLength = 2;
        }

        // Check if we have enough data for a message
        if (inputBuffer.getContentSize() < messageLength) {
            return null;
        }

        // Get whole message data
        byte[] messageBytes = inputBuffer.read(messageLength);

        // Create a message instance
        byte[] messageIdArray = new byte[] { messageId };
        Message message = decodeMessagePayload(messageIdArray, messageBytes, 2, messageLength - 2);

        // Return the message instance
        return message;
    }

}
```

##### Message encoder
Message encoder builds a binary packet from a message instance, which is opposite of message decoder. Encoder is used for outgoing messages. Let's create a message encoder called `SimpleRemoteEncoder` by subclassing `MessageEncoder`.

```java
public class SimpleRemoteEncoder extends MessageEncoder {

    public SimpleRemoteEncoder() {
        super(new MessageMapper());
    }

    @Override
    public byte[] encodeMessage(Message message) {
        return null;
    }

}
```

We need to write encoding logic in `encodeMessage()` method. The method receives an instance of `Message` and returns a byte array of encoded result. The actual payload is encoded by message class itself like the way of decoding.

```java
byte[] payload = message.encodePayload();
int payloadLength = (payload == null ? 0 : payload.length);
```

Then create a whole message byte array. And put header byte, message ID and payload.

```java
byte[] messageBytes = new byte[2 + payloadLength];

// Header
messageBytes[0] = (byte) 0x9d;

// Message ID
byte[] messageId = getMessageId(message.getClass());
System.arraycopy(messageId, 0, messageBytes, 1, messageId.length);

// Payload
System.arraycopy(payload, 0, messageBytes, 2, payloadLength);
```

You get the message ID by calling `getMessageId()` method. It is provided by the framework and it is related to message class mapper which will be covered later.

Then return the encoded result at the end of `encodeMessage()`.

Here is the complete `SimpleRemoteEncoder` source.

```java
public class SimpleRemoteEncoder extends MessageEncoder {

    public SimpleRemoteEncoder() {
        super(new MessageMapper());
    }

    @Override
    public byte[] encodeMessage(Message message) {
        final byte HEADER_BYTE = (byte) 0x9d;

        byte[] payload = message.encodePayload();
        int payloadLength = (payload == null ? 0 : payload.length);

        byte[] messageBytes = new byte[2 + payloadLength];

        // Header
        messageBytes[0] = HEADER_BYTE;

        // Message ID
        byte[] messageId = getMessageId(message.getClass());
        System.arraycopy(messageId, 0, messageBytes, 1, messageId.length);

        // Payload
        System.arraycopy(payload, 0, messageBytes, 2, payloadLength);

        return messageBytes;
    }

}
```

##### Message classes
Message class represents a message in protocol specification. We have 3 messages in the protocol so create corresponding message classes, `ButtonPressedMessage`, `LowBatteryAlertMessage` and `StartBeepMessage` by subclassing `MessageImpl`.

###### ButtonPressedMessage
This is an incoming message so it needs to override `decodePayload()` method.

```java
public class ButtonPressedMessage extends MessageImpl {

    private byte buttonId;

    public byte getButtonId() {
        return buttonId;
    }

    @Override
    public void decodePayload(byte[] data, int startIndex, int length) {
        buttonId = data[startIndex];
    }

}
```

###### LowBatteryAlertMessage
This is also an incoming message and it has no payload.

```java
public class LowBatteryAlertMessage extends MessageImpl {

    @Override
    public void decodePayload(byte[] data, int startIndex, int length) {
        // No payload
    }

}
```

###### StartBeepMessage
This is an outgoing message so it needs to override `encodePayload()` method.

```java
public class StartBeepMessage extends MessageImpl {

    private int timeDuration;

    public StartBeepMessage(int timeDuration) {
        this.timeDuration = timeDuration;
    }

    @Override
    public byte[] encodePayload() {
        return new byte[] {
                (byte) ((timeDuration >> 8) & 0xff),
                (byte) (timeDuration & 0xff)
        };
    }

}
```

##### Message class mapper
Message class mapper represents a table of message classes. The table is used to figure out which message class is needed for a certain message ID, or to get a message ID from a message class. For now we have only three messages, the table is simple. Let's create a message class mapper called `MessageMapper` implementing `MessageClassMapper` interface.

```java
public class MessageMapper implements MessageClassMapper {

    @Override
    public byte[] getMessageIdByClass(Class<? extends Message> messageClass) {
        if (messageClass == ButtonPressedMessage.class) {
            return new byte[] { 0x01 };
        } else if (messageClass == LowBatteryAlertMessage.class) {
            return new byte[] { 0x02 };
        } else if (messageClass == StartBeepMessage.class) {
            return new byte[] { 0x03 };
        }
        return null;
    }

    @Override
    public Class<? extends Message> getMessageClassById(byte[] messageId) {
        if (messageId[0] == 0x01) {
            return ButtonPressedMessage.class;
        } else if (messageId[0] == 0x02) {
            return LowBatteryAlertMessage.class;
        } else if (messageId[0] == 0x03) {
            return StartBeepMessage.class;
        }
        return null;
    }

}
```

* Note : Enum class can be used to keep it neat when the message table is big.

The protocol implementation is done now. We have only two more steps.

### Device Instance
We are going to have a virtual instance representing the real device - SimpleRemote.
Let's create a device class called `SimpleRemote` by subclassing `Device`.

```java
public class SimpleRemote extends Device {

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
    }

    @Override
    protected boolean processMessage(Message message) {
        return false;
    }
}
```

The subclass of `Device` must have a constructor which receives only one parameter of `Connection` class. This constructor is called by framework through reflection.

First, implement the operation method `startBeep()`.

```java
public void startBeep(int timeDuration) {
    StartBeepMessage message = new StartBeepMessage(timeDuration);
    getConnection().sendMessage(message);
}
```

The concept is easy and clear. Operation (or command) is done by creating a message and sending it. `sendMessage()` is a framework method.

Next, implement the `processMessage()` method. It handles all incoming messages. In according to the spec, there are two incoming messages.

```java
@Override
protected boolean processMessage(Message message) {
    if (message instanceof ButtonPressedMessage) {
        lastPressedButtonId = ((ButtonPressedMessage) message).getButtonId();
        Log.i(LOG_TAG, "onButtonPressed: buttonId=" + lastPressedButtonId);
        return true;
    } else if (message instanceof LowBatteryAlertMessage) {
        lowBattery = true;
        return true;
    } else {
        Log.w(LOG_TAG, "processMessage: Unknown message! message=" + message.getDescription());
    }
    return false;
}
```

It updates the instance's variables with incoming messages. This is how the device instance is synchronized with the remote real device.

And you need to look carefully the return value of `processMessage()`. Once a message is processed, and by its result any attributes of the device is updated, it returns true. Sometimes there are messages which don't make any change on device, then just return false. If device is updated, an event for the update is dispatched by framework.

The complete `SimpleRemote` is following.

```java
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
```

### Controller
One piece is left, it is Controller. A controller connects the three modules, the encoder, the decoder and the device. Let's create a controller `SimpleRemoteController` by subclassing `Controller`. It is a generic class which receives a subclass of `Device` as type. And `Controller` constructor receives a pair of encoder and decoder.

```java
public class SimpleRemoteController extends Controller<SimpleRemote> {

    public SimpleRemoteController() {
        super(new SimpleRemoteEncoder(), new SimpleRemoteDecoder());
    }
}
```

There are more things customizable in `Controller`, but for a simple communication this implementation is enough.

### Usage
All necessary steps to communicate with our SimpleRemote are done. Let's make some Android UI.

We use `Communicator.connect()` to connect to device. It asks for four parameters which are a `Context`, a connection type, a device's identifier and a `Controller`.

There are four kinds of connection types for now.

* Bluetooth SPP
* Bluetooth insecure SPP
* USB serial
* Dummy

The device identifier is literally an identifier for specific connection type. It will be a MAC address for Bluetooth, in other case it will be a name of USB device for USB serial connection. You can refer to the actual implementation in the sample.

And put a newly created `SimpleRemoteController` as 4th parameter.

After `Communicator.connect()` call, we get notified by any communication events through a listener. Register a listener to the communicator in `onResume()` and unregister it in `onPause()`.

Once a device is connected, `onDeviceConnected()` gets called with an instance of device. We keep it as a variable and use it when we want to command.

Please refer to the actual implementation in `sample_app` module.

## Dummy Connection

Communicator is for physical communication channel like Bluetooth and USB serial. But it has a functionality to communicate with non-real device using dummy connection.

Thanks to Communicator's feature of separation of protocol implementation and connection, the dummy connection can be utilized to implement protocol without real device and to test application independently to device firmware.

By subclassing `DummyPhysicalDevice` and putting some communication logic in it, you can connect to the dummy physical device through dummy connection. Please refer to `DummySimpleRemote` and `DummySimpleRemoteTest` in `sample_app`.

## Products
#### [RAPAEL Smart Glove][1]
* Bluetooth SPP connection


#### [CHEMION LED Glasses][2]
* Bluetooth LE (Customized)

#### [RAPAEL Smart Board][3]
* USB connection

[1]:http://www.rapaelhome.com/us/smart-glove-2/
[2]:https://www.instagram.com/chemionglasses/
[3]:http://www.rapaelhome.com/us/smart-board/

## License
Copyright 2017 Neofect Co., Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
