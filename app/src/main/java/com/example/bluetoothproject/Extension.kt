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

fun useTimber(message: String) {
    Timber.d(message)
}

fun Context.hasPermission(permissionTypes: Array<String>): Boolean {
    return permissionTypes.all { permissionType ->
        ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }
}

const val DEVICE_ADDRESS = "deviceAddress"
const val DEVICE_NAME = "deviceName"