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

/**
 * In certain environment like JUnit test (not Android JUnit), Android runtime
 * may not loaded. This class is a wrapper for Android built-in log. It checks
 * if {@link android.util.Log} of Android runtime is loaded, and confirms not to
 * use it but standard output instead.
 * 
 * @author neo.kim@neofect.com
 * @date Jan 24, 2014
 */
public class Log {
	
	private static boolean	USE_STANDARD_OUTPUT;
	
	public static final int	VERBOSE	= 0;
	public static final int	DEBUG	= 1;
	public static final int	INFO	= 2;
	public static final int	WARN	= 3;
	public static final int	ERROR	= 4;

	private static boolean	messageOnlyWhenStandardOutput = false;
	private static int		logLevel = VERBOSE; 
	
	static {
		// Check if Android built-in log exists.
		try {
			Log.class.getClassLoader().loadClass("android.util.Log");
			USE_STANDARD_OUTPUT = false;
		} catch (ClassNotFoundException e) {
			USE_STANDARD_OUTPUT = true;
		}
	}
	
	public static void setMessageOnlyWhenStandardOutput(boolean messageOnlyWhenStandardOutput) {
		Log.messageOnlyWhenStandardOutput = messageOnlyWhenStandardOutput;
	}
	
	public static void setLogLevel(int logLevel) {
		Log.logLevel = logLevel;
	}
	
	public static void v(String tag, String message) {
		v(tag, message, null);
	}
	
	public static void v(String tag, String message, Throwable cause) {
		if(logLevel > VERBOSE)
			return;
		if(!USE_STANDARD_OUTPUT)
			android.util.Log.v(tag, message, cause);
		else
			printStandardOutput("V", tag, message, cause);
	}
	
	public static void d(String tag, String message) {
		d(tag, message, null);
	}
	
	public static void d(String tag, String message, Throwable cause) {
		if(logLevel > DEBUG)
			return;
		if(!USE_STANDARD_OUTPUT)
			android.util.Log.d(tag, message, cause);
		else
			printStandardOutput("D", tag, message, cause);
	}
	
	public static void i(String tag, String message) {
		i(tag, message, null);
	}
	
	public static void i(String tag, String message, Throwable cause) {
		if(logLevel > INFO)
			return;
		if(!USE_STANDARD_OUTPUT)
			android.util.Log.i(tag, message, cause);
		else
			printStandardOutput("I", tag, message, cause);
	}
	
	public static void w(String tag, String message) {
		w(tag, message, null);
	}
	
	public static void w(String tag, String message, Throwable cause) {
		if(logLevel > WARN)
			return;
		if(!USE_STANDARD_OUTPUT)
			android.util.Log.w(tag, message, cause);
		else
			printStandardOutput("W", tag, message, cause);
	}
	
	public static void e(String tag, String message) {
		e(tag, message, null);
	}
	
	public static void e(String tag, String message, Throwable cause) {
		if(logLevel > ERROR)
			return;
		if(!USE_STANDARD_OUTPUT)
			android.util.Log.e(tag, message, cause);
		else {
			printStandardOutput("E", tag, message, cause);
			if(cause != null)
				cause.printStackTrace(System.err);
		}
	}
	
	private static void printStandardOutput(String logLevel, String tag, String message, Throwable cause) {
		if(!messageOnlyWhenStandardOutput)
			System.out.println(logLevel + " | " + tag + " | " + message + (cause != null ? (" | " + cause) : ""));
		else
			System.out.println(message + (cause != null ? (" | " + cause) : ""));
	}
	
}
