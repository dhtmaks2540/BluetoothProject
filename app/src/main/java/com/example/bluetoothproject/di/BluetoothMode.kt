package com.example.bluetoothproject.di

import javax.inject.Inject

class BluetoothModeProvider @Inject constructor() {
    var bluetoothMode = BluetoothMode.NOT_CONNECTED_MODE
}

enum class BluetoothMode {
    NOT_CONNECTED_MODE, CHARGE_MODE, WAITING_MODE, ACTIVATE_MODE
}