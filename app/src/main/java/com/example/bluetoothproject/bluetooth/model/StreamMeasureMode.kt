package com.example.bluetoothproject.bluetooth.model

import com.example.bluetoothproject.bluetooth.getBit
import com.example.bluetoothproject.bluetooth.getBitRange
import com.example.bluetoothproject.di.BluetoothMode

@OptIn(ExperimentalUnsignedTypes::class)
data class StreamMeasureMode(
    private val uByteArray: UByteArray
) : StreamMode() {
    override val nowMode: BluetoothMode = BluetoothMode.ACTIVATE_MODE
    override val packetCount: Int = uByteArray[4].toInt()

    val pcd: Int = uByteArray[6].toInt()
    val bpm: Int = uByteArray[5].toInt()
    val ch1: Boolean = when (getBit(uByteArray[7].toInt(), 5)) {
        1 -> true
        else -> false
    }
    val ch2: Boolean = when (getBit(uByteArray[7].toInt(), 4)) {
        1 -> true
        else -> false
    }
    val ref: Boolean = when (getBit(uByteArray[7].toInt(), 3)) {
        1 -> true
        else -> false
    }
    val leftEEG: Int = getBitRange(uByteArray[8].toInt(), 7) * 256 +
            getBit(uByteArray[9].toInt(), 7)
    val rightEEG: Int = getBitRange(uByteArray[10].toInt(), 8) * 256 +
            getBit(uByteArray[11].toInt(), 7)
    val ppg: Int = getBitRange(uByteArray[14].toInt(), 7) * 256 +
            getBit(uByteArray[15].toInt(), 8)
    val sdPPG: Int = getBitRange(uByteArray[16].toInt(), 7) * 256 +
            getBit(uByteArray[17].toInt(), 8)
    val rrInterval: Int = getBitRange(uByteArray[18].toInt(), 7) * 256 +
            getBit(uByteArray[19].toInt(), 8)
}

