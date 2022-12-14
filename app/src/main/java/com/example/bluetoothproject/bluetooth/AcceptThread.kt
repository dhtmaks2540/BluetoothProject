package com.example.bluetoothproject.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import com.example.bluetoothproject.useTimber
import java.io.IOException
import java.util.*

// 블루투스에서 서버의 역할을 수행하는 스레드
class AcceptThread(private val bluetoothAdapter: BluetoothAdapter): Thread() {
    private lateinit var serverSocket: BluetoothServerSocket

    companion object {
        private const val TAG = "ACCEPT_THREAD"
        private const val SOCKET_NAME = "server"
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }

    init {
        try {
            // 서버 소켓
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SOCKET_NAME, MY_UUID)
        } catch(e: Exception) {
            useTimber(e.message.toString())
        }
    }

    override fun run() {
        var socket: BluetoothSocket? = null
        while(true) {
            try {
                // 클라이언트 소켓
                socket = serverSocket.accept()
            } catch (e: IOException) {
                useTimber(e.message.toString())
            }

            socket?.let {
                /* 클라이언트 소켓과 관련된 작업..... */

                // 더 이상 연결을 수행하지 않는다면 서버 소켓 종료(그래도 연결된 소켓은 작동)
                serverSocket.close()
            }
            break
        }
    }

    fun cancel() {
        try {
            serverSocket.close()
        } catch (e: IOException) {
            useTimber(e.message.toString())
        }
    }
}