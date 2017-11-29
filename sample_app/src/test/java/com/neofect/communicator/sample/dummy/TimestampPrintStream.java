package com.neofect.communicator.sample.dummy;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author neo.kim@neofect.com
 * @date Nov 16, 2017
 */
public class TimestampPrintStream extends PrintStream {

	private final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

	public TimestampPrintStream() {
		this(System.out);
	}

	public TimestampPrintStream(PrintStream ps) {
		super(ps);
	}

	public void println(String text) {
		super.println(sdf.format(new Date()) + " " + text);
	}

}
