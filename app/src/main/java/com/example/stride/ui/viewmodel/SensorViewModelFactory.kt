package com.example.stride.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stride.bluetooth.BluetoothManager

class SensorViewModelFactory(
    private val bluetoothManager: BluetoothManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SensorViewModel(bluetoothManager) as T
    }
}
