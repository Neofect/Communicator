/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.exception;

/**
 * @author neo.kim@neofect.com
 * @date Dec 7, 2015
 */
public class UndefinedMessageIdException extends RuntimeException {

	private static final long serialVersionUID = 2096105385362735443L;
	
	private byte[] undefinedMessageId;
	
	private void copyFromParameter(byte[] undefinedMessageId) {
		this.undefinedMessageId = new byte[undefinedMessageId.length];
		System.arraycopy(undefinedMessageId, 0, this.undefinedMessageId, 0, undefinedMessageId.length);
	}

	public UndefinedMessageIdException(byte[] undefinedMessageId, String message) {
		super(message);
		copyFromParameter(undefinedMessageId);
	}
	
	public UndefinedMessageIdException(byte[] undefinedMessageId, String message, Throwable cause) {
		super(message, cause);
		copyFromParameter(undefinedMessageId);
	}
	
	public byte[] getUndefinedMessageId() {
		return undefinedMessageId;
	}

}
