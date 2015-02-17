/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample.device;

import com.neofect.communicator.CommunicationController;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class SimpleRobotCommunicationController extends CommunicationController<SimpleRobot> {

	public SimpleRobotCommunicationController() {
		super(SimpleRobot.class, new SimpleRobotMessageEncoder(), new SimpleRobotMessageDecoder());
	}

}
