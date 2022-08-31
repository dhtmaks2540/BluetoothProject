package com.example.bluetoothproject

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@SuppressLint("MissingPermission")
class ConnectThread(
    private val myUUID: UUID,
    private val device: BluetoothDevice,
) : Thread() {
    companion object {
        private const val TAG = "CONNECT_THREAD"
    }

    // BluetoothDevice 로부터 BluetoothSocket 획득
    private val connectSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(myUUID)
    }

    private val connectedThread: ConnectedThread? by lazy {
        ConnectedThread(connectSocket)
    }

    override fun run() {
        try {
            // 연결 수행
            connectSocket?.connect()
            connectSocket?.let {
                connectedThread?.start()
            }
        } catch (e: IOException) { // 기기와의 연결이 실패할 경우 호출
            connectSocket?.close()
            connectedThread?.let {
                if(it.isAlive) {
                    it.cancel()
                }
            }
            setLog(TAG, e.message.toString())
            throw Exception("연결 실패")
        }
    }

    fun cancel() {
        try {
            connectSocket?.close()
        } catch (e: IOException) {
            setLog(TAG, e.message.toString())
        }
    }

    private inner class ConnectedThread(private val bluetoothSocket: BluetoothSocket?) : Thread() {
        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null

        init {
            try {
                // BluetoothSocket의 InputStream, OutputStream 초기화
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
            } catch (e: IOException) {
                setLog(TAG, e.message.toString())
            }
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int?

            while (true) {
                try {
                    // 데이터 받기(읽기)
                    bytes = inputStream?.read(buffer)
                    setLog(TAG, bytes.toString())
                } catch (e: Exception) { // 기기와의 연결이 끊기면 호출
                    setLog(TAG, "기기와의 연결이 끊겼습니다.")
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                // 데이터 전송
                outputStream?.write(bytes)
            } catch (e: IOException) {
                setLog(TAG, e.message.toString())
            }
        }

        fun cancel() {
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                setLog(TAG, e.message.toString())
            }
        }
    }
}