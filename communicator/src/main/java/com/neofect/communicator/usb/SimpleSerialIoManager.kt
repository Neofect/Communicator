package com.neofect.communicator.usb

import android.os.Process
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.neofect.communicator.usb.SimpleSerialIoManager
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by jhchoi on 2022/07/11
 * jhchoi@neofect.com
 */
internal class SimpleSerialIoManager(
    private val serialPort: UsbSerialPort,
    bufferSize: Int? = null,
    listener: Listener? = null
) : Runnable {
    private var readBufferArray: ByteArray
    private var readBufferLength: Int = 0

    @get:Synchronized
    var state = SerialInputOutputManager.State.STOPPED // Synchronized by 'this'
        private set

    @get:Synchronized
    @set:Synchronized
    var listener: Listener? = listener  // Synchronized by 'this'

    init {
        Log.d(TAG, "buffer size: ${serialPort.readEndpoint.maxPacketSize}")
        val readBuffer = bufferSize?.let {
            ByteBuffer.allocate(it)
        } ?: run {
            ByteBuffer.allocate(serialPort.readEndpoint.maxPacketSize)
        }
        readBufferArray = readBuffer.array()
    }

    /**
     * start SerialInputOutputManager in separate thread
     */
    fun start() {
        check(state == SerialInputOutputManager.State.STOPPED) { "already started" }
        Thread(this, this.javaClass.simpleName).start()
    }

    /**
     * stop SerialInputOutputManager thread
     *
     *
     * when using readTimeout == 0 (default), additionally use usbSerialPort.close() to
     * interrupt blocking read
     */
    @Synchronized
    fun stop() {
        if (state == SerialInputOutputManager.State.RUNNING) {
            Log.i(TAG, "Stop requested")
            state = SerialInputOutputManager.State.STOPPING
        }
    }

    /**
     * Continuously services the read and write buffers until [.stop] is
     * called, or until a driver exception is raised.
     */
    override fun run() {
        synchronized(this) {
            check(state == SerialInputOutputManager.State.STOPPED) { "Already running" }
            state = SerialInputOutputManager.State.RUNNING
        }
        Log.i(TAG, "Running ...")
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            while (true) {
                if (state != SerialInputOutputManager.State.RUNNING) {
                    Log.i(TAG, "Stopping mState=$state")
                    break
                }
                readStep()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Run ending due to exception: " + e.message, e)
            val listener = listener
            listener?.onRunError(e)
        } finally {
            synchronized(this) {
                state = SerialInputOutputManager.State.STOPPED
                Log.i(TAG, "Stopped")
            }
        }
    }

    @Throws(IOException::class)
    private fun readStep() {
        // Handle incoming data.
        readBufferLength = serialPort.read(readBufferArray, 0)
        if (readBufferLength > 0) {
            this.listener?.onNewData(readBufferArray, readBufferLength)
        }
    }

    companion object {
        private val TAG = SimpleSerialIoManager::class.java.simpleName
    }

    interface Listener {
        fun onNewData(data: ByteArray, length: Int)

        fun onRunError(e: java.lang.Exception?)
    }
}