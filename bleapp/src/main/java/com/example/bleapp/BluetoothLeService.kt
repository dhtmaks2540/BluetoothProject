package com.example.bleapp

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.bleapp.ble.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

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

        val GENERIC_ACCESS = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
        val GENERIC_ATTRIBUTES = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        val DEVICE_INFORMATION = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val UNKNOWN_SERVICE_1 = UUID.fromString("581f3b86-6e63-48cc-a618-288167d2c4a2")
        val UNKNOWN_SERVICE_2 = UUID.fromString("1d14d6ee-fd63-4fa1-bfa4-8f47b42119f0")
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var bluetoothGatt: BluetoothGatt? = null

    // BluetoothGattCallback
    private val gattCallback = object : BluetoothGattCallback() {
        // GATT 서버에 대한 연결이 변경될 때 트리거
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            setLog(TAG, "onConnectionStateChange : $status $newState")

            when (newState) {
                // GATT Server에 연결이 되었을 때
                BluetoothProfile.STATE_CONNECTED -> {
                    val intentAction = ACTION_GATT_CONNECTED
                    broadcastUpdate(intentAction)
                    // 연결에 성공한 후 services 검색
                    bluetoothGatt?.discoverServices()
                }
                // Gatt Server에 연결이 끊겼을 때
                BluetoothProfile.STATE_DISCONNECTED -> {
                    val intentAction = ACTION_GATT_DISCONNECTED
                    broadcastUpdate(intentAction)
                }
                else -> {
                    close()
                }
            }
        }

        // 원격 기기의 service, characteristic, descriptor가 업데이트 될때 트리거
        // 즉, 새로운 service가 발견될 때 트리거
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)


            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    val gattServices = gatt?.services
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                    gattServices?.forEach { service ->
                        val characteristicsTable = service.characteristics.joinToString(
                            separator = "\\n|--",
                            prefix = "|--",
                        ) { char ->
                            var description = "${char.uuid}: ${char.printProperties()}"
                            if(char.descriptors.isNotEmpty()) {
                                description += "\n" + char.descriptors.joinToString(
                                    separator = "\\n|------",
                                    prefix = "|------"
                                ) { descriptor ->
                                    "${descriptor.uuid}: ${descriptor.printProperties()}"
                                }
                            }

                            description
                        }
                        setLog(TAG, "Service ${service.uuid}\nCharacteristics:\n$characteristicsTable")
                    }

//                    enableNotifications(notifiableCharacteristic)
                    setLog(TAG, "ACTION_GATT_SERVICES_DISCOVERED")
                }
                else -> {
                    setLog(TAG, "onServicesDiscovered : $status")
                }
            }
        }

        // characteristic 읽기 작업의 결과를 보고할 때 트리거
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        setLog(TAG, "Read characteristic ${this?.uuid}\n${this?.value?.toHexString()} - ${this?.value?.size}")
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        setLog(TAG, "Read not permitted ${this?.uuid}!")
                    }
                    else -> {
                        setLog(TAG, "Characteristic read failed for ${this?.uuid}, error: $status")
                    }
                }
            }
        }

        //  characteristic 알림의 결과로 트리거.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            setLog(TAG, "onCharacteristicChanged: ${characteristic?.getStringValue(0)}")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            with(characteristic) {
                when(status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        setLog(TAG, "BluetoothGattCallback Wrote to characteristic ${this?.uuid} | value: ${this?.value?.toHexString()}\"")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        setLog(TAG, "BluetoothGattCallback Write not permitted for ${this?.uuid}!")
                    }
                    else -> {
                        setLog(TAG, "BluetoothGattCallback Characteristic write failed for ${this?.uuid}, error: $status")
                    }
                }
            }
        }
    }

    // 초기화
    fun initialize(): Boolean {
        // BluetoothAdapter 체크
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
    fun readData(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let {
            val serviceUUID = characteristic.service.uuid
            val characterChar = it.getService(serviceUUID).getCharacteristic(characteristic.uuid)

            if(characterChar.isReadable()) {
                setLog(TAG,"READABLE")
                it.readCharacteristic(characterChar)
            } else {
                setLog(TAG, "NOT READABLE")
            }
        } ?: run {
            setLog(TAG, "BluetoothAdapter가 초기화되지 않았습니다.")
        }
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic?, payload: ByteArray) {
        characteristic?.let {
            val writeType = when {
                characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                characteristic.isWritableWithoutResponse() -> BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                else -> error("Characteristic ${characteristic.uuid} cannot be written to")
            }

            setLog(TAG, "WRITE : $writeType")

            bluetoothGatt?.let { gatt ->
                characteristic.writeType = writeType
                characteristic.value = payload
                gatt.writeCharacteristic(characteristic)
            } ?: error("Not connected to a BLE device!")
        }
    }

    @SuppressLint("MissingPermission")
    fun writeDescriptor(descriptor: BluetoothGattDescriptor, payload: ByteArray) {
        bluetoothGatt?.let { gatt ->
            descriptor.value = payload
            gatt.writeDescriptor(descriptor)
        } ?: error("Not connected to a BLE device!")
    }

    @SuppressLint("MissingPermission")
    fun enableNotifications(characteristic: BluetoothGattCharacteristic?) {
        characteristic?.let {
            val cccdUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val payload = when {
                characteristic.isIndicatable() -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                characteristic.isNotifiable() -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else -> {
                    setLog(TAG, "${characteristic.uuid} doesn't support notifications/indications")
                    return
                }
            }

            characteristic.getDescriptor(cccdUUID)?.let { cccDescriptor ->
                if(bluetoothGatt?.setCharacteristicNotification(characteristic, true) == false) {
                    setLog(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                    return
                }

                writeDescriptor(cccDescriptor, payload)
            } ?: setLog(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
        }
    }

    @SuppressLint("MissingPermission")
    fun disableNotifications(characteristic: BluetoothGattCharacteristic) {
        if(!characteristic.isNotifiable() && !characteristic.isIndicatable()) {
            setLog(TAG, "${characteristic.uuid} doesn't support indications/notifications")
            return
        }

        val cccUuid = UUID.fromString("581f3b86-6e63-48cc-a618-288167d2c4a5")
        characteristic.getDescriptor(cccUuid)?.let { cccDescriptor ->
            if(bluetoothGatt?.setCharacteristicNotification(characteristic, false) == false) {
                setLog(TAG, "setCharacteristicNotification failed for ${characteristic.uuid}")
                return
            }
            writeDescriptor(cccDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
        } ?: setLog(TAG, "${characteristic.uuid} doesn't contain the CCC descriptor!")
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
        val intent = Intent(action)
        // characteristic가 null이 아니라면
        characteristic?.let {
            val flag = it.properties
            var format = 0
            if((flag and 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8
            }

            val rate = it.getIntValue(format, 1)
            intent.putExtra("DATA", rate)
            sendBroadcast(intent)
        } ?: run { // null 이라면
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