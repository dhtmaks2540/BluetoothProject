package com.example.bluetoothproject

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.bluetoothproject.databinding.ActivityConnectBinding
import timber.log.Timber
import java.util.*

class ConnectActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CONNECT_ACTIVITY"
    }

    private val binding: ActivityConnectBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_connect)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val connectViewModel: ConnectViewModel by viewModels()

    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var isConnected = false
        set(value) {
            field = value
            if (field) {
                binding.apply {
                    tvState.text = "연결"
                    btnDisconnect.text = "연결 해제"
                }
            } else {
                binding.apply {
                    tvState.text = "미연결"
                    btnDisconnect.text = "연결"
                }

            }
        }

    // 권한 확인
    private val isPermissionGranted
        get() = hasPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf()
            }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()
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
                if (grantResults.isNotEmpty()) {
                    if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
                        showMessage(this, "권한이 허용되지 않았습니다.")
                        finish()
                    }
                }
            }
        }
    }

    private fun init() {
        deviceAddress = intent.getStringExtra(DEVICE_ADDRESS)
        deviceName = intent.getStringExtra(DEVICE_NAME)

        binding.apply {
            viewModel = connectViewModel
            lifecycleOwner = this@ConnectActivity
            btnDisconnect.setOnClickListener {
                deviceAddress?.let { address ->
                    connectDevice(address)
                }
            }
            tvName.text = deviceName
            tvAddress.text = deviceAddress
        }

        setUpTimber()
    }

    // 팀버 초기화
    private fun setUpTimber() {
        Timber.plant(Timber.DebugTree())
    }

    // 디바이스에 연결
    @SuppressLint("MissingPermission")
    private fun connectDevice(deviceAddress: String) {
        bluetoothAdapter?.let { adapter ->
            if (!isPermissionGranted) {
                requestPermission()
            } else {
                // 기기 검색을 수행중이라면 취소
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }

                // 서버의 역할을 수행 할 Device 획득
                val device = adapter.getRemoteDevice(deviceAddress)
                // UUID 선언
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                try {
                    val thread = ConnectThread(uuid, device, connectViewModel)

                    /**
                     * run은 스레드의 override된 메서드만 호출하는 것
                     * start는 해당 쓰레드를 new에서, run이 가능한 상태로 만들어 준다.
                     */
                    thread.start()
                    isConnected = true
                    showMessage(this, "${device.name}과 연결되었습니다.")
                } catch (e: Exception) { // 연결에 실패할 경우 호출됨
                    isConnected = false
                    showMessage(this, e.message.toString())
                    return
                }
            }
        }
    }

    // 권한요청
    private fun requestPermission() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        requestPermissions(permissions, BLUETOOTH_PERMISSION)
    }
}