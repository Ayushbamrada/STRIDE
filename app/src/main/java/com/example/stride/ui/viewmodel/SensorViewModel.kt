package com.example.stride.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stride.bluetooth.BluetoothManager
import com.example.stride.data.model.SensorData
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SensorViewModel(
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    val sensorData: StateFlow<SensorData?> = bluetoothManager.sensorData

    fun startListening() {
        viewModelScope.launch {
            bluetoothManager.connect()
//            val connected = bluetoothManager.connect()
//            if (connected) {
//                bluetoothManager.startReading()
//            } else {
//                Log.d("SensorViewModel","Not Device Connected.")
//            }
        }
    }

    fun startReading(){
        viewModelScope.launch {
            bluetoothManager.startReading()
        }
    }

    fun stopReading(){
        viewModelScope.launch {
            bluetoothManager.startReading()
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.stop()
    }
}

