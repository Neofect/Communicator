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

/**
 * 
 * @author neo.kim@neofect.com
 * @date Feb 8, 2015
 */
public class ByteRingBuffer {
	
	private static final int	BUFFER_DEFAULT_INIT_CAPACITY	= 128;
	private static final int	BUFFER_DEFAULT_MAX_CAPACITY		= 2048;
	
	private byte[]	buffer;
	private int		maxCapacity	= BUFFER_DEFAULT_MAX_CAPACITY;
	private int		contentSize	= 0;
	private int		headIndex	= 0;
	
	public ByteRingBuffer() {
		buffer = new byte[BUFFER_DEFAULT_INIT_CAPACITY];
	}
	
	public ByteRingBuffer(int capacity) {
		buffer = new byte[capacity];
	}
	
	public ByteRingBuffer(int capacity, int maxCapacity) {
		this.buffer = new byte[capacity];
		this.maxCapacity = maxCapacity;
	}
	
	public int getMaxCapacity() {
		return maxCapacity;
	}

	public void changeMaxCapacity(int maxCapacity) {
		if(this.maxCapacity > maxCapacity)
			throw new IllegalArgumentException("Cannot reduce max capacity!");
		this.maxCapacity = maxCapacity;
	}
	
	/**
	 * Put the byte data into the buffer.
	 * 
	 * @param data
	 */
	public void put(byte[] data) {
		// Check if the internal buffer has enough room for given data.
		if(getAvailableSize() < data.length) {
			if(buffer.length < maxCapacity)
				expandBuffer(contentSize + data.length);
			else
				Log.w("RingBuffer", "Reached max capacity of internal buffer. Oldest data will be overwritten!");
		}
		
		int dataLength = data.length;
		int dataIndex = 0;
		while(dataLength > 0) {
			int tailIndex = headIndex + contentSize;
			int copyLength = 0;
			if(tailIndex + dataLength < buffer.length)
				copyLength = dataLength;
			else if(tailIndex < buffer.length)
				copyLength = buffer.length - tailIndex;
			else
				copyLength = Math.min(dataLength, buffer.length - (tailIndex - buffer.length));
			
			System.arraycopy(data, dataIndex, buffer, tailIndex % buffer.length, copyLength);
			
			contentSize = Math.min(buffer.length, contentSize + copyLength);
			headIndex = ((tailIndex + copyLength) - contentSize) % buffer.length;
			dataLength -= copyLength;
			dataIndex += copyLength;
		}
	}
	
	private void fillByteArrayFromInternalBuffer(byte[] target, int targetIndex, int sourceIndex, int length) {
		if(targetIndex + length > target.length)
			throw new ArrayIndexOutOfBoundsException();
		else if(length > contentSize - sourceIndex)
			throw new ArrayIndexOutOfBoundsException();
		
		int localHeadIndex = (headIndex + sourceIndex) % buffer.length;
		while(length > 0) {
			int copyLength = Math.min(length, buffer.length - localHeadIndex);
			System.arraycopy(buffer, localHeadIndex, target, targetIndex, copyLength);
			localHeadIndex = (localHeadIndex + copyLength) % buffer.length;
			length -= copyLength;
			targetIndex += copyLength;
		}
	}
	
	private void expandBuffer(int requestedSize) {
		byte[] newBuffer = new byte[Math.min(requestedSize,  maxCapacity)];
		fillByteArrayFromInternalBuffer(newBuffer, 0, 0, contentSize);
		Log.i("RingBuffer", "Expanded internal buffer size from " + buffer.length + " to " + newBuffer.length + ".");
		buffer = newBuffer;
		headIndex = 0;
	}
	
	/**
	 * Remove byte data of given size from the buffer.
	 * 
	 * @param size
	 */
	public void consume(int size) {
		if(size > contentSize)
			throw new IllegalArgumentException("Not enough data to consume! remaining=" + contentSize + ", requested=" + size);
		headIndex = (headIndex + size) % buffer.length;
		contentSize -= size;
	}
	
	public byte peek(int index) {
		if(index >= contentSize)
			throw new ArrayIndexOutOfBoundsException(index + " out of " + contentSize);
		return buffer[(headIndex + index) % buffer.length];
	}
	
	/**
	 * Read byte sequence of given length and return it as an array. The returned byte array is newly created.
	 * The byte data which returned is removed from the buffer.
	 * 
	 * @param length
	 * @return
	 */
	public byte[] read(int length) {
		byte[] result = readWithoutConsume(length);
		consume(length);
		return result;
	}
	
	/**
	 * Read byte sequence of given length and return it as an array. The returned byte array is newly created.
	 * The byte data which returned is still existing in the buffer in contrary to {@link #read(int)}.
	 * {@link #consume(int)} needs to be called explicitly. 
	 * 
	 * @param length
	 * @return
	 */
	public byte[] readWithoutConsume(int length) {
		if(length > contentSize)
			throw new ArrayIndexOutOfBoundsException("Possible length=" + contentSize + ", but requested=" + length);
		byte[] result = new byte[length];
		fillByteArrayFromInternalBuffer(result, 0, 0, length);
		return result;
	}
	
	public void clear() {
		headIndex = 0;
		contentSize = 0;
	}
	
	public int getContentSize() {
		return contentSize;
	}
	
	public int getAvailableSize() {
		return buffer.length - contentSize;
	}
	
	// For unit test
	int getHeadIndex() {
		return headIndex;
	}
	
	String toStringOnlyBuffer() {
		return ByteArrayConverter.byteArrayToHex(buffer);
	}
	
	String toStringWithHeadIndicator() {
		String result = ByteArrayConverter.byteArrayToHex(buffer);
		result += "\n";
		for (int i = 0; i < headIndex; ++i)
			result += "   ";
		result += "^\n";
		result += "headIndex=" + headIndex + ", contentSize=" + contentSize + "\n";
		return result;
	}

}

