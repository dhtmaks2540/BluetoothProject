package com.example.bluetoothproject

import android.content.Context
import android.util.Log
import android.widget.Toast

fun showMessage(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun setLog(tag: String, message: String?) {
    Log.d(tag, message ?: "No Data")
}