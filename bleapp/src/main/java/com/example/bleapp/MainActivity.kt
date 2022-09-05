package com.example.bleapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.databinding.DataBindingUtil
import com.example.bleapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MAIN_ACTIVITY"
        private const val BLUETOOTH_PERMISSION = 1
        const val DEVICE_NAME = "name"
        const val DEVICE_ADDRESS = "address"
    }

    private var isScanning = false
    private val scanResult = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        if(isScanning) {
            stopBleScan()
        }
        ScanResultAdapter(scanResultClicked = {
            val intent = Intent(this, ConnectActivity::class.java)
            intent.putExtra(DEVICE_NAME, it.device.name)
            intent.putExtra(DEVICE_ADDRESS, it.device.address)
            startActivity(intent)
        })
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // DataBinding
    private val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    // Permission Check
    private val isPermissionGranted
        get() = hasPermission(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        )

    // BluetoothAdapter, BluetoothLeScanner 초기화
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private lateinit var handler: Handler

    // 스캔에 대한 결과를 Callback 해주는 객체
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        // 스캔 성공 - > 결과
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val indexQuery = scanResult.indexOfFirst { it.device.address == result.device.address }

            if (result.device.name != null && result.device.address != null) {
                if (indexQuery != -1) {
                    scanResult[indexQuery] = result
                    scanResultAdapter.notifyItemChanged(indexQuery)
                } else {
                    with(result.device) {
                        setLog(TAG, "Found BLE device! Name : ${name ?: "Unnamed"}, address : $address")
                    }
                    scanResult.add(result)
                    scanResultAdapter.notifyItemInserted(scanResult.size - 1)
                }
            }
        }

        // 스캔 실패
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            setLog(TAG, "onScanFailed: code $errorCode")
        }
    }

    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            showMessage(this, "블루투스 활성화")
        } else if (it.resultCode == RESULT_CANCELED) {
            showMessage(this, "취소")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BLE를 지원하지 않는 기기라면 종료
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showMessage(this, "BLE를 지원하지 않는 기기입니다.")
            finish()
        }

        // 블루투스를 지원하지 않는 기기라면 종료
        if (bluetoothAdapter == null) {
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
        when (requestCode) {
            BLUETOOTH_PERMISSION -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
                        showMessage(this, "권한을 허용하지 않아 앱이 종료됩니다.")
                        finish()
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onResume() {
        super.onResume()

        bluetoothAdapter?.let {
            if (!it.isEnabled) setActivate()
        }
    }

    // 초기화
    private fun init() {
        binding.apply {
            handler = this@MainActivity
            adapter = scanResultAdapter

            btnSearch.setOnClickListener {
                if(isScanning) {
                    stopBleScan()
                } else {
                    scanLeDevice()
                }
            }
        }

        scanResultAdapter.submitList(scanResult)
        setBluetooth()
        handler = Handler(Looper.getMainLooper())
    }

    // 초기 권한 확인
    private fun setBluetooth() {
        if (!isPermissionGranted) {
            requestPermission()
        }
    }

    // 권한요청
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            requestPermissions(permissions, BLUETOOTH_PERMISSION)
        } else {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            requestPermissions(permissions, BLUETOOTH_PERMISSION)
        }
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
                // 활성 상태라면
            } else {
                // 블루투스 비활성화
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

    // BLE 기기 검색
    fun scanLeDevice() {
        // 권한이 허용되어 있지 않다면
        if (!isPermissionGranted) {
            requestPermission()
        } else {
            // 블루투스가 활성화되어 있지 않다면 return
            bluetoothAdapter?.let {
                if (!it.isEnabled) {
                    showMessage(this, "블루투스가 활성화되어 있지 않습니다.")
                    return
                }
            }
            // 스캔하지 않고 있다면
            if (!isScanning) {
                scanResult.clear()
                bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
                isScanning = true
            }
        }
    }

    private fun stopBleScan() {
        bluetoothLeScanner?.stopScan(leScanCallback)
        isScanning = false
    }
}