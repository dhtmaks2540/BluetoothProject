package com.example.bleapp

import android.bluetooth.BluetoothGattCharacteristic
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bleapp.ble.printProperties
import com.example.bleapp.databinding.ItemCharacteristicBinding

class CharacteristicAdapter(
    private val characteristicOnClicked: (BluetoothGattCharacteristic) -> Unit
) : ListAdapter<BluetoothGattCharacteristic, CharacteristicAdapter.ViewHolder>(difCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCharacteristicBinding.inflate(inflater, parent, false)

        return ViewHolder(binding).apply {
            binding.layoutRoot.setOnClickListener {
                val position = adapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?: return@setOnClickListener

                characteristicOnClicked(getItem(position))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
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

    inner class ViewHolder(private val binding: ItemCharacteristicBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(characteristic: BluetoothGattCharacteristic) {
            binding.apply {
                item = characteristic
                tvState.text = characteristic.printProperties()
            }
        }
    }
}