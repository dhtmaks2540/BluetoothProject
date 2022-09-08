package com.example.bluetoothproject.bluetooth.model

import com.example.bluetoothproject.bluetooth.getBit

@OptIn(ExperimentalUnsignedTypes::class)
data class StreamWaitingMode(
    private val uByteArray: UByteArray
) {
    val remainTime: Int = uByteArray[3].toInt()
    val packetCount: Int = uByteArray[4].toInt()
    val batteryValue: Int = uByteArray[5].toInt()
    val leftForehead: Int = getBit(uByteArray[7].toInt(), 5)
    val rightForehead: Int=  getBit(uByteArray[7].toInt(), 4)
    val earlobe: Int = getBit(uByteArray[7].toInt(), 3)
}
