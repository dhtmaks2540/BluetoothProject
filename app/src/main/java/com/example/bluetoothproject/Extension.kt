package com.example.bluetoothproject

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import timber.log.Timber

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun setLog(tag: String, message: String?) {
    Log.d(tag, message ?: "No Data")
}

fun useTimber(message: String) {
    Timber.d(message)
}

fun Context.hasPermission(permissionTypes: Array<String>): Boolean {
    return permissionTypes.all { permissionType ->
        ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

const val DEVICE_ADDRESS = "deviceAddress"
const val DEVICE_NAME = "deviceName"
const val BLUETOOTH_CONNECT = 1
const val BLUETOOTH_SCAN = 2
const val BLUETOOTH_PERMISSION = 100