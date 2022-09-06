package com.example.bleapp

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.bleapp.ble.*
import com.example.bleapp.databinding.ActivityConnectBinding

class ConnectActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CONNECT_ACTIVITY"
    }

    private val binding: ActivityConnectBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_connect)
    }
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var bluetoothLeService: BluetoothLeService? = null
    private var connected: Boolean = false
        set(value) {
            field = value
            runOnUiThread {
                if (value) {
                    binding.btnDisconnect.text = "연결 해제"
                    binding.tvState.text = "연결"
                } else {
                    binding.btnDisconnect.text = "연결"
                    binding.tvState.text = "연결 해제"
                }
            }
        }
    private val characteristicList = mutableListOf<BluetoothGattCharacteristic>()
    private val recyclerAdapter: CharacteristicAdapter by lazy {
        CharacteristicAdapter(
            characteristicOnClicked = { characteristic ->
                val stateArray = arrayListOf<String>()
                if (characteristic.isReadable()) stateArray.add("Read")
                if (characteristic.isWritable()) stateArray.add("Write")
                if (characteristic.isIndicatable()) stateArray.add("Toggle Indications")
                if (characteristic.isNotifiable()) stateArray.add("Toggle Notifications")
                if (characteristic.isWritableWithoutResponse()) stateArray.add("Write Without Response")
                val builder = AlertDialog.Builder(this)
                    .setTitle("실행할 액션을 선택해주세요.")
                    .setItems(
                        stateArray.toTypedArray()
                    ) { _, pos ->
                        when (stateArray[pos]) {
                            "Read" -> bluetoothLeService?.readData(characteristic)
                            "Write" -> bluetoothLeService?.writeCharacteristic(
                                characteristic,
                                ByteArray(3)
                            )
                            "Toggle Indications" -> bluetoothLeService?.enableNotifications(
                                characteristic
                            )
                            "Toggle Notifications" -> bluetoothLeService?.enableNotifications(
                                characteristic
                            )
                            "Write Without Response" -> bluetoothLeService?.writeCharacteristic(
                                characteristic,
                                ByteArray(3)
                            )
                        }
                    }

                val alterDialog = builder.create()
                alterDialog.show()
            }
        )
    }

    // 서비스 바인딩 상태에 따라서 호출되는 콜백 메서드를 가지는 객체
    private val serviceConnection = object : ServiceConnection {
        // 서비스에 연결이 되었을 때
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            // 서비스 객체 초기화
            bluetoothLeService = (binder as BluetoothLeService.LocalBinder)
                .getService()
            bluetoothLeService?.let { bluetooth ->
                // 초기화를 진행할 수 없다면
                if (!bluetooth.initialize()) {
                    setLog(TAG, "Bluetooth를 초기화할 수 없습니다.")
                    finish()
                }
                // 연결 호출
                bluetooth.connect(deviceAddress)
            }
        }

        // 서비스와 연결이 끊어졌을 때
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService = null
        }
    }

    // 연결상태에 대한 결과를 받는 BroadcastReceiver
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                // 연결 상태라면
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    setLog(TAG, "ACTION_GATT_CONNECTED")
                }
                // 연결이 끊긴 상태라면
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    setLog(TAG, "ACTION_GATT_DISCONNECTED")
                    showMessage(this@ConnectActivity, "기기와의 연결이 끊겼습니다. 기기를 확인해주세요.")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    setLog(TAG, "ACTION_GATT_SERVICES_DISCOVERED")
                    val services = bluetoothLeService?.getSupportedGattServices()
                    services?.forEach { service ->
                        val characteristics = service.characteristics
                        characteristics?.forEach { characteristic ->
                            characteristicList.add(characteristic)
                            recyclerAdapter.notifyItemChanged(characteristicList.size - 1)
                        }
                    }
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val rate: Int = intent.getIntExtra("DATA", 0)
                    setLog(TAG, "ACTION_DATA_AVAILABLE : $rate")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 서비스 unbind
        unbindService(serviceConnection)
        bluetoothLeService = null
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        bluetoothLeService?.let {
            val result = it.connect(deviceAddress)
            setLog(TAG, "Connect 결과 : $result")
        }
    }

    private fun init() {
        deviceAddress = intent.getStringExtra(MainActivity.DEVICE_ADDRESS)
        deviceName = intent.getStringExtra(MainActivity.DEVICE_NAME)

        binding.apply {
            handler = this@ConnectActivity
            adapter = recyclerAdapter
            tvName.text = deviceName
            tvAddress.text = deviceAddress
        }

        // Adapter 초기화
        recyclerAdapter.submitList(characteristicList)

        // 서비스 bind
        val gattServerIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    // IntentFilter 생성
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }

    fun disConnect() {
        if (connected) {
            bluetoothLeService?.disconnect()
        } else {
            bluetoothLeService?.connect(deviceAddress)
        }
    }
}