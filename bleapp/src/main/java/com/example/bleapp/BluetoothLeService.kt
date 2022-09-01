package com.example.bleapp

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BluetoothLeService : Service() {
    companion object {
        private const val TAG = "BLUETOOTH_SERVICE"
        // 연결
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        // 연결 해제
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
        const val UUID = "00002a37-0000-1000-8000-00805f9b34fb"
    }

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
    }

    // BluetoothGattCallback
    private val gattCallback = object : BluetoothGattCallback() {
        // GATT 서버에 대한 연결이 변경될 때 트리거
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            when (newState) {
                // GATT Server에 연결이 되었을 때
                BluetoothProfile.STATE_CONNECTED -> {
                    val intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    // 연결에 성공한 후 services 검색
                    bluetoothGatt?.discoverServices()
                    setLog(TAG, "STATE_CONNECTED")
                }
                // Gatt Server에 연결이 끊겼을 때
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val intentAction = ACTION_GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                }
            }
        }

        // 서비스 발견
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                    setLog(TAG, "ACTION_GATT_SERVICES_DISCOVERED")
                }
                else -> {
                    setLog(TAG, "onServicesDiscovered : $status")
                }
            }
        }

        // 데이터 읽기
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    setLog(TAG, "onCharacteristicRead - GATT_SUCCESS")
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                }
                BluetoothGatt.GATT_FAILURE -> {
                    setLog(TAG, "onCharacteristicRead - GATT_FAILURE")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    // 초기화
    fun initialize(): Boolean {
        // BluetoothAdapter 체크
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothAdapter ?: run {
            setLog(TAG, "BluetoothAdapter 획득 오류")
            return false
        }

        return true
    }

    // 연결
    @SuppressLint("MissingPermission")
    fun connect(address: String?): Boolean {
        bluetoothAdapter?.let { adapter ->
            return try {
                // BluetoothDevice 객체 생성
                val device = adapter.getRemoteDevice(address)
                // GATT 서버에 연결
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
                true
            } catch (e: IllegalArgumentException) {
                setLog(TAG, "잘못된 주소")
                false
            }
        } ?: run {
            setLog(TAG, "블루투스 어댑터가 존재하지 않습니다.")
            return false
        }
    }

    // 연결 해제
    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            setLog(TAG, "BluetoothAdapter가 초기화되지 않았습니다.")
            return
        }

        bluetoothGatt?.disconnect()
    }

    // Gatt 객체 종료 -> 종료하지 않으면 배터리 누수 발생
    @SuppressLint("MissingPermission")
    private fun close() {
        bluetoothGatt?.let {
            it.close()
            bluetoothGatt = null
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic) ?: run {
            setLog(TAG, "BluetoothAdapter가 초기화되지 않았습니다.")
            return
        }
    }

    @SuppressLint("MissingPermission")
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        bluetoothGatt?.setCharacteristicNotification(characteristic, enabled) ?: run {
            setLog(TAG, "BluetoothAdapter가 초기화되지 않았습니다.")
            return
        }
    }

    fun getSupportedGattServices(): List<BluetoothGattService>? {
        bluetoothGatt ?: run {
            return null
        }

        return bluetoothGatt?.services ?: emptyList()
    }

    // Broadcast 메세지 보내기
    private fun broadcastUpdate(
        action: String,
        characteristic: BluetoothGattCharacteristic? = null
    ) {
        // characteristic가 null이 아니라면
        characteristic?.let {

        } ?: run { // null 이라면
            val intent = Intent(action)
            sendBroadcast(intent)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    // 서비스 bind
    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    // 서비스 unbind
    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private val mBinder = LocalBinder()
}