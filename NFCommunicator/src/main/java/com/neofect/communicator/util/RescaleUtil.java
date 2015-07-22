/*
 * Copyright (c) 2014 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.util;

/**
 * @author neo.kim@neofect.com
 * @date Feb 6, 2014
 */
public class RescaleUtil {
	
	public static float rescale(float originalMin, float originalMax, float originalValue, float scaledMin, float scaledMax, boolean allowOutOfRange) {
		float originalRatio = (originalValue - originalMin) / (originalMax - originalMin);
		float scaledRatio = originalRatio * (scaledMax - scaledMin) + scaledMin;
		if(!allowOutOfRange) {
			scaledRatio = Math.min(scaledRatio, scaledMax);
			scaledRatio = Math.max(scaledRatio, scaledMin);
		}
		return scaledRatio;
	}
	
	public static float rescale(float originalMin, float originalMax, float originalValue, float scaledMin, float scaledMax) {
		return rescale(originalMin, originalMax, originalValue, scaledMin, scaledMax, false);
	}
	
}
