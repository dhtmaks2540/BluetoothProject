package com.example.bluetoothproject.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.example.bluetoothproject.ConnectViewModel
import com.example.bluetoothproject.useTimber
import java.io.*
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
            cancel()
            connectViewModel.setConnected(false)
//            useTimber("Connect Thread Exception")
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
                useTimber(e.message.toString())
            }
        }

        @OptIn(ExperimentalUnsignedTypes::class)
        override fun run() {
            connectViewModel.setConnected(true)

            while (true) {
                try {
                    val byteArr = ByteArray(20)
                    val readByteCnt = inputStream.read(byteArr)
                    val uBuffer = byteArr.toUByteArray()
                    if (uBuffer[0].toInt() != 255 || uBuffer[1].toInt() != 254) {
                        continue
                    } else {
                        val sb = StringBuilder()
                        uBuffer.forEach { byte ->
                            sb.append("$byte ")
                        }
                        val data = sb.toString().trim()

                        val ch1 = getBit(uBuffer[7].toInt(), 5)
                        val ch2 = getBit(uBuffer[7].toInt(), 4)
                        val ref = getBit(uBuffer[7].toInt(), 3)
                        useTimber(data)
                        useTimber(" 왼쪽이마: ${if(ch1 == 0) "미부착" else "부착"}, 오른쪽이마: ${if(ch2 == 0) "미부착" else "부착"}, 귓볼: ${if(ref == 0) "미부착" else "부착"}")
                        connectViewModel.setDataString(data)
                    }
                } catch (e: Exception) { // 기기와의 연결이 끊기면 호출
                    useTimber("Connected Thread Exception")
                    connectViewModel.setConnected(false)
                    cancel()
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                // 데이터 전송
                outputStream.write(bytes)
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