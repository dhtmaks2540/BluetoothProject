package com.example.bluetoothproject

import android.content.Context
import android.util.Log
import android.widget.Toast

const val BLUETOOTH_CONNECT = 1
const val BLUETOOTH_SCAN = 2

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun setLog(tag: String, message: String) {
    Log.d(tag, message)
}