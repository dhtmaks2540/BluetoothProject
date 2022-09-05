package com.example.bleapp.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
    return properties and property != 0
}

fun BluetoothGattCharacteristic.isReadable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.printProperties(): String = mutableListOf<String>().apply {
    if(isReadable()) add("READABLE")
    if(isWritable()) add("WRITABLE")
    if(isWritableWithoutResponse()) add("WRITABLE WITHOUT RESPONSE")
    if(isIndicatable()) add("INDICATABLE")
    if(isNotifiable()) add("NOTIFIABLE")
    if(isEmpty()) add("EMPTY")
}.joinToString()

fun BluetoothGattDescriptor.isReadable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_READ) ||
            containsPermission(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED) ||
            containsPermission(BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM)

fun BluetoothGattDescriptor.isWritable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE) ||
            containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED) ||
            containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM) ||
            containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED) ||
            containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM)

fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
    permissions and permission != 0

fun BluetoothGattDescriptor.printProperties(): String = mutableListOf<String>().apply {
    if(isReadable()) add("READABLE")
    if(isWritable()) add("WRITABLE")
    if(isEmpty()) add("EMPTY")
}.joinToString()

fun ByteArray.toHexString(): String =
    joinToString(separator = " ", prefix = "0x") { String.format("%02X", it) }