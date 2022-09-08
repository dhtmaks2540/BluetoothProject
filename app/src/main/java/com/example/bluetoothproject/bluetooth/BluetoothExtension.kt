package com.example.bluetoothproject.bluetooth

const val BLUETOOTH_CONNECT = 1000
const val BLUETOOTH_SCAN = 1001
const val BLUETOOTH_PERMISSION = 3000

const val CONNECT_FLAG = "connect_flag"
const val MODE_DATA = 100
const val CONNECT_TYPE = 200

enum class BluetoothMode {
    NOT_CONNECTED_MODE, CHARGE_MODE, WAITING_MODE, ACTIVATE_MODE
}

// ByteArray -> 16진수 문자열로
fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

// 해당 위치의 bit값 파악
fun getBit(num: Int, cnt: Int): Int {
    return (num and (1 shl cnt)) shr cnt
}

interface HandlerMessageListener {
    fun getFlagMessage(flag: Boolean)

    fun getModeData(modeData: Any)
}