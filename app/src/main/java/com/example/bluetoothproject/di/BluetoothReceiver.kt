package com.example.bluetoothproject.di

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import javax.inject.Inject

class BluetoothReceiver @Inject constructor(): BroadcastReceiver() {
    val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("BLUETOOTH_RECEIVER", "ㅠㅠ")
        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name
                val deviceHardwareAddress = device?.address
            }
        }
    }
}