package com.example.bluetoothproject.bluetooth.model

import com.example.bluetoothproject.di.BluetoothMode

abstract class StreamMode {
    abstract val nowMode: BluetoothMode
    abstract val packetCount: Int?
}