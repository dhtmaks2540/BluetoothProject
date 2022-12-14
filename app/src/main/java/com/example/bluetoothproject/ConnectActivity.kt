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
import com.example.bluetoothproject.base.BaseActivity
import com.example.bluetoothproject.bluetooth.*
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.databinding.ActivityConnectBinding
import com.example.bluetoothproject.di.BluetoothMode
import com.example.bluetoothproject.di.BluetoothModeProvider
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
    lateinit var bluetoothModeProvider: BluetoothModeProvider

    override val viewModel: ConnectViewModel by viewModels()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    // ęśí íě¸
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

    override fun onDestroy() {
        super.onDestroy()

        connectedHandler.unRegisterListener()
        if(::connectThread.isInitialized) {
            connectThread.cancel()
        }
    }

    // ęśíěě˛­ ę˛°ęłź
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
                        showMessage(this, "ęśíě´ íěŠëě§ ěěěľëë¤.")
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

        connectedHandler.registerListener(object : HandlerMessageListener {
            override fun getFlagMessage(flag: Boolean) {
                viewModel.setConnected(flag)
                showMessage(this@ConnectActivity, if(flag) "ě°ę˛°" else "ě°ę˛° ëęš")
            }

            override fun getModeData(modeData: StreamMode) {
                viewModel.setPacketData(modeData)
            }
        })

        setUpTimber()
    }

    // íë˛ ě´ę¸°í
    private fun setUpTimber() {
        Timber.plant(Timber.DebugTree())
    }

    // ëë°ě´ě¤ě ě°ę˛°
    @SuppressLint("MissingPermission")
    private fun connectDevice(deviceAddress: String) {
        bluetoothAdapter?.let { adapter ->
            if (!isPermissionGranted) {
                requestPermission()
            } else {
                if (!adapter.isEnabled) {
                    showMessage(this, "ë¸ëŁ¨íŹě¤ę° ëšíěąíëě´ ěěľëë¤.")
                    return
                }

                // ę¸°ę¸° ę˛ěě ěíě¤ě´ëźëŠ´ ěˇ¨ě
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }

                // ěë˛ě ě­í ě ěí í  Device íë
                val device = adapter.getRemoteDevice(deviceAddress)
                // UUID ě ě¸
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                try {
                    connectThread = ConnectThread(uuid, device, connectedHandler, bluetoothModeProvider)

                    /**
                     * runě ě¤ë ëě overrideë ëŠěëë§ í¸ěśíë ę˛
                     * startë í´ëš ě°ë ëëĽź newěě, runě´ ę°ëĽí ěíëĄ ë§ë¤ě´ ě¤ë¤.
                     */
                    connectThread.start()

                } catch (e: Exception) { // ě°ę˛°ě ě¤í¨í  ę˛˝ě° í¸ěśë¨
                    useTimber("Connect Exception")
                }
            }
        }
    }

    private fun startMeasure() {
        if (bluetoothAdapter?.isEnabled == false) {
            showMessage(this, "ë¸ëŁ¨íŹě¤ę° ëšíěąíëě´ ěěľëë¤.")
            return
        }
        
        if (viewModel.isConnected.value == false) {
            showMessage(this, "ę¸°ę¸°ě ě°ę˛°ě´ ëě§ěě ě¸Ąě ě´ ëśę°íŠëë¤.")
            return
        }

        if (bluetoothModeProvider.bluetoothMode != BluetoothMode.ACTIVATE_MODE) {
            showMessage(this, "í¤ëěě ě ëëĄ ě°ŠěŠíě§ ěěěľëë¤.")
            return
        }


    }

    // ęśíěě˛­
    private fun requestPermission() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        requestPermissions(permissions, BLUETOOTH_PERMISSION)
    }
}