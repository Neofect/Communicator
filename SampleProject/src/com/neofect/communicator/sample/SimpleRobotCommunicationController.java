/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample;

import com.neofect.communicator.CommunicationController;
import com.neofect.communicator.sample.protocol.SimpleRobotMessageDecoder;
import com.neofect.communicator.sample.protocol.SimpleRobotMessageEncoder;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class SimpleRobotCommunicationController extends CommunicationController<SimpleRobot> {

	public SimpleRobotCommunicationController() {
		super(SimpleRobot.class, new SimpleRobotMessageEncoder(), new SimpleRobotMessageDecoder());
	}

}
