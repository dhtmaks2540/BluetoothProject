package com.example.bluetoothproject.di

import com.example.bluetoothproject.bluetooth.BluetoothMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModeModule {
    @Provides
    @Singleton
    fun provideBluetoothMode() = BluetoothMode.NOT_CONNECTED_MODE
}