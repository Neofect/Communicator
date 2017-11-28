/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.sample;

/**
 * @author neo.kim@neofect.com
 * @date Feb 18, 2015
 */
public class Controller extends com.neofect.communicator.Controller<SimpleRemote> {

	public Controller() {
		super(new Encoder(), new Decoder());
	}

}
