package com.neofect.communicator.usb

import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbId
import com.hoho.android.usbserial.driver.UsbSerialProber

/**
 * Created by jhchoi on 2022/07/11
 * jhchoi@neofect.com
 */
object UsbCustomProber {
    fun getCustomProber(): UsbSerialProber {
        val customTable = ProbeTable()
        customTable.addProduct(UsbId.VENDOR_SILABS, 0xea80, Cp21xxSerialDriver::class.java)
        return UsbSerialProber(customTable)
    }
}