package com.example.bluetoothproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    private val _device = MutableLiveData<Pair<String, String>>()
    val device: LiveData<Pair<String, String>> = _device

    fun setDeviceList(item: Pair<String, String>) {
        _device.value = item
    }
}