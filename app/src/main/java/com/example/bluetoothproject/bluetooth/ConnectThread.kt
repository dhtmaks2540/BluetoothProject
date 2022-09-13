package com.example.bluetoothproject.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Message
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamMeasureMode
import com.example.bluetoothproject.bluetooth.model.StreamMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.di.BluetoothMode
import com.example.bluetoothproject.di.BluetoothModeProvider
import com.example.bluetoothproject.useTimber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

@SuppressLint("MissingPermission")
class ConnectThread(
    private val myUUID: UUID,
    private val device: BluetoothDevice,
    private val connectedHandler: ConnectedHandler,
    private val bluetoothModeProvider: BluetoothModeProvider
) : Thread() {
    private lateinit var message: Message
    private val bundle = Bundle()

    // BluetoothDevice 로부터 BluetoothSocket 획득
    private val connectSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(myUUID)
    }

    override fun run() {
        try {
            val connectedThread = ConnectedThread(connectedHandler, connectSocket, bluetoothModeProvider)

            // 연결 수행
            connectSocket?.connect()
            connectSocket?.let {
                connectedThread.start()
            }
        } catch (e: IOException) { // 기기와의 연결이 실패할 경우 호출
            cancel()

            bundle.putBoolean(CONNECT_FLAG, false)
            bluetoothModeProvider.bluetoothMode = BluetoothMode.NOT_CONNECTED_MODE
            message = connectedHandler.obtainMessage()
            message.what = CONNECT_TYPE
            message.data = bundle
            connectedHandler.sendMessage(message)

            useTimber("Connect Thread Exception")
        }
    }

    fun cancel() {
        try {
            connectSocket?.close()
        } catch (e: IOException) {
            useTimber(e.message.toString())
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private class ConnectedThread(
        private val connectedHandler: ConnectedHandler,
        private val connectSocket: BluetoothSocket?,
        private val bluetoothModeProvider: BluetoothModeProvider,
    ) : Thread() {
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream
        private val bundle = Bundle()
        private lateinit var message: Message

        init {
            try {
                // BluetoothSocket의 InputStream, OutputStream 초기화
                inputStream = connectSocket?.inputStream!!
                outputStream = connectSocket.outputStream!!
            } catch (e: IOException) {
                useTimber(e.message.toString())
            }
        }

        override fun run() {
            bundle.putBoolean(CONNECT_FLAG, true)
            message = connectedHandler.obtainMessage()
            message.what = CONNECT_TYPE
            message.data = bundle
            connectedHandler.sendMessage(message)

            while (true) {
                try {
                    val byteArr = ByteArray(20)
                    inputStream.read(byteArr)
                    val uBuffer = byteArr.toUByteArray()
                    if (uBuffer[0].toInt() != 255 || uBuffer[1].toInt() != 254) {
                        continue
                    } else {
                        message = connectedHandler.obtainMessage()
                        // 모드 확인
                        val modeValue = uBuffer[2].toInt()
                        // 스트림
                        if (modeValue in 0..15) {
                            message.what = MODE_DATA
                            when (modeValue) {
                                // 대기
                                0 -> {
                                    bluetoothModeProvider.bluetoothMode = BluetoothMode.WAITING_MODE
                                    message.obj = StreamWaitingMode(uBuffer)
                                }
                                // 충전
                                2 -> {
                                    bluetoothModeProvider.bluetoothMode = BluetoothMode.CHARGE_MODE
                                    message.obj = StreamChargeMode(uBuffer)
                                }
                                // 활성화
                                else -> {
                                    bluetoothModeProvider.bluetoothMode = BluetoothMode.ACTIVATE_MODE
                                    message.obj = StreamMeasureMode(uBuffer)

                                }
                            }
                            connectedHandler.sendMessage(message)
                        }
                    }
                } catch (e: Exception) { // 기기와의 연결이 끊기면 호출
                    bundle.putBoolean(CONNECT_FLAG, false)
                    bluetoothModeProvider.bluetoothMode = BluetoothMode.NOT_CONNECTED_MODE
                    message = connectedHandler.obtainMessage()
                    message.what = CONNECT_TYPE
                    message.data = bundle
                    connectedHandler.sendMessage(message)
                    cancel()
                    useTimber("Connected Thread Exception")
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
                connectSocket?.close()
            } catch (e: IOException) {
                useTimber(e.message.toString())
            }
        }
    }
}