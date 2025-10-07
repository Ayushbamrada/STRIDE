package com.example.stride.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stride.bluetooth.BluetoothManager
import com.example.stride.bluetooth.ConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// We need SuppressLint for BLUETOOTH_CONNECT permission, which is handled in the UI
@SuppressLint("MissingPermission")
class SensorViewModel(
    private val context: Context // Takes Context instead of BluetoothManager
) : ViewModel() {

    // The ViewModel now manages the BluetoothManager instance internally. It's nullable.
    private var bluetoothManager: BluetoothManager? = null

    // The ViewModel now owns its state. This is crucial.
    // It starts as Disconnected and is updated based on the manager's state.
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // We'll use these to mirror the manager's data when connected
    private val _data = MutableStateFlow("")
    val data: StateFlow<String> = _data.asStateFlow()

    private val _dataList = MutableStateFlow<List<String>>(emptyList())
    val dataList: StateFlow<List<String>> = _dataList.asStateFlow()

    private val _ackMessage = MutableStateFlow<String?>(null)
    val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()

    // This job will hold our data collection coroutines, so we can cancel them on disconnect
    private var dataCollectionJob: Job? = null

    // --- The rest of your state flows for sliders remain unchanged ---
    private val _commandText = MutableStateFlow("")
    val commandText: StateFlow<String> = _commandText.asStateFlow()

    private val _vibTime = MutableStateFlow(1.0f)
    val vibTime:StateFlow<Float> = _vibTime.asStateFlow()

    private val _vibTimeGap = MutableStateFlow(1.0f)
    val vibTimeGap:StateFlow<Float> = _vibTimeGap.asStateFlow()

    private val _vibCount = MutableStateFlow(3.0f)
    val vibCount:StateFlow<Float> = _vibCount.asStateFlow()

    private val _delayToStartVib = MutableStateFlow(4.0f)
    val delayToStartVib:StateFlow<Float> = _delayToStartVib.asStateFlow()

    // --- State update functions are also unchanged ---
    fun onCommandTextChanged(newText: String) { _commandText.value = newText }
    fun onVibTimeChanged(newValue: Float){ _vibTime.value = newValue }
    fun onVibTimeGapChanged(newValue: Float){ _vibTimeGap.value = newValue }
    fun onVibCountChanged(newValue: Float){ _vibCount.value = newValue }
    fun onVibDelayChanged(newValue: Float){ _delayToStartVib.value = newValue }

    /**
     * NEW: Connects to a specific Bluetooth device.
     * This is the core of the new logic.
     */
    fun connect(device: BluetoothDevice) {
        // If already connected, disconnect first
        if (_connectionState.value != ConnectionState.Disconnected) {
            disconnect()
        }

        // Create a new manager for the selected device and update state
        bluetoothManager = BluetoothManager(context, device)
        _connectionState.value = ConnectionState.Connecting

        // Launch a new job to collect all data from the manager
        dataCollectionJob = viewModelScope.launch {
            // Collect connection state
            launch {
                bluetoothManager?.connectionState?.collect { state ->
                    _connectionState.value = state
                }
            }
            // Collect single data string
            launch {
                bluetoothManager?.data?.collect { _data.value = it }
            }

            launch {
                bluetoothManager?.ackMessage?.collect { msg ->
                    msg?.let {
                        when {
                            it.startsWith("cal")  -> _ackMessage.value = "Calibration Started."
                            it.startsWith("vibtime") -> _ackMessage.value = "Vibration Time has been set."
                            it.startsWith("vibtimegap") -> _ackMessage.value = "Vibration Time Gap has been set."
                            it.startsWith("vibcount") -> _ackMessage.value = "Vibration Count has been set."
                            it.startsWith("maxdelay") -> _ackMessage.value = "Vibration Delay has been set."
                        }
                    }
                }
            }
            // Collect list of data
            launch {
                bluetoothManager?.dataList?.collect { _dataList.value = it }
            }
        }

        // Start the connection attempt
        viewModelScope.launch {
            bluetoothManager?.connect()
        }
    }

    /**
     * UPDATED: Properly disconnects and cleans up resources.
     */
    fun disconnect() {
        bluetoothManager?.stop() // Use stop() for full cleanup
        bluetoothManager = null
        dataCollectionJob?.cancel() // Stop collecting data
        _connectionState.value = ConnectionState.Disconnected
        _dataList.value = emptyList() // Clear data list on disconnect
    }

    fun clearData(){
        bluetoothManager?.clearDataList()
    }

    fun calibrate(){
        bluetoothManager?.writeCommand("cal,1")
    }

    fun sendCommand() {
        // Only send if connected and text is not empty
        if (_connectionState.value == ConnectionState.Connected && commandText.value.isNotEmpty()) {
            bluetoothManager?.writeCommand(commandText.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect() // Ensure cleanup when ViewModel is destroyed
    }
}