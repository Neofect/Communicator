/*
 * Copyright 2014-2015 Neofect Co., Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neofect.communicator.message;

/**
 * @author neo.kim@neofect.com
 * @date 2014. 5. 22.
 */
public class CommunicationMessageImpl implements CommunicationMessage {

	@Override
	public String getDescription() {
		return "[" + getClass().getSimpleName() + "]";
	}

	@Override
	public byte[] encodePayload() {
		throw new UnsupportedOperationException("This message is designed only for receiving, not sending. So not supposed to be encoded.");
	}

	@Override
	public void decodePayload(byte[] data, int startIndex, int length) {
		throw new UnsupportedOperationException("This message is designed only for sending, not receiving. So not supposed to be decoded.");
	}
	
}
