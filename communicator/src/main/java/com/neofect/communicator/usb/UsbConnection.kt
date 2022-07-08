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
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.neofect.communicator.Connection
import com.neofect.communicator.ConnectionType
import com.neofect.communicator.Controller
import com.neofect.communicator.Device
import com.neofect.communicator.util.ByteArrayConverter
import java.io.IOException

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
    private var usbIoManager: SerialInputOutputManager? = null

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
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val usbConnection = usbManager.openDevice(device)
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: run {
            throw IllegalStateException("Unknown usb device. driver not found.")
        }
        val usbSerialPort = driver.ports.first()
        runCatching {
            usbSerialPort.open(usbConnection)
            when (usbSerialPort) {
                is CdcAcmSerialDriver.CdcAcmSerialPort -> {
                    usbSerialPort.setParameters(
                        921600,
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
            usbIoManager = SerialInputOutputManager(usbSerialPort, inputOutputManagerListener)
            usbIoManager?.start()
            this@UsbConnection.usbSerialPort = usbSerialPort

            handleConnected()
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
            val hex = ByteArrayConverter.byteArrayToHex(data)
            Log.d(LOG_TAG, "writeData: $hex")
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

    private val inputOutputManagerListener = object : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray?) {
            val hex = ByteArrayConverter.byteArrayToHex(data)
            Log.d(LOG_TAG, "readData: $hex")
            handleReadData(data)
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
    }

}