package com.example.bleapp

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BluetoothLeService: Service(){
    companion object {
        private const val TAG = "BLUETOOTH_SERVICE"
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var bluetoothDeviceAddress: String

    private var mConnectionState = STATE_DISCONNECTED

    private val mGattCallback = object : BluetoothGattCallback() {
        // GATT 서버에 대한 연결이 변경될 때 트리거
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            when(newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // GATT Server에 연결에 성공했을 때
                    val intentAction = ACTION_GATT_CONNECTED
                    mConnectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    setLog(TAG, "연결 실패")
                }
            }
        }

        // 서비스 발견
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            when(status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
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

            when(status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
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

    fun init(): Boolean {
        if(bluetoothManager == null) {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if(bluetoothManager == null) {
                setLog(TAG, "BluetoothManager 초기화 오류")
                return false
            }
        }

        bluetoothAdapter = bluetoothManager?.adapter
        if(bluetoothAdapter == null) {
            setLog(TAG, "BluetoothAdapter 획득 오류")
            return false
        }

        return true
    }

    fun connect(address: String?): Boolean {
        bluetoothAdapter?.let { adapter ->
            return try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, false, mGattCallback)
                mConnectionState = STATE_CONNECTING
                true
            } catch (e: IllegalArgumentException) {
                setLog(TAG, "잘못된 주소")
                false
            }
        } ?: run {
            setLog(TAG, "블루투스 어댑터가 초기화 X")
            return false
        }
    }

    fun disconnect() {
        if(bluetoothAdapter == null || bluetoothGatt == null) {
            setLog(TAG, "BluetoothAdapter가 초기화되지 않았습니다.")
            return
        }

        bluetoothGatt?.disconnect()
    }

    fun close() {
        if(bluetoothGatt == null) return

        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    // Broadcast 메세지 보내기
    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic? = null) {
        if(characteristic == null) {
            val intent = Intent(action)
            sendBroadcast(intent)
        } else {

        }
    }

    inner class LocalBinder: Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private val mBinder = LocalBinder()
}