package com.example.bleapp

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun setLog(tag: String, message: String?) {
    Log.d(tag, message ?: "No Data")
}

fun Context.hasPermission(permissionTypes: Array<String>): Boolean {
    return permissionTypes.all { permissionType ->
        ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }
}