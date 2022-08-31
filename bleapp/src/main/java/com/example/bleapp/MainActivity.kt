package com.example.bleapp

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.bleapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MAIN_ACTIVITY"
        private const val BLUETOOTH_PERMISSION = 1
        private const val SCAN_PERIOD: Long = 15000
        const val DEVICE_NAME = "name"
        const val DEVICE_ADDRESS = "address"
    }
    private lateinit var binding: ActivityMainBinding
    
    // BluetoothManager, BluetoothAdapter, BluetoothLeScanner 초기화
    private val bluetoothManager: BluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private var scanning = false
    private lateinit var handler: Handler

    // 스캔에 대한 결과를 Callback 해주는 객체
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        // 스캔 성공 - > 결과
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                if(it.device.name != null && it.device.address != null) {
                    adapter.add(Pair(it.device.name, it.device.address))
                }
            }
        }

        // 스캔 실패
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            setLog(TAG, errorCode.toString())
        }
    }

    private lateinit var gattCallback: BluetoothGattCallback

    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            showMessage(this, "블루투스 활성화")
        } else if (it.resultCode == RESULT_CANCELED) {
            showMessage(this, "취소")
        }
    }

    private val adapter: ArrayAdapter<Pair<String, String>> by lazy {
        ArrayAdapter(this, android.R.layout.simple_list_item_1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        
        // BLE를 지원하지 않는 기기라면 종료
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage(this, "BLE를 지원하지 않는 기기입니다.")
            finish()
        }

        // 블루투스를 지원하지 않는 기기라면 종료
        if(bluetoothAdapter == null) {
            showMessage(this, "블루투스를 지원하지 않는 기기입니다.")
            finish()
        }

        init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            BLUETOOTH_PERMISSION -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                    setUi(true)
                } else {
                    setUi(false)
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun init() {
        binding.apply {
            handler = this@MainActivity
            arrayAdapter = adapter
            lvDevice.setOnItemClickListener { _, _, position, _ ->
                adapter.getItem(position)?.let {
                    val intent = Intent(this@MainActivity, ConnectActivity::class.java)
                    intent.putExtra(DEVICE_NAME, it.first)
                    intent.putExtra(DEVICE_ADDRESS, it.second)
                    setLog(TAG, "Name : ${it.first}, Address : ${it.second}")
                    startActivity(intent)
                }
            }
        }

        handler = Handler(Looper.getMainLooper())
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
        }
    }

    // 권한확인
    private fun checkPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

    // 권한요청
    private fun requestPermission() {
        if (!checkPermission()) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            ActivityCompat.requestPermissions(this, permissions, BLUETOOTH_PERMISSION)
        }
    }

    private fun connGatt(device: BluetoothDevice?) {
        val bluetoothGatt = device?.connectGatt(this, false, gattCallback)
        setLog(TAG, bluetoothGatt?.device.toString())
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

    fun scanLeDevice() {
        // 블루투스가 활성화되어 있지 않다면 return
        bluetoothAdapter?.let {
            if(!it.isEnabled) return
        }
        // 스캔하지 않고 있다면
        if(!scanning) {
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)

            adapter.clear()
            
            scanning = true
            bluetoothLeScanner?.startScan(leScanCallback)
            showMessage(this, "기기를 검색합니다.")
        } else {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }
}