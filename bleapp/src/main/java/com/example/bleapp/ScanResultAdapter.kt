package com.example.bleapp

import android.bluetooth.le.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bleapp.databinding.ItemScanResultBinding

class ScanResultAdapter(
    private val scanResultClicked: (ScanResult) -> Unit
): ListAdapter<ScanResult, ScanResultAdapter.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemScanResultBinding.inflate(layoutInflater, parent, false)

        return ViewHolder(binding).apply {
            binding.layoutRoot.setOnClickListener {
                val position = adapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener

                scanResultClicked(
                    getItem(position)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemScanResultBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(scanResult: ScanResult) {
            binding.apply {
                item = scanResult
            }
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ScanResult>() {
            override fun areItemsTheSame(
                oldItem: ScanResult,
                newItem: ScanResult
            ): Boolean = oldItem.device.address == newItem.device.address

            override fun areContentsTheSame(
                oldItem: ScanResult,
                newItem: ScanResult
            ): Boolean = oldItem == newItem

        }
    }
}