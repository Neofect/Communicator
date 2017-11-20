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
package com.neofect.communicator.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 
 * @author ehlee@neofect.com
 */
public class ByteArrayConverter {

	private static final String LOG_TAG = "ByteArrayConverter";

	/**
	 * Convert a hex string to a byte array.
	 * @param hex A hex string like 'fa3db9'.
	 */
	public static byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}

		byte[] ba = new byte[hex.length() / 2];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return ba;
	}

	/**
	 * Convert a hex string containing spaces to a byte array.
	 * @param hex A hex string like 'fa 3d b9'.
	 */
	public static byte[] hexToByteArrayWithSpace(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}

		String[] eachByte = hex.split(" ");
		byte[] ba = new byte[eachByte.length];
		for (int i = 0; i < eachByte.length; i++) {
			ba[i] = (byte) Integer.parseInt(eachByte[i], 16);
		}
		return ba;
	}

	/**
	 * Convert a byte value to a hex string.
	 */
	public static String byteToHex(byte byteValue) {
		String result = Integer.toHexString(0xff & byteValue);
		if (result.length() == 1) {
			result = "0" + result;
		}
		return result;
	}

	/**
	 * Convert a byte array to a hex string containing spaces.
	 * @return A hex string like 'fa 3d b9'.
	 */
	public static String byteArrayToHex(byte[] ba) {
		return byteArrayToHex(ba, 0, ba.length);
	}
	
	/**
	 * Convert a byte array to a hex string without spaces.
	 * @return A hex string like 'fa3db9'.
	 */
	public static String byteArrayToHexWithoutSpace(byte[] ba) {
		return byteArrayToHex(ba, 0, ba.length, false);
	}

	/**
	 * Convert a byte array to a hex string without spaces.
	 */
	public static String byteArrayToHex(byte[] ba, int startIndex, int endIndex) {
		return byteArrayToHex(ba, startIndex, endIndex, true);
	}
	
	/**
	 * Convert a byte array to a hex string.
	 */
	public static String byteArrayToHex(byte[] ba, int startIndex, int endIndex, boolean includeSpace) {
		if (ba == null || ba.length == 0 || startIndex == endIndex)
			return "";
		else if (startIndex > endIndex) {
			Log.e(LOG_TAG, "byteArrayToHex() The input byte array is not valid!");
			return "";
		}

		endIndex = (ba.length < endIndex) ? ba.length : endIndex;

		StringBuffer sb = new StringBuffer(endIndex * 2);
		for (int x = startIndex; x < endIndex; x++) {
			if (includeSpace && x != startIndex)
				sb.append(" ");
			sb.append(byteToHex(ba[x]));
		}
		return sb.toString();
	}
	
	/**
	 * Convert an integer value to a byte array.
	 */
	public static byte[] intToByteArray(int integer, ByteOrder byteOrder) {
		return ByteBuffer.allocate(Integer.SIZE / 8).order(byteOrder).putInt(integer).array();
	}

	/**
	 * Convert a byte array to an integer value.
	 */
	public static int byteArrayToInt(byte[] bytes, int startIndex, int length) {
		return byteArrayToInt(bytes, startIndex, length, ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Convert a byte array to an integer value.
	 */
	public static int byteArrayToInt(byte[] bytes, int startIndex, int length, ByteOrder byteOrder) {
    	if (bytes == null)
    		return 0;
    	
        final int newLength = Integer.SIZE / 8;
        final byte[] newBytes = new byte[newLength];
        
        for (int i = 0; i < newLength; i++) {
        	if (byteOrder == ByteOrder.BIG_ENDIAN) {
        		if (i < newLength - length)
        			newBytes[i] = (byte) 0x00;
        		else
        			newBytes[i] = bytes[startIndex + i - (newLength - length)];
        	} else {
        		if (i >= length)
        			newBytes[i] = (byte) 0x00;
        		else
        			newBytes[i] = bytes[startIndex + i];
        	}
        }
        	
        ByteBuffer buff = ByteBuffer.wrap(newBytes);
        buff.order(byteOrder);
        return buff.getInt();
    }
	
    /**
     * @author "wjchoi@neofect.com"
     */
	public static String byteArrayToHex(byte[] array, int numForRow) {
		return byteArrayToHex(array, numForRow, "%#-2x ");
	}
	
	/**
     * @author "wjchoi@neofect.com"
	 */
	public static String byteArrayToHex(byte[] array, int numForRow, String format) {
		if (array == null) {
			return null;
		}
		
		int byteLen = array.length;

		StringBuilder byteArrayElements = new StringBuilder();
		
		// start with new line
		byteArrayElements.append("\n");
		for(int loop=0; loop<byteLen; loop++) {
			byte b = array[loop];
			byteArrayElements.append(String.format(format, b));

			// intent to feed a line per a numForRow
			if ( ((loop+1)% numForRow ==0) && ((loop+1)/numForRow >=1) ) {
				byteArrayElements.append("\n");
			}
		}
		return byteArrayElements.toString();
	}
	
}
