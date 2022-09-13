package com.example.bluetoothproject.bluetooth

import android.os.Handler
import android.os.Message
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamMeasureMode
import com.example.bluetoothproject.bluetooth.model.StreamMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.useTimber
import javax.inject.Inject

class ConnectedHandler @Inject constructor(
) : Handler() {
    var handlerMessageListener: HandlerMessageListener? = null

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)

        when (msg.what) {
            CONNECT_TYPE -> {
                val bundle = msg.data
                val connectedFlag = bundle.getBoolean(CONNECT_FLAG)
                handlerMessageListener?.getFlagMessage(connectedFlag)
            }
            MODE_DATA -> {
                when(val modeData = msg.obj) {
                    is StreamChargeMode -> handlerMessageListener?.getModeData(modeData)
                    is StreamMeasureMode -> handlerMessageListener?.getModeData(modeData)
                    is StreamWaitingMode -> handlerMessageListener?.getModeData(modeData)
                }
            }
        }
    }

    fun registerListener(handlerMessageListener: HandlerMessageListener) {
        this.handlerMessageListener = handlerMessageListener
    }

    fun unRegisterListener() {
        this.handlerMessageListener = null
    }
}