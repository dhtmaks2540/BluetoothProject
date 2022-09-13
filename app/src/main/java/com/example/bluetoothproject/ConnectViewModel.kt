package com.example.bluetoothproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamMeasureMode
import com.example.bluetoothproject.bluetooth.model.StreamMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.di.BluetoothMode
import com.example.bluetoothproject.di.BluetoothModeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalUnsignedTypes::class)
@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val bluetoothModeProvider: BluetoothModeProvider
) : ViewModel() {
    private val _deviceName = MutableLiveData(savedStateHandle.get<String>(DEVICE_NAME))
    val deviceName: LiveData<String?> = _deviceName

    private val _deviceAddress = MutableLiveData(savedStateHandle.get<String>(DEVICE_ADDRESS))
    val deviceAddress: LiveData<String?> = _deviceAddress

    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _packetData = MutableLiveData<StreamMode>()

    val packetData: LiveData<StreamMode> = _packetData

    fun setConnected(flag: Boolean) {
        _isConnected.postValue(flag)
    }

    fun setPacketData(data: StreamMode) {
        when(data) {
            is StreamChargeMode -> _packetData.postValue(data)
            is StreamWaitingMode -> _packetData.postValue(data)
            is StreamMeasureMode -> _packetData.postValue(data)
        }
//        when(bluetoothModeProvider.bluetoothMode) {
//            BluetoothMode.WAITING_MODE -> {
//                _packetData.postValue(data as StreamWaitingMode)
//            }
//            BluetoothMode.CHARGE_MODE -> {
//                _packetData.postValue(data as StreamChargeMode)
//            }
//            BluetoothMode.ACTIVATE_MODE -> {
//                _packetData.postValue(data as StreamMeasureMode)
//            }
//            else -> {
//
//            }
//        }
    }
}