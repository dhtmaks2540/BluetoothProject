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
    private val connectViewModel: ConnectViewModel
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
            if (connectedThread?.state == State.RUNNABLE) {
                connectedThread?.cancel()
            }

            // 연결 수행
            connectSocket?.connect()
            connectSocket?.let {
                connectedThread?.start()
            }
        } catch (e: IOException) { // 기기와의 연결이 실패할 경우 호출
            connectSocket?.close()
            useTimber(e.message.toString())
        }
    }

    fun cancel() {
        try {
            connectSocket?.close()
        } catch (e: IOException) {
            useTimber(e.message.toString())
        }
    }

    private inner class ConnectedThread(private val bluetoothSocket: BluetoothSocket?) : Thread() {
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        init {
            try {
                // BluetoothSocket의 InputStream, OutputStream 초기화
                inputStream = bluetoothSocket?.inputStream!!
                outputStream = bluetoothSocket.outputStream!!
            } catch (e: IOException) {
                setLog(TAG, e.message.toString())
            }
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun run() {
            var buffer = ByteArray(1024)
            var bytes: Int = 0

            while (true) {
                try {
                    // 데이터 받기(읽기)
                    bytes = inputStream.available()
                    if (bytes != 0) {
                        buffer = ByteArray(bytes)
                        bytes = inputStream.available()
                        bytes = inputStream.read(buffer, 0, bytes)
                        val uBuffer = buffer.toUByteArray()
                        if(uBuffer.first().toInt() != 255 || uBuffer.first().toInt() == 254) {
                            continue
                        } else {
//                            connectViewModel.setDataArray(uBuffer)
                            val sb = StringBuilder()
                            uBuffer.forEach { byte ->
                                sb.append("$byte ")
                            }
                            useTimber(sb.toString())
                        }
                    }
                } catch (e: Exception) { // 기기와의 연결이 끊기면 호출
                    useTimber(e.message.toString())
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                // 데이터 전송
                outputStream?.write(bytes)
            } catch (e: IOException) {
                useTimber(e.message.toString())
            }
        }

        fun cancel() {
            try {
                bluetoothSocket?.close()
            } catch (e: IOException) {
                useTimber(e.message.toString())
            }
        }
    }
}