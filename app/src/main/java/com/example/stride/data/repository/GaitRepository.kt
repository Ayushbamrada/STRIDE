package com.example.stride.data.repository

import android.bluetooth.BluetoothDevice
import com.example.stride.data.model.ConnectionState
import com.example.stride.data.model.SensorDataDto
import kotlinx.coroutines.flow.StateFlow

interface GaitRepository {
    val connectionState: StateFlow<ConnectionState>
    val sensorDataStream: StateFlow<SensorDataDto?>
    val ackMessage: StateFlow<String?>

    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun sendCommand(command: String)
}

