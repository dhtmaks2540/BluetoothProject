package com.example.bluetoothproject.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel

abstract class BaseActivity<VDB: ViewDataBinding, VM: ViewModel>(@LayoutRes val layoutRes: Int): AppCompatActivity() {
    protected lateinit var binding: VDB
    protected abstract val viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, layoutRes)

        binding.apply {
            lifecycleOwner = this@BaseActivity
        }
    }
}