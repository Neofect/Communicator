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

package com.neofect.communicator.message;

/**
 * @author neo.kim@neofect.com
 * @date Feb 5, 2014
 */
public abstract class MessageEncoder {
	
	private MessageClassMapper	messageClassMapper;
	
	public MessageEncoder(MessageClassMapper messageClassMapper) {
		this.messageClassMapper = messageClassMapper;
	}
	
	protected final byte[] getMessageId(Class<? extends CommunicationMessage> messageClass) {
		return messageClassMapper.getMessageIdByClass(messageClass);
	}
	
	/**
	 * A subclass must implement this method to create an encoded byte array from {@link CommunicationMessage}.
	 * 
	 * @param message
	 * @return
	 */
	public abstract byte[]	encodeMessage(CommunicationMessage message);
	
}
