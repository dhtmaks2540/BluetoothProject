package com.example.bluetoothproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

@OptIn(ExperimentalUnsignedTypes::class)
class ConnectViewModel: ViewModel() {
    private val _dataArray = MutableLiveData<UByteArray>()
    val dataArray: LiveData<UByteArray> = _dataArray

    val mode: LiveData<Int?> by lazy {
        MutableLiveData(_dataArray.value?.get(2)?.toInt())
    }

    fun setDataArray(uByteArray: UByteArray) {
        _dataArray.postValue(uByteArray)
    }

    fun getDataArray() = _dataArray
}