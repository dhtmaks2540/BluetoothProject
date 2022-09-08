package com.example.bluetoothproject

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.activity.viewModels
import com.example.bluetoothproject.base.BaseActivity
import com.example.bluetoothproject.bluetooth.*
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.databinding.ActivityConnectBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ConnectActivity @Inject constructor() : BaseActivity<ActivityConnectBinding, ConnectViewModel>(R.layout.activity_connect) {
    @Inject
    lateinit var connectedHandler: ConnectedHandler
    private lateinit var connectThread: ConnectThread

    @Inject
    lateinit var bluetoothMode: BluetoothMode

    override val viewModel: ConnectViewModel by viewModels()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
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
        binding.apply {
            vm = viewModel

            btnDisconnect.setOnClickListener {
                viewModel.deviceAddress.value?.let { address ->
                    connectDevice(address)
                }
            }

            btnMeasure.setOnClickListener {
                startMeasure()
            }
        }

        connectedHandler.setListener(object : HandlerMessageListener {
            override fun getFlagMessage(flag: Boolean) {
                viewModel.setConnected(flag)
            }

            override fun getModeData(modeData: Any) {
                useTimber("Mode : $bluetoothMode")
                when(bluetoothMode) {
                    BluetoothMode.WAITING_MODE -> {
                        val data = modeData as StreamWaitingMode
                        useTimber("Wait - ${data.remainTime}")
                    }
                    BluetoothMode.CHARGE_MODE -> {
                        val data = modeData as StreamChargeMode
                        useTimber("Charge - ${data.packetCount}")
                    }
                }
            }
        })

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
                if (!adapter.isEnabled) {
                    showMessage(this, "블루투스가 비활성화되어 있습니다.")
                    return
                }

                // 기기 검색을 수행중이라면 취소
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }

                // 서버의 역할을 수행 할 Device 획득
                val device = adapter.getRemoteDevice(deviceAddress)
                // UUID 선언
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                try {
                    connectThread = ConnectThread(uuid, device, connectedHandler)

                    /**
                     * run은 스레드의 override된 메서드만 호출하는 것
                     * start는 해당 쓰레드를 new에서, run이 가능한 상태로 만들어 준다.
                     */
                    connectThread.start()

                    showMessage(this, "연결 시도")
                } catch (e: Exception) { // 연결에 실패할 경우 호출됨
                    useTimber("Connect Exception")
                }
            }
        }
    }

    private fun startMeasure() {
        if (bluetoothAdapter?.isEnabled == false) {
            showMessage(this, "블루투스가 비활성화되어 있습니다.")
            return
        }
        if (viewModel.isConnected.value == false) {
            showMessage(this, "연결이 되지않아 측정이 불가합니다.")
            return
        }
        if (viewModel.dataList.value?.get(2) != 1) {
            showMessage(this, "헤드셋이 이마에 밀착되지 않았거나 오른쪽 귓볼에 센서가 착용되지 않았습니다.")
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