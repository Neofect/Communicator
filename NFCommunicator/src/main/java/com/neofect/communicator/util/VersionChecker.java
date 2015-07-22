/*
 * Copyright (c) 2015 Neofect Co., Ltd.
 * 
 * All rights reserved. Used by permission.
 */

package com.neofect.communicator.util;

/**
 * @author neo.kim@neofect.com
 * @date Jul 15, 2015
 */
public class VersionChecker {

	private static final String LOG_TAG = VersionChecker.class.getSimpleName();
	
	public static boolean check(String targetClientVersion, String minVersion, String maxVersion) {
		if(targetClientVersion == null || targetClientVersion.equals("")) {
			return false;
		}
		
		try {
			int maxLength = getMaxLength(new String[] { targetClientVersion, minVersion, maxVersion });
			
			int[] target = padEnoughDigits(targetClientVersion, maxLength);
			int[] min = padEnoughDigits(minVersion, maxLength);
			int[] max = padEnoughDigits(maxVersion, maxLength);
			
			boolean biggerThanMin = false;
			boolean smallerThanMax = false;
			for (int i = 0; i < maxLength && (!biggerThanMin || !smallerThanMax); ++i) {
				if(!biggerThanMin) {
					if(target[i] < min[i]) {
						return false;
					} else if(target[i] > min[i]) {
						biggerThanMin = true;
					}
				}
				
				if(!smallerThanMax) {
					if(target[i] > max[i]) {
						return false;
					} else if(target[i] < max[i]) {
						smallerThanMax = true;
					}
				}
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Exception raised during checking IPC version!", e);
			return false;
		}
		return true;
	}
	
	private static int getMaxLength(String[] versions) {
		int maxLength = 0;
		for(int i = 0; i < versions.length; ++i) {
			int length = getNumberOfDigits(versions[i]);
			if(length > maxLength) {
				maxLength = length;
			}
		}
		return maxLength;
	}
	
	private static int getNumberOfDigits(String version) {
		String[] versionTokens = version.split("\\.");
		return versionTokens.length;
	}
	
	private static int[] padEnoughDigits(String version, int targetNumberOfDigits) {
		int[] result = new int[targetNumberOfDigits];
		
		String[] versionTokens = version.split("\\.");
		for(int i = 0; i < versionTokens.length; ++i) {
			result[i] = Integer.parseInt(versionTokens[i]);
		}
		return result;
	}
	
	public static void main(String[] args) {
		String[][] dataSet = {
				{ "1.0", "1.0", "1.0", "true"},
				{ "1.0.0.0.0.0.0.1", "1.0", "3.0.5", "true"},
				{ "1.1", "1.00000.00.11.3330.13131", "1.1", "true"},
				{ "1.0.0.0.0.0.0.1", "1.0", "1.0.5", "true"},
				{ "1.0.3", "1.3", "1.4", "false"},
				{ "1.0.4", "1.0", "1.0.5", "true" },
		};
		
		for(int i = 0; i < dataSet.length; ++i) {
			String[] data = dataSet[i];
			boolean result = VersionChecker.check(data[0], data[1], data[2]);
			Log.d(LOG_TAG, data[0] + ", " + data[1] + ", " + data[2] + ", result=" + result);
			if(result != Boolean.parseBoolean(data[3])) {
				Log.e(LOG_TAG, "Failed!");
			}
		}
	}
	
}
