package com.crescenzi.volkswagen_launcher.core

import android.bluetooth.BluetoothManager
import android.content.Context

// Reading the adapter's on/off state needs no runtime permission. A missing
// adapter (no Bluetooth hardware) reads as off.
fun Context.isBluetoothOn(): Boolean {
    val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
    return manager.adapter?.isEnabled == true
}
