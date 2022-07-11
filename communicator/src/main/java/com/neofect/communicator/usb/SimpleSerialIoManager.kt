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
    listener: SerialInputOutputManager.Listener? = null
) : Runnable {
    private var readBuffer // default size = getReadEndpoint().getMaxPacketSize()
            : ByteBuffer

    @get:Synchronized
    var state = SerialInputOutputManager.State.STOPPED // Synchronized by 'this'
        private set

    @get:Synchronized
    @set:Synchronized
    var listener // Synchronized by 'this'
            : SerialInputOutputManager.Listener? = listener

    init {
        Log.d(TAG, "buffer size: ${serialPort.readEndpoint.maxPacketSize}")
        readBuffer = bufferSize?.let {
            ByteBuffer.allocate(it)
        } ?: run {
            ByteBuffer.allocate(serialPort.readEndpoint.maxPacketSize)
        }
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
        val buffer: ByteArray? = readBuffer.array()
        val len = serialPort.read(buffer, 0)
        if (len > 0) {
            val listener = listener
            if (listener != null) {
                val data = ByteArray(len)
                System.arraycopy(buffer, 0, data, 0, len)
                listener.onNewData(data)
            }
        }
    }

    companion object {
        private val TAG = SimpleSerialIoManager::class.java.simpleName
    }
}