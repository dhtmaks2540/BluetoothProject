package com.example.bluetoothproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.bluetoothproject.bluetooth.BluetoothMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalUnsignedTypes::class)
@HiltViewModel
class ConnectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel()
{
    private val _deviceName = MutableLiveData(savedStateHandle.get<String>(DEVICE_NAME))
    val deviceName: LiveData<String?> = _deviceName

    private val _deviceAddress = MutableLiveData(savedStateHandle.get<String>(DEVICE_ADDRESS))
    val deviceAddress: LiveData<String?> = _deviceAddress

    private val _test = MutableLiveData<UByteArray>()
    val test: LiveData<UByteArray> = _test

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

    fun setTest(byteArray: ByteArray) {
        _test.postValue(byteArray.toUByteArray())
    }
}