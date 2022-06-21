package com.neofect.communicator.usb;

import static android.content.ContentValues.TAG;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.io.IOException;

/**
 * Created by jhchoi on 2022/06/08
 * jhchoi@neofect.com
 */
class UsbSTM32SerialDriver extends CdcAcmSerialDriver {
    UsbSTM32SerialDriver(UsbDevice device, UsbDeviceConnection usbDeviceConnection) {
        super(device, usbDeviceConnection);
    }
}
