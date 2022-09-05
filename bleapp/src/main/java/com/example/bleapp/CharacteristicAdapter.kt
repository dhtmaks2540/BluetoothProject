package com.example.bleapp

import android.bluetooth.BluetoothGattCharacteristic
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bleapp.databinding.ItemCharacteristicBinding

class CharacteristicAdapter :
    ListAdapter<BluetoothGattCharacteristic, CharacteristicAdapter.ViewHolder>(difCallback) {
    inner class ViewHolder(binding: ItemCharacteristicBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    companion object {
        private val difCallback = object : DiffUtil.ItemCallback<BluetoothGattCharacteristic>() {
            override fun areItemsTheSame(
                oldItem: BluetoothGattCharacteristic,
                newItem: BluetoothGattCharacteristic
            ): Boolean = oldItem.uuid == newItem.uuid

            override fun areContentsTheSame(
                oldItem: BluetoothGattCharacteristic,
                newItem: BluetoothGattCharacteristic
            ): Boolean = oldItem.equals(newItem)

        }
    }
}