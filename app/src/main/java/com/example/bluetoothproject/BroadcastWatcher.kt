package com.example.bluetoothproject

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LiveData

class BroadcastWatcher(
    private val context: Context
) : LiveData<Pair<String, String>>() {
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onActive() {
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        broadcastReceiver = createBroadcastReceiver()
        context.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onInactive() {
        context.unregisterReceiver(broadcastReceiver)
    }

    private fun createBroadcastReceiver() = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address
                    if (deviceName != null && deviceHardwareAddress != null) {
                        postValue(Pair(deviceName, deviceHardwareAddress))
                    }
                }
            }
        }
    }
}