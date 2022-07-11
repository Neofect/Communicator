package com.neofect.communicator.usb

import android.util.Log
import com.neofect.communicator.util.ByteArrayConverter
import kotlin.experimental.and

/**
 * Created by jhchoi on 2022/07/13
 * jhchoi@neofect.com
 */
class SmartBalanceSensorDataChecker(val tag: String) {

    private var frameIdx = 0x00.toByte()
    private var beforeXIdx = 0
    private var noProblemCount = 0

    private val summary = Summary()


    fun simpleCheckSensorData(messageByte: ByteArray) {
        if (messageByte[0] != 0xfa.toByte()) {
            Log.d(
                tag, "maybe.. sensor data tail. ${
                    ByteArrayConverter.byteArrayToHex(
                        messageByte
                    )
                }"
            )
            return
        }
        if (messageByte[1] != 0xD3.toByte()) {
            //not sensor data message.
            return
        }

        val frameIdx = messageByte[8]

        val xIdx = (messageByte[17] and 0xff.toByte()).toInt()
        val frameHex = ByteArrayConverter.byteToHex(frameIdx)
        if (frameIdx != 0x00.toByte()) {
            if (this.frameIdx == 0x00.toByte()) {
                this.frameIdx = frameIdx
                noProblemCount = 0
            }
            if (frameIdx == this.frameIdx) {
                if (beforeXIdx != xIdx) {
                    if (beforeXIdx != xIdx - 1) {
                        val lossCount = xIdx - beforeXIdx - 1
                        summary.setLossCount(lossCount, noProblemCount)
                        Log.d(
                            tag,
                            "frameIdx: $frameHex, xIdx: $xIdx. $lossCount lines data loss. noProblemCount: $noProblemCount. lossInfo: $summary"
                        )
                        noProblemCount = 0
                    } else {
                        Log.d(tag, "frameIdx: $frameHex, xIdx: $xIdx. lossInfo: $summary")
                        noProblemCount++
                    }
                }
            } else {
                Log.d(tag, "frameIdx: $frameHex, xIdx: $xIdx. lossInfo: $summary")
                noProblemCount++
            }
        }
        summary.totalSensorDataCount++
        this.beforeXIdx = xIdx
        this.frameIdx = frameIdx
    }

}