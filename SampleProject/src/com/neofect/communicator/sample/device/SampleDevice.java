/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.device;

import com.neofect.communicator.Connection;
import com.neofect.communicator.Device;
import com.neofect.communicator.message.CommunicationMessage;

/**
 * @author neo.kim@neofect.com
 * @date Feb 14, 2015
 */
public class SampleDevice extends Device {
	
	
	
	/**
	 * @param connection
	 */
	public SampleDevice(Connection connection) {
		super(connection);
	}

	/* (non-Javadoc)
	 * @see com.neofect.communicator.Device#processMessage(com.neofect.communicator.message.CommunicationMessage)
	 */
	@Override
	protected boolean processMessage(CommunicationMessage message) {
		// TODO Auto-generated method stub
		return false;
	}

}
