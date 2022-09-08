package com.example.bluetoothproject

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.bluetoothproject.base.BaseActivity
import com.example.bluetoothproject.bluetooth.BLUETOOTH_CONNECT
import com.example.bluetoothproject.bluetooth.BLUETOOTH_PERMISSION
import com.example.bluetoothproject.bluetooth.BLUETOOTH_SCAN
import com.example.bluetoothproject.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class MainActivity() : BaseActivity<ActivityMainBinding, MainViewModel>(R.layout.activity_main) {
    override val viewModel: MainViewModel by viewModels()
    // 권한 확인
    private val isPermissionGranted
        get() = hasPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf()
            }
        )

    // 블루투스 어댑터
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // 활성화 결과용
    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            showMessage(this, "블루투스 활성화")
        } else if (it.resultCode == RESULT_CANCELED) {
            showMessage(this, "취소")
        }
    }

    // RecyclerAdapter
    private val recyclerAdapter: DeviceRecyclerAdapter by lazy {
        DeviceRecyclerAdapter(deviceClicked = { device ->
            val intent = Intent(this, ConnectActivity::class.java)
            intent.putExtra(DEVICE_ADDRESS, device.address)
            intent.putExtra(DEVICE_NAME, device.name)
            startActivity(intent)
        })
    }
    private val deviceList = mutableListOf<BluetoothDevice>()

    // BroadcastReceiver
    private lateinit var bluetoothBroadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 블루투스를 지원하지 않는 기기라면 종료
        if (bluetoothAdapter == null) {
            showMessage(this, "블루투스를 지원하지 않는 기기입니다.")
            finish()
        }

        init()
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

    override fun onResume() {
        super.onResume()

        bluetoothAdapter?.let {
            if (!it.isEnabled) setActivate()
        }

        registerReceiver(bluetoothBroadcastReceiver, getIntentFilter())
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

    // 초기화
    private fun init() {
        setBluetooth()

        binding.apply {
            adapter = recyclerAdapter
            handler = this@MainActivity
        }

        recyclerAdapter.submitList(deviceList)

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
                        if (deviceName != null && deviceName == "neuroNicle FX2" && deviceHardwareAddress != null) {
                            val indexQuery =
                                deviceList.indexOfFirst { it.address == deviceHardwareAddress }
                            if (indexQuery != -1) {
                                deviceList[indexQuery] = device
                                recyclerAdapter.notifyItemChanged(indexQuery)
                            } else {
                                deviceList.add(device)
                                recyclerAdapter.notifyItemInserted(deviceList.size - 1)
                            }
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
    }

    // 초기 권한 확인
    private fun setBluetooth() {
        if (!isPermissionGranted) {
            requestPermission()
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

    // 활성화 요청
    fun setActivate() {
        bluetoothAdapter?.let {
            if (!isPermissionGranted) {
                requestPermission()
            } else {
                // 비활성화 상태라면
                if (!it.isEnabled) {
                    // 활성화 요청
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activityResultLauncher.launch(intent)
                } else { // 활성 상태라면
                    showMessage(this, "이미 활성화 되어 있습니다")
                }
            }
        }
    }

    // 비활성화 요청
    fun setDeActivate() {
        bluetoothAdapter?.let {
            // 비활성화 상태라면
            if (!it.isEnabled) {
                showMessage(this, "이미 비활성화 되어 있습니다")
            } else { // 활성 상태라면
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!isPermissionGranted) {
                        requestPermission()
                    } else {
                        it.disable()
                        showMessage(this, "블루투스를 비활성화 하였습니다")
                    }
                }
            }
        }
    }

    // 페어링된 디바이스 검색
    fun getPairedDevices() {
        bluetoothAdapter?.let {
            if (!isPermissionGranted) {
                requestPermission()
            } else {
                // 블루투스 활성화 상태라면
                if (it.isEnabled) {
                    deviceList.clear()
                    recyclerAdapter.notifyDataSetChanged()
                    // 페어링된 기기 확인
                    val pairedDevices: Set<BluetoothDevice> = it.bondedDevices
                    // 페어링된 기기가 존재하는 경우
                    if (pairedDevices.isNotEmpty()) {
                        pairedDevices.forEach { device ->
                            // ArrayAdapter에 아이템 추가
                            deviceList.add(device)
                        }
                    } else {
                        showMessage(this, "페어링된 기기가 없습니다.")
                    }
                } else {
                    showMessage(this, "블루투스가 비활성화 되어 있습니다.")
                }
            }
        }
    }

    // 기기 검색
    fun findDevice() {
        bluetoothAdapter?.let {
            if (!isPermissionGranted) {
                requestPermission()
            } else {
                // 블루투스가 활성화 상태라면
                if (it.isEnabled) {
                    // 현재 검색중이라면
                    if (it.isDiscovering) {
                        // 검색 취소
                        it.cancelDiscovery()
                        showMessage(this, "기기검색이 중단되었습니다.")
                        return
                    }

                    deviceList.clear()
                    recyclerAdapter.notifyDataSetChanged()
                    // 검색시작
                    it.startDiscovery()
                    showMessage(this, "기기 검색을 시작하였습니다")
                } else {
                    showMessage(this, "블루투스가 비활성화되어 있습니다")
                }
            }
        }
    }

    private fun getIntentFilter() = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_FOUND)
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    }
}
