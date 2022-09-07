package com.example.bluetoothproject.binding

import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter

object ViewBinding {
    @JvmStatic
    @BindingAdapter("battery")
    fun AppCompatTextView.bindSetBattery(mode: Int, batteryValue: Int) {
        text = when(batteryValue) {
            0 -> "충전중.."
            else -> "현재 $batteryValue%"
        }
    }
    
    @JvmStatic
    @BindingAdapter("mode")
    fun AppCompatTextView.bindSetMode(mode: Int) {
        text = if(mode in 0..15) {
            when (mode) {
                0 -> "대기 모드"
                2 -> "충전 모드"
                else -> "스트림 모드"
            }
        } else {
            "비스트림 모드"
        }
    }
}