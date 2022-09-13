package com.example.bluetoothproject.binding

import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import com.example.bluetoothproject.bluetooth.model.StreamChargeMode
import com.example.bluetoothproject.bluetooth.model.StreamMeasureMode
import com.example.bluetoothproject.bluetooth.model.StreamMode
import com.example.bluetoothproject.bluetooth.model.StreamWaitingMode
import com.example.bluetoothproject.di.BluetoothMode
import com.example.bluetoothproject.di.BluetoothModeProvider

object ViewBinding {
    @JvmStatic
    @BindingAdapter("mode")
    fun AppCompatTextView.bindSetMode(mode: BluetoothMode?) {
        text = when (mode) {
            BluetoothMode.CHARGE_MODE -> "충전 모드"
            BluetoothMode.WAITING_MODE -> "대기 모드"
            BluetoothMode.ACTIVATE_MODE -> "활성 모드"
            else -> "연결 안됨"
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "battery")
    fun AppCompatTextView.bindSetBattery(mode: BluetoothMode?, data: StreamMode?) {
        when (data) {
            is StreamChargeMode -> text = if (data.chargeState == 0) {
                "충전 중"
            } else {
                "충전 완료"
            }
            is StreamWaitingMode -> text = "${data.batteryValue}%"
            is StreamMeasureMode -> if (data.packetCount == 1) {
                text = "${data.pcd}%"
            }
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "bpm")
    fun AppCompatTextView.setBpm(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            if (data is StreamMeasureMode) {
                text = data.bpm.toString()
            }
        } else {
            text = "0"
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "ch1")
    fun AppCompatTextView.bindSetCh1(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            text = if (data is StreamMeasureMode) {
                if (data.ch1) {
                    "true"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "ch2")
    fun AppCompatTextView.bindSetCh2(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            text = if (data is StreamMeasureMode) {
                if (data.ch2) {
                    "true"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "ref")
    fun AppCompatTextView.bindSetRef(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            text = if (data is StreamMeasureMode) {
                if (data.ref) {
                    "true"
                } else {
                    "false"
                }
            } else {
                "false"
            }
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "left_eeg")
    fun AppCompatTextView.bindSetLeftEEG(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            if (data is StreamMeasureMode) {
                text = data.leftEEG.toString()
            }
        } else {
            text = "0"
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "right_eeg")
    fun AppCompatTextView.bindSetRightEEG(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            if (data is StreamMeasureMode) {
                text = data.rightEEG.toString()
            }
        } else {
            text = "0"
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "ppg")
    fun AppCompatTextView.bindSetPPG(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            if (data is StreamMeasureMode) {
                text = data.ppg.toString()
            }
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "sd_ppg")
    fun AppCompatTextView.bindSetSdPPG(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            if (data is StreamMeasureMode) {
                text = data.sdPPG.toString()
            }
        } else {
            text = "0"
        }
    }

    @JvmStatic
    @BindingAdapter("bluetooth_mode", "rr_interval")
    fun AppCompatTextView.bindSetRrInterval(mode: BluetoothMode?, data: StreamMode?) {
        if (mode == BluetoothMode.ACTIVATE_MODE) {
            if (data is StreamMeasureMode) {
                text = data.rrInterval.toString()
            }
        } else {
            text = "0"
        }
    }
}