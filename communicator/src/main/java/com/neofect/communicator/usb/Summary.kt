package com.neofect.communicator.usb

/**
 * Created by jhchoi on 2022/07/15
 * jhchoi@neofect.com
 */
class Summary {
    var totalLossCount: Int = 0
    var maxLossCount: Int = 0
    var maxNoProblemCount: Int = 0
    var totalSensorDataCount: Int = 0
    val dataLossRate: Float
        get() = totalLossCount.toFloat() / totalSensorDataCount

    fun setLossCount(lossCount: Int, noProblemCount: Int) {
        totalLossCount += lossCount
        if (maxLossCount < lossCount) {
            maxLossCount = lossCount
        }
        if (maxNoProblemCount < noProblemCount) {
            maxNoProblemCount = noProblemCount
        }
    }

    fun clear() {
        totalLossCount = 0
        maxLossCount = 0
        maxNoProblemCount = 0
        totalSensorDataCount = 0
    }

    override fun toString(): String {
        return "totalLossCount: $totalLossCount, maxLossCount: $maxLossCount, maxNoProblemCount: $maxNoProblemCount, totalSensorDataCount: $totalSensorDataCount, dataLossRate: $dataLossRate"
    }
}