package com.example.bluetoothproject

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.bluetoothproject.databinding.ActivityMainBinding
import com.example.bluetoothproject.di.BluetoothReceiver
import dagger.hilt.android.AndroidEntryPoint
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject

private const val TAG = "MAIN_ACTIVITY"
private const val BLUETOOTH_PERMISSION = 1

@AndroidEntryPoint
class MainActivity @Inject constructor() : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val activityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                showMessage(this, "블루투스 활성화")
            } else if (it.resultCode == RESULT_CANCELED) {
                showMessage(this, "취소")
            }
        }

    @Inject
    lateinit var receiver: BluetoothReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityMainBinding?>(this, R.layout.activity_main)
            .apply {
                handler = this@MainActivity
            }

        init()
    }

    private fun init() {
        // 블루투스 초기화
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        setBluetooth()
        registerReceiver(receiver, receiver.filter)
    }

    // 초기 권한 확인
    private fun setBluetooth() {
        if (checkPermission()) {
            setUi(true)
        } else {
            requestPermission()
        }
    }

    // 권한에 따른 UI 분기
    private fun setUi(flag: Boolean) {
        binding.apply {
            btnCon.isEnabled = flag
            btnDiscon.isEnabled = flag
            btnPair.isEnabled = flag
            btnSearch.isEnabled = flag
        }
    }

    // 권한확인
    private fun checkPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

    // 권한요청
    private fun requestPermission() {
        if (!checkPermission()) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION)
        }
    }

    // 연결
    fun setConnect() {
        if (!::bluetoothAdapter.isInitialized) {
            showMessage(this, "블루투스를 지원하지 않는 장치입니다.")
        } else if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(intent)
        } else {
            showMessage(this, "이미 활성화가 되었습니다.")
        }
    }

    // 연결해제
    @SuppressLint("MissingPermission")
    fun setDisConnect() {
        if (!bluetoothAdapter.isEnabled) {
            showMessage(this, "이미 비활성화 되어 있습니다.")
        } else {
            bluetoothAdapter.disable()
            showMessage(this, "블루투스를 비활성화 하였습니다.")
        }
    }

    // 페어링된 디바이스 검색
    @SuppressLint("MissingPermission")
    fun getPairedDevices() {
        if (bluetoothAdapter.isEnabled) {
            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            if (pairedDevices.isNotEmpty()) {
                val alertBuilder = AlertDialog.Builder(this).apply {
                    val listPairedDevices = pairedDevices.map { it.name }
                    val items = listPairedDevices.toTypedArray()
                    setItems(
                        items
                    ) { _, index ->
                        connectDevice(pairedDevices, items[index])
                    }
                }

                val alertDialog = alertBuilder.create()
                alertDialog.show()
            } else {
                showMessage(this, "페어링된 기기가 없습니다.")
            }
        } else {
            showMessage(this, "블루투스가 비활성화 되어 있습니다.")
        }
    }

    // 기기 검색(권한에 따른 오류 예상, 더 검색 필요 https://kimyounghoons.github.io/android/android-bluetooth-android10/)
    fun findDevice() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this, permissions, 2)

        val flag = bluetoothAdapter.startDiscovery()
        setLog(TAG, "StartDiscovery : $flag")
        showMessage(this, "기기 검색을 시작하였습니다.")
    }

    // 디바이스에 연결
    private fun connectDevice(devices: Set<BluetoothDevice>, deviceName: String) {
        val device = devices.first { it.name == deviceName }
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        val thread = ConnectThread(uuid, device)

        if(thread.isAlive) {
            thread.cancel()
            return
        }

        thread.run()
        setLog(TAG, "Connect : ${isConnected(device)} - ${device.address} - ${device.name} - ${device.uuids}")
    }


    // 연결되었는지 확인하는 메서드
    private fun isConnected(device: BluetoothDevice): Boolean? {
        return try {
            val m = device.javaClass.getMethod("isConnected")
            m.invoke(device) as? Boolean
        } catch (e: Exception) {
            showMessage(this, e.message.toString())
            null
        }
    }

    // Client Thread
    @SuppressLint("MissingPermission")
    private inner class ConnectThread(myUUid: UUID, device: BluetoothDevice) : Thread() {
        val connectSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(myUUid)
        }

        override fun run() {
            bluetoothAdapter.cancelDiscovery()

            try {
                connectSocket?.let { socket ->
                    socket.connect()
                    Log.d(TAG, "SOCKET : ${connectSocket?.isConnected} - $connectSocket - ${connectSocket?.remoteDevice} - ${connectSocket?.inputStream}")
                    val connectedThread = ConnectedThread(socket)
                    connectedThread.start()
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message.toString())
            }
        }

        fun cancel() {
            try {
                connectSocket?.close()
            } catch (e: Exception) {
                setLog(TAG, e.message.toString())
            }
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream
        private val buffer = ByteArray(1024)

        init {
            try {
                inputStream = socket.inputStream
                outputStream = socket.outputStream
            } catch (e: Exception) {
                setLog(TAG, e.message.toString())
            }
        }

        override fun run() {
            var numBytes: Int
            while(!this.isInterrupted) {
                sleep(5000)
                numBytes = try {
                    inputStream.read(buffer)
                } catch (e: Exception) {
                    setLog(TAG, "Disconnected")
                    break
                }
                write(buffer)
                setLog(TAG, "RECEIVE_DATA")
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
                setLog(TAG, "SEND_DATA")
            } catch (e: Exception) {
                setLog(TAG, "SEND_ERROR")
            }
        }
    }

    // 권한요청 결과
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            BLUETOOTH_CONNECT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setUi(true)
                } else {
                    setUi(false)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bluetoothAdapter.isInitialized) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val result =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                if (result == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), BLUETOOTH_SCAN)
                }
            }
        }

        unregisterReceiver(receiver)
    }
}