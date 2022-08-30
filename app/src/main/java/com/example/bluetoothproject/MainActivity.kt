package com.example.bluetoothproject

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.bluetoothproject.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*
import java.util.stream.Collectors
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity @Inject constructor() : AppCompatActivity() {
    companion object {
        private const val TAG = "MAIN_ACTIVITY"
        private const val BLUETOOTH_CONNECT = 1
        private const val BLUETOOTH_SCAN = 2
        private const val BLUETOOTH_PERMISSION = 100
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val adapter: ArrayAdapter<Pair<String, String>> by lazy {
        ArrayAdapter(this, android.R.layout.simple_list_item_1)
    }

    // BroadcastReceiver
    private lateinit var bluetoothBroadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun init() {
        // binding 초기화
        binding = DataBindingUtil.setContentView<ActivityMainBinding?>(this, R.layout.activity_main)
            .apply {
                handler = this@MainActivity
                arrayAdapter = adapter
                lvDevice.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        adapter.getItem(position)?.second?.let {
                            connectDevice(it)
                        }
                    }
            }

        // 블루투스 초기화
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        // ActivityResultLauncher 초기화
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    showMessage(this, "블루투스 활성화")
                } else if (it.resultCode == RESULT_CANCELED) {
                    showMessage(this, "취소")
                }
            }

        // 블루투스 기기 검색 및 상태변화 브로드캐스트
        bluetoothBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        // BluetoothDevice 객체 획득
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        // 기기 이름
                        val deviceName = device?.name
                        // 기기 주소
                        val deviceHardwareAddress = device?.address
                        // 이름과 주소가 null이 아닌 경우
                        if (deviceName != null && deviceHardwareAddress != null) {
                            adapter.add(Pair(deviceName, deviceHardwareAddress))
                        }
                    }

                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )) {
                            // 비활성화
                            BluetoothAdapter.STATE_OFF -> {

                            }
                            // 비활성화 되고 있음
                            BluetoothAdapter.STATE_TURNING_OFF -> {
                                showMessage(this@MainActivity, "블루투스가 비활성화되어 연결을 중단합니다.")
                            }
                            // 활성화
                            BluetoothAdapter.STATE_ON -> {

                            }
                            // 활성화 되고 있음
                            BluetoothAdapter.STATE_TURNING_ON -> {

                            }
                        }
                    }
                }
            }
        }

        // Register BroadcastReceiver
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothBroadcastReceiver, intentFilter)

        setBluetooth()
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
            btnActivate.isEnabled = flag
            btnDeactivate.isEnabled = flag
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

    // 활성화 요청
    fun setActivate() {
        bluetoothAdapter?.let {
            // 비활성화 상태라면
            if (!it.isEnabled) {
                // 활성화 요청
                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                activityResultLauncher.launch(intent)
            } else { // 활성 상태라면
                showMessage(this, "이미 활성화 되어 있습니다")
            }
            return
        }
        showMessage(this, "블루투스를 지원하지 않는 장치")
    }

    // 비활성화 요청
    fun setDeActivate() {
        bluetoothAdapter?.let {
            // 비활성화 상태라면
            if (!it.isEnabled) {
                showMessage(this, "이미 비활성화 되어 있습니다")
                // 활성 상태라면
            } else {
                // 블루투스 비활성화
                it.disable()
                showMessage(this, "블루투스를 비활성화 하였습니다")
            }

            return
        }

        showMessage(this, "블루투스를 지원하지 않는 장치")
    }

    // 페어링된 디바이스 검색
    fun getPairedDevices() {
        bluetoothAdapter?.let {
            // 블루투스 활성화 상태라면
            if (it.isEnabled) {
                // ArrayAdapter clear
                adapter.clear()
                // 페어링된 기기 확인
                val pairedDevices: Set<BluetoothDevice> = it.bondedDevices
                // 페어링된 기기가 존재하는 경우
                if (pairedDevices.isNotEmpty()) {
                    pairedDevices.forEach { device ->
                        // ArrayAdapter에 아이템 추가
                        adapter.add(Pair(device.name, device.address))
                    }
                } else {
                    showMessage(this, "페어링된 기기가 없습니다.")
                }
            } else {
                showMessage(this, "블루투스가 비활성화 되어 있습니다.")
            }

            return
        }

        showMessage(this, "블루투스를 지원하지 않는 장치")
    }

    // 기기 검색
    fun findDevice() {
        bluetoothAdapter?.let {
            // 블루투스가 활성화 상태라면
            if (it.isEnabled) {
                // 현재 검색중이라면
                if (it.isDiscovering) {
                    // 검색 취소
                    it.cancelDiscovery()
                    showMessage(this, "기기검색이 중단되었습니다.")
                    return
                }

                // ArrayAdapter clear
                adapter.clear()
                // 검색시작
                it.startDiscovery()
                showMessage(this, "기기 검색을 시작하였습니다")
            } else {
                showMessage(this, "블루투스가 비활성화되어 있습니다")
            }
            return
        }

        showMessage(this, "블루투스를 지원하지 않는 장치")
    }

    // 디바이스에 연결
    private fun connectDevice(deviceAddress: String) {
        bluetoothAdapter?.let { adapter ->
            // 기기 검색을 수행중이라면 취소
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }

            // 서버의 역할을 수행 할 Device 획득
            val device = adapter.getRemoteDevice(deviceAddress)
            // UUID 선언
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            try {
                val thread = ConnectThread(uuid, device)

                thread.run()
                showMessage(this, "${device.name}과 연결되었습니다.")
            } catch (e: Exception) { // 연결에 실패할 경우 호출됨
                showMessage(this, "기기의 전원이 꺼져 있습니다. 기기를 확인해주세요.")
                return
            }
        }
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
        bluetoothAdapter?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val result =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                if (result == PackageManager.PERMISSION_GRANTED) {
                    it.cancelDiscovery()
                } else {
                    requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN), BLUETOOTH_SCAN)
                }
            }
        }

        unregisterReceiver(bluetoothBroadcastReceiver)
    }
}