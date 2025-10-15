//package com.example.stride.ui.features.vibration_config
//
//import android.annotation.SuppressLint
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.stride.data.ble.ConnectionState
//import com.example.stride.data.repository.GaitRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//data class VibrationConfigUiState(
//    val connectionState: ConnectionState = ConnectionState.Disconnected,
//    val pairedDevices: List<BluetoothDevice> = emptyList(),
//    val vibTime: Float = 1.0f,
//    val vibTimeGap: Float = 1.0f,
//    val vibCount: Float = 3.0f,
//    val delayToStartVib: Float = 4.0f,
//    val snackbarMessage: String? = null
//)
//
//@SuppressLint("MissingPermission")
//@HiltViewModel
//class VibrationConfigViewModel @Inject constructor(
//    private val repository: GaitRepository,
//    private val bluetoothAdapter: BluetoothAdapter? // Hilt can provide this
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(VibrationConfigUiState())
//    val uiState = _uiState.asStateFlow()
//
//    init {
//        viewModelScope.launch {
//            repository.connectionState.collect { state ->
//                _uiState.update { it.copy(connectionState = state) }
//            }
//        }
//        viewModelScope.launch {
//            repository.ackMessageStream.collect { msg ->
//                handleAckMessage(msg)
//            }
//        }
//    }
//
//    fun onConnect(device: BluetoothDevice) {
//        repository.connect(device)
//    }
//
//    fun onDisconnect() {
//        repository.disconnect()
//    }
//
//    fun onCalibrate() {
//        repository.sendCommand("cal,1")
//    }
//
//    fun updatePairedDevices() {
//        _uiState.update {
//            it.copy(pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList())
//        }
//    }
//
//    // --- State update functions for sliders ---
//    fun onVibTimeChanged(newValue: Float) { _uiState.update { it.copy(vibTime = newValue) } }
//    fun onVibTimeGapChanged(newValue: Float) { _uiState.update { it.copy(vibTimeGap = newValue) } }
//    fun onVibCountChanged(newValue: Float) { _uiState.update { it.copy(vibCount = newValue) } }
//    fun onVibDelayChanged(newValue: Float) { _uiState.update { it.copy(delayToStartVib = newValue) } }
//
//    fun onSaveParameter(index: Int, value: Float) {
//        val (paramName, formattedValue) = when (index) {
//            0 -> "vibtime" to (value * 1000).toInt()
//            1 -> "vibtimegap" to (value * 1000).toInt()
//            2 -> "vibcount" to value.toInt()
//            else -> "maxdelay" to (value * 1000).toInt()
//        }
//        val command = "$paramName,$formattedValue"
//        repository.sendCommand(command)
//    }
//
//    fun snackbarMessageShown() {
//        _uiState.update { it.copy(snackbarMessage = null) }
//    }
//
//    private fun handleAckMessage(msg: String?) {
//        val snackbarMsg = when {
//            msg?.startsWith("cal") == true -> "Calibration Started."
//            msg?.startsWith("vibtime") == true -> "Vibration Time has been set."
//            msg?.startsWith("vibtimegap") == true -> "Vibration Time Gap has been set."
//            msg?.startsWith("vibcount") == true -> "Vibration Count has been set."
//            msg?.startsWith("maxdelay") == true -> "Vibration Delay has been set."
//            else -> null
//        }
//        snackbarMsg?.let {
//            _uiState.update { it.copy(snackbarMessage = snackbarMsg) }
//        }
//    }
//}

package com.example.stride.ui.features.vibration_config

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stride.data.model.ConnectionState
import com.example.stride.data.repository.GaitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VibrationConfigUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val snackbarMessage: String? = null,
    val vibTime: Float = 1.0f,
    val vibTimeGap: Float = 1.0f,
    val vibCount: Float = 3.0f,
    val delayToStartVib: Float = 4.0f
)

@SuppressLint("MissingPermission")
@HiltViewModel
class VibrationConfigViewModel @Inject constructor(
    private val repository: GaitRepository,
    private val bluetoothAdapter: BluetoothAdapter?
) : ViewModel() {

    private val _uiState = MutableStateFlow(VibrationConfigUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Observe connection state from the repository
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                Log.d("VibConfigVM",state.toString())
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // Observe acknowledgement messages from the repository
        viewModelScope.launch {
            repository.ackMessage.collect { message ->
                message?.let {
                    _uiState.update { it.copy(snackbarMessage = "Device: $message") }
                }
            }
        }
    }

    fun updatePairedDevices() {
        if (bluetoothAdapter?.isEnabled == true) {
            _uiState.update { it.copy(pairedDevices = bluetoothAdapter.bondedDevices.toList()) }
        }
    }

    fun connect(device: BluetoothDevice) {
        repository.connect(device)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun onVibTimeChanged(newValue: Float) { _uiState.update { it.copy(vibTime = newValue) } }
    fun onVibTimeGapChanged(newValue: Float) { _uiState.update { it.copy(vibTimeGap = newValue) } }
    fun onVibCountChanged(newValue: Float) { _uiState.update { it.copy(vibCount = newValue) } }
    fun onVibDelayChanged(newValue: Float) { _uiState.update { it.copy(delayToStartVib = newValue) } }

    fun onSaveParameter(index: Int, value: Float) {
        val (paramName, formattedValue) = when (index) {
            0 -> "vibtime" to "%.1f".format(value)
            1 -> "vibtimegap" to "%.1f".format(value)
            2 -> "vibcount" to "%.0f".format(value)
            else -> "maxdelay" to "%.1f".format(value)
        }
        val command = if (index == 2) "$paramName,${value.toInt()}" else "$paramName,${(value * 1000).toInt()}"
        repository.sendCommand(command)
    }


    fun calibrate() {
        repository.sendCommand("cal,1")
        Log.d("VibViewModel","Sending command")
    }

    fun snackbarMessageShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
