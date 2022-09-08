package com.example.bluetoothproject

import android.app.Application
import com.example.bluetoothproject.bluetooth.BluetoothMode
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication @Inject constructor() : Application() {
    @Inject
    lateinit var bluetoothMode: BluetoothMode
}