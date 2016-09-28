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
 * @date Feb 5, 2014
 */
public abstract class MessageEncoder {
	
	private MessageClassMapper messageClassMapper;
	
	public MessageEncoder(MessageClassMapper messageClassMapper) {
		this.messageClassMapper = messageClassMapper;
	}
	
	protected final byte[] getMessageId(Class<? extends CommunicationMessage> messageClass) {
		return messageClassMapper.getMessageIdByClass(messageClass);
	}
	
	public MessageClassMapper getMessageClassMapper() {
		return messageClassMapper;
	}
	
	public void setMessageClassMapper(MessageClassMapper messageClassMapper) {
		this.messageClassMapper = messageClassMapper;
	}
	
	/**
	 * A subclass must implement this method to create an encoded byte array from {@link CommunicationMessage}.
	 * 
	 * @param message
	 * @return
	 */
	public abstract byte[]	encodeMessage(CommunicationMessage message);
	
}
