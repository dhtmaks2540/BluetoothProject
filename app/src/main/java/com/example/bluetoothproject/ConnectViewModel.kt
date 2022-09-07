package com.example.bluetoothproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectViewModel: ViewModel() {
    private val _dataList = MutableLiveData<List<Int>>()
    val dataList: LiveData<List<Int>> = _dataList

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    fun setDataString(data: String) {
        val list = data.split(" ").map { it.toInt() }
        _dataList.postValue(list)
    }

    fun setConnected(flag: Boolean) {
        _isConnected.postValue(flag)
    }
}