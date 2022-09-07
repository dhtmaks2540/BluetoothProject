package com.example.bluetoothproject.bluetooth

const val BLUETOOTH_CONNECT = 1000
const val BLUETOOTH_SCAN = 1001
const val BLUETOOTH_PERMISSION = 3000

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun getBit(num: Int, cnt: Int): Int {
    return (num and (1 shl cnt)) shr cnt
}