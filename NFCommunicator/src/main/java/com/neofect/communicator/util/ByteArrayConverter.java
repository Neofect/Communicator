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

package com.neofect.communicator.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 
 * @author ehlee@neofect.com
 */
public class ByteArrayConverter {

	private static final String	LOG_TAG	= ByteArrayConverter.class.getSimpleName();

	/**
	 * 'fa3db9' 형태의 hex string을 byte[]로 반환한다.
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
	 * 'fa 3d b9' 형태의 hex string을 byte[]로 반환한다.
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
	 * byte array를 'fa 3d b9' 형태의 hex string으로 변환한다.
	 * 
	 * @param byte[]
	 * @return hex string
	 */
	public static String byteToHex(byte b) {
		String result = Integer.toHexString(0xff & b);
		if (result.length() == 1)
			result = "0" + result;
		return result;
	}

	/**
	 * byte array를 'fa 3d b9' 형태의 hex string으로 변환한다.
	 * 
	 * @param byte[]
	 * @return hex string
	 */
	public static String byteArrayToHex(byte[] ba) {
		return byteArrayToHex(ba, 0, ba.length);
	}
	
	/**
	 * byte array를 'fa3db9' 형태의 hex string으로 변환한다.
	 * 
	 * @param byte[]
	 * @return hex string
	 */
	public static String byteArrayToHexWithoutSpace(byte[] ba) {
		return byteArrayToHex(ba, 0, ba.length, false);
	}

	/**
	 * byte array를 'fa 3d b9' 형태의 hex string으로 변환한다.
	 * 
	 * @param byte[]
	 * @param startIndex	시작 인덱스
	 * @param endIndex		종료 인덱스
	 * @return hex string
	 */
	public static String byteArrayToHex(byte[] ba, int startIndex, int endIndex) {
		return byteArrayToHex(ba, startIndex, endIndex, true);
	}
	
	/**
	 * byte array를 'fa 3d b9' 형태의 hex string으로 변환한다.
	 * 
	 * @param byte[]
	 * @param startIndex	시작 인덱스
	 * @param endIndex		종료 인덱스
	 * @return hex string
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

	public static byte[] intToByteArray(int integer, ByteOrder byteOrder) {
		return ByteBuffer.allocate(Integer.SIZE / 8).order(byteOrder).putInt(integer).array();
	}

	/**
	 * Convert the byte array to an integer value.
	 */
	public static int byteArrayToInt(byte[] bytes, int startIndex, int length) {
		return byteArrayToInt(bytes, startIndex, length, ByteOrder.BIG_ENDIAN);
	}

	/**
	 * Convert the byte array to an integer value.
	 */
	public static int byteArrayToInt(byte[] bytes, int startIndex, int length, ByteOrder byteOrder) {
    	if(bytes == null)
    		return 0;
    	
        final int newLength = Integer.SIZE / 8;
        final byte[] newBytes = new byte[newLength];
        
        for (int i = 0; i < newLength; i++) {
        	if(byteOrder == ByteOrder.BIG_ENDIAN) {
        		if(i < newLength - length)
        			newBytes[i] = (byte) 0x00;
        		else
        			newBytes[i] = bytes[startIndex + i - (newLength - length)];
        	} else {
        		if(i >= length)
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
	 * byte[]를 numForRow 길이의 row 로 정열하여 HEX 문자열로 변환해 반환한다. 
     * default format "%#-2x "
     * @param array
     * @param numForRow
     * @return
     * String
     * @author "wjchoi@neofect.com"
     */
	public static String byteArrayToHex(byte[] array, int numForRow) {
		return byteArrayToHex(array, numForRow, "%#-2x ");
	}
	
	/**
	 * 출력 문자열에 format 을 적용할 수 있다. 
	 * @param array
	 * @param numForRow
	 * @param format example) "%#-2x ", "%c "
	 * @return
	 * String
     * @author "wjchoi@neofect.com"
	 */
	public static String byteArrayToHex(byte[] array, int numForRow, String format) {
		if(array == null)
			return null;
		
		int byteLen = array.length;

		StringBuilder byteArrayElements = new StringBuilder();
		
		// start with new line
		byteArrayElements.append("\n");
		for(int loop=0; loop<byteLen; loop++) {
			byte b = array[loop];
			byteArrayElements.append(String.format(format, b));

			// intent to feed a line per a numForRow
			if( ((loop+1)% numForRow ==0) && ((loop+1)/numForRow >=1) )
				byteArrayElements.append("\n");
		}
		return byteArrayElements.toString();
	}
	
}
