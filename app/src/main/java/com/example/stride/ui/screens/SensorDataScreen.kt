package com.example.stride.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stride.bluetooth.BluetoothManager
import com.example.stride.ui.viewmodel.SensorViewModel
import com.example.stride.ui.viewmodel.SensorViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDataScreen(deviceAddress: String) {
    val device: BluetoothDevice = remember(deviceAddress) {
        BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
    }
    val context= LocalContext.current;
    val bluetoothManager = remember { BluetoothManager(context,device) }

    val viewModel: SensorViewModel = viewModel(
        factory = SensorViewModelFactory(bluetoothManager)
    )

    val sensorData by viewModel.sensorData.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Sensor Data") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
//          Stop Button
            Button(
                onClick ={
                    viewModel.stopReading()
                },modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)){
                Text("Stop")
            }
//          Start Button
            Button(
                onClick ={
                    viewModel.startReading()
                },modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)){
                Text("Start")
            }
            if (sensorData != null) {
                // ... [your existing UI] ...
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("ACC_Y: ${sensorData!!.accY}")
                    Text("ACC_Z: ${sensorData!!.accZ}")
                    Text("GYRO: ${sensorData!!.gyro}")
                    Text("ROLL: ${sensorData!!.roll}")
                }
            }else{
                Text("Waiting for Data.")
            }
        }
    }
}

