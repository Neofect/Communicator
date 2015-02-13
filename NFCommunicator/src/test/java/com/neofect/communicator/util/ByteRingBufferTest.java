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

import junit.framework.TestCase;

import com.neofect.communicator.util.ByteArrayConverter;
import com.neofect.communicator.util.ByteRingBuffer;
import com.neofect.communicator.util.Log;

public class ByteRingBufferTest extends TestCase {
	
	private int sequenceNumber = 0;
	
	@Override
	protected void setUp() {
		Log.setMessageOnlyWhenStandardOutput(true);
		sequenceNumber = 0;
	}
	
	private byte[] createByteArray(int numberOfElement) {
		byte[] result = new byte[numberOfElement];
		for(int i = 0; i < numberOfElement; ++i)
			result[i] = (byte) (sequenceNumber++);
		return result;
	}
	
	public void testPut() {
		ByteRingBuffer buffer = new ByteRingBuffer(10, 15);
		buffer.put(createByteArray(17));
		String result = buffer.toStringOnlyBuffer();
		assertEquals("Buffer content", result, "0f 10 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e");
		assertEquals("Head index", buffer.getHeadIndex(), 2);
		assertEquals("Content size", buffer.getContentSize(), 15);
	}
	
	public void testExpansion() {
		ByteRingBuffer buffer = new ByteRingBuffer(10, 15);
		buffer.put(createByteArray(19));
		
		buffer.changeMaxCapacity(20);
		buffer.put(createByteArray(8));
		String result = buffer.toStringOnlyBuffer();
		assertEquals("Buffer content", result, "18 19 1a 07 08 09 0a 0b 0c 0d 0e 0f 10 11 12 13 14 15 16 17");
		assertEquals("Head index", buffer.getHeadIndex(), 3);
		assertEquals("Content size", buffer.getContentSize(), 20);
	}
	
	public void testRead() {
		int maxCapacity = 15;
		int numberToRead = 11;
		
		ByteRingBuffer buffer = new ByteRingBuffer(10, maxCapacity);
		buffer.put(createByteArray(19));
		
		int previousHeadIndex = buffer.getHeadIndex();
		byte[] read = buffer.read(numberToRead);
		assertEquals("Read message", ByteArrayConverter.byteArrayToHex(read), "04 05 06 07 08 09 0a 0b 0c 0d 0e");
		
		String result = buffer.toStringOnlyBuffer();
		assertEquals("Buffer content after read", result, "0f 10 11 12 04 05 06 07 08 09 0a 0b 0c 0d 0e");
		assertEquals("Head index", buffer.getHeadIndex(), (previousHeadIndex + numberToRead) % maxCapacity);
		assertEquals("Content size", buffer.getContentSize(), maxCapacity - numberToRead);
	}
	
}
