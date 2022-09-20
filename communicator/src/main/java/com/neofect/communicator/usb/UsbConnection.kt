package com.neofect.communicator.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.neofect.communicator.Connection
import com.neofect.communicator.ConnectionType
import com.neofect.communicator.Controller
import com.neofect.communicator.Device
import com.neofect.communicator.util.ByteRingBuffer
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Created by jhchoi on 2022/07/08
 * jhchoi@neofect.com
 */
class UsbConnection(
    private val context: Context,
    private val device: UsbDevice,
    controller: Controller<out Device>?
) : Connection(ConnectionType.USB_SERIAL, controller) {
    private var usbSerialPort: UsbSerialPort? = null
    private var usbEventReceiver: BroadcastReceiver? = null
    private var usbIoManager: SimpleSerialIoManager? = null

    private var readDataHandlerThread: Thread? = null

    private val readDataCache = ByteRingBuffer(
        2 * 1024 * 1024 // Maximum 2MB
    )
    private val cacheLock = Object()

    override fun connect() {
        registerReceiver()

        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        //permission check.
        if (!usbManager.hasPermission(device)) {
            // Ask permission for USB connection
            val intent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
            Log.d(LOG_TAG, "Requesting permission for USB device '${device.deviceName}'...")
            usbManager.requestPermission(device, intent)
            return
        }
        startConnecting()
    }

    override fun disconnect() {
        if (usbSerialPort == null) {
            Log.e(LOG_TAG, "disconnect: Already disconnected")
            return
        }
        cleanUp()
        handleDisconnected()
    }

    private fun cleanUp() {

        readDataHandlerThread?.interrupt()
        readDataHandlerThread = null

        usbIoManager?.let {
            it.listener = null
            it.stop()
        }
        usbIoManager = null

        if (usbEventReceiver != null) {
            synchronized(this) {
                try {
                    context.unregisterReceiver(usbEventReceiver)
                    Log.d(LOG_TAG, "cleanUp: Receiver unregistered.")
                } catch (e: IllegalArgumentException) {
                    Log.e(LOG_TAG, "cleanUp: Failed to unregister the receiver!", e)
                }
                usbEventReceiver = null
            }
        }

        runCatching {
            usbSerialPort?.close()
        }
        usbSerialPort = null
    }

    private fun startConnecting() {
        handleConnecting()
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbConnection = usbManager.openDevice(device)
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: run {
            UsbCustomProber.getCustomProber().probeDevice(device)
        } ?: run {
            throw IllegalStateException("Unknown usb device. driver not found.")
        }

        val usbSerialPort = driver.ports.first()
        runCatching {
            usbSerialPort.open(usbConnection)
            when (usbSerialPort) {
                is CdcAcmSerialDriver.CdcAcmSerialPort -> {
                    usbSerialPort.setParameters(
                        115200,
                        8,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )
                }
                else -> {
                    usbSerialPort.setParameters(
                        115200,
                        8,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )
                }
            }

            readDataHandlerThread?.interrupt()

            usbIoManager =
                SimpleSerialIoManager(usbSerialPort, READ_BUFFER_SIZE, inputOutputManagerListener)
            usbIoManager?.start()
            this@UsbConnection.usbSerialPort = usbSerialPort

            handleConnected()

            readDataHandlerThread = ReadDataHandlerThread()
            readDataHandlerThread?.start()


        }.onFailure {
            cleanUp()
            val exception = if (it is Exception) it else IOException("connect failed.")
            handleFailedToConnect(exception)
        }
    }

    @Synchronized
    private fun registerReceiver() {
        if (usbEventReceiver != null) {
            Log.e(LOG_TAG, "USB event receiver is already registered!")
            return
        }
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        usbEventReceiver = createReceiver()
        context.registerReceiver(usbEventReceiver, filter)
        Log.d(LOG_TAG, "USB event receiver is registered.")
    }

    private fun createReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)?.takeIf {
                    this@UsbConnection.device == device
                } ?: return
                val action = intent.action
                Log.d(
                    LOG_TAG,
                    "USB event is received. action=$action, device=${this@UsbConnection.description}"
                )
                when (action) {
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        Log.i(LOG_TAG, "USB device is detached. device=$device")
                        disconnect()
                    }
                    ACTION_USB_PERMISSION -> {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            Log.i(LOG_TAG, "Permission granted for the device $device")
                            startConnecting()
                        } else {
                            Log.i(LOG_TAG, "Permission denied for the device $device")
                            cleanUp()
                            handleFailedToConnect(SecurityException("User denied to grant USB permission!"))
                        }
                    }
                }
            }
        }
    }

    override fun write(data: ByteArray?) {
        if (!isConnected) {
            Log.e(LOG_TAG, "write: Not connected!")
            return
        }
        runCatching {
            usbSerialPort?.write(data, WRITE_TIMEOUT_MILLIS)
        }.onFailure { e ->
            Log.e(LOG_TAG, "write()", e)
        }
    }

    override fun getDeviceIdentifier(): String = device.deviceName

    override fun getDeviceName(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            device.productName
        } else {
            device.deviceName
        }

    override fun getDescription(): String = "$deviceName($deviceIdentifier)"

    private inner class ReadDataHandlerThread : Thread() {
        override fun run() {
            Log.d(LOG_TAG, "ReadDataHandlerThread start.")
            runCatching {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
                var bufferCache = ByteBuffer.allocate(2 * 1024 * 1024).array()
                while (isConnected) {
                    val contentSize = synchronized(cacheLock) {
                        val contentSize = readDataCache.contentSize
                        if (contentSize > bufferCache.size) {
                            Log.d(LOG_TAG, "handle thread buffer alloc. size: $contentSize")
                            bufferCache = ByteBuffer.allocate(contentSize).array()
                        }
                        readDataCache.read(bufferCache, contentSize)
                        contentSize
                    }
                    if (bufferCache.isNotEmpty()) {
                        handleReadData(bufferCache, contentSize)
                    }
                    sleep(1)
                }
            }.onFailure {
                it.printStackTrace()
            }
            Log.d(LOG_TAG, "ReadDataHandlerThread finish.")
        }
    }

    private val inputOutputManagerListener = object : SimpleSerialIoManager.Listener {
        override fun onNewData(data: ByteArray, length: Int) {
            if (data != null) {
                synchronized(cacheLock) {
                    readDataCache.put(data, length)
                }
            }
        }

        override fun onRunError(e: Exception?) {
            Log.e(LOG_TAG, "onRunError. ", e)
            disconnect()
        }

    }

    companion object {
        private val LOG_TAG = UsbConnection::class.java.simpleName
        private val ACTION_USB_PERMISSION = "com.neofect.communicator.USB_PERMISSION"
        private const val WRITE_TIMEOUT_MILLIS = 200
        private const val READ_BUFFER_SIZE = 100
    }
}