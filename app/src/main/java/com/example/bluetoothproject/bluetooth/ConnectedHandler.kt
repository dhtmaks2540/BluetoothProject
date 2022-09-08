package com.example.bluetoothproject.bluetooth

import android.os.Handler
import android.os.Message
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.useTimber
import javax.inject.Inject

class ConnectedHandler @Inject constructor(
) : Handler() {
    lateinit var handlerMessageListener: HandlerMessageListener

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        when (msg.what) {
            CONNECT_TYPE -> {
                val bundle = msg.data
                val connectedFlag = bundle.getBoolean(CONNECT_FLAG)
                handlerMessageListener.getFlagMessage(connectedFlag)
            }
            MODE_DATA -> {
                val modeData = msg.obj
                handlerMessageListener.getModeData(modeData)
            }
        }
    }

    fun setListener(handlerMessageListener: HandlerMessageListener) {
        this.handlerMessageListener = handlerMessageListener
    }
}