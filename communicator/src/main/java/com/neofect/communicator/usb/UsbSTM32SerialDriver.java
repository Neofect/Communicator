package com.neofect.communicator.usb;

import static android.content.ContentValues.TAG;

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
class UsbSTM32SerialDriver extends UsbSerialDriver {
    UsbSTM32SerialDriver(UsbDevice device, UsbDeviceConnection connection) {
        super(device, connection);
    }

    @Override
    public UsbEndpoint[] open() throws IOException {
        UsbEndpoint[] endpoints = new UsbEndpoint[2];

        boolean opened = false;

        try {
            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface usbIface = device.getInterface(i);
                if (connection.claimInterface(usbIface, true)) {
                    Log.d(TAG, "open() claimInterface(" + i + ") succeeded.");
                } else {
                    Log.d(TAG, "open() claimInterface(" + i + ") failed.");
                }
            }

            UsbInterface dataIface = device.getInterface(device.getInterfaceCount() - 1);
            for (int i = 0; i < dataIface.getEndpointCount(); i++) {
                UsbEndpoint ep = dataIface.getEndpoint(i);
                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                        endpoints[0] = ep;
                    } else {
                        endpoints[1] = ep;
                    }
                }
            }
            opened = true;
        } catch (Exception e) {
            throw new IOException("Failed to open endpoints!", e);
        } finally {
            if (!opened) {
                try {
                    close();
                } catch (IOException e) {
                    // Ignore IOExceptions during close()
                }
            }
        }
        return endpoints;
    }

    @Override
    public void close() throws IOException {
        if (connection == null) {
            throw new IOException("Already closed");
        }
        connection.close();
    }

    @Override
    public void setParameters(int baudRate, int dataBits, int stopBits, int parity) throws IOException {
        // TODO: jhchoi 2022/06/08
//        connection.controlTransfer(
//
//        )
    }


}
