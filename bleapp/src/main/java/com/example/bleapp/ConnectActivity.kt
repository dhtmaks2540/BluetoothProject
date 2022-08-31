package com.example.bleapp

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.bleapp.databinding.ActivityConnectBinding

class ConnectActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CONNECT_ACTIVITY"
    }

    private lateinit var binding: ActivityConnectBinding
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var bluetoothLeService: BluetoothLeService? = null
    private var connected: Boolean = false
    private val serviceConnection = object : ServiceConnection {
        // 연결이 되었을 때
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bluetoothLeService = (binder as BluetoothLeService.LocalBinder).getService()
            bluetoothLeService?.let { bluetooth ->
                if (!bluetooth.init()) {
                    setLog(TAG, "Bluetooth를 초기화할 수 없습니다.")
                    finish()
                }
                bluetooth.connect(deviceAddress)
            }
        }

        // 연결이 끊어졌을 때
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService = null
        }

    }
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    updateConnectionState("연결")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    updateConnectionState("연결해제")
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_connect)
        init()
    }

    private fun init() {
        deviceAddress = intent.getStringExtra(MainActivity.DEVICE_ADDRESS)
        deviceName = intent.getStringExtra(MainActivity.DEVICE_NAME)

        binding.apply {
            tvName.text = deviceName
            tvAddress.text = deviceAddress
        }

        val gattServerIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServerIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(gattUpdateReceiver)
    }

    override fun onRestart() {
        super.onRestart()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
    }

    private fun updateConnectionState(connected: String) {
        runOnUiThread { Runnable {
            binding.tvState.text = connected
        } }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }
}