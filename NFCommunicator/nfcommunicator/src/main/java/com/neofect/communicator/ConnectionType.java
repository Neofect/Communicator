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
package com.neofect.communicator;

/**
 * @author neo.kim@neofect.com
 * @date Feb 9, 2015
 */
public enum ConnectionType {
	BLUETOOTH_SPP,
	BLUETOOTH_SPP_INSECURE,
	BLUETOOTH_A2DP,
	BLUETOOTH_LOW_ENERGY,	// Not used for now

	USB_SERIAL,

	DUMMY,
}
