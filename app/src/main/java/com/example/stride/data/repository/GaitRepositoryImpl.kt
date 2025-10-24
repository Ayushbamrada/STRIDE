//package com.example.stride.data.repository
//
//import android.bluetooth.BluetoothDevice
//import android.content.Context
//import android.util.Log
//import com.example.stride.data.ble.BluetoothManager
//import com.example.stride.data.model.ConnectionState
//import com.example.stride.data.model.SensorDataDto
//import dagger.hilt.android.qualifiers.ApplicationContext
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class GaitRepositoryImpl @Inject constructor(
//    @ApplicationContext private val context: Context
//) : GaitRepository {
//
//    private var bluetoothManager: BluetoothManager? = null
//
//    // --- THIS IS THE CRITICAL FIX ---
//    // The repository now owns its own state flows. It is the single source of truth.
//    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
//    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
//
//    private val _sensorDataStream = MutableStateFlow<SensorDataDto?>(null)
//    override val sensorDataStream: StateFlow<SensorDataDto?> = _sensorDataStream.asStateFlow()
//
//    private val _ackMessage = MutableStateFlow<String?>(null)
//    override val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()
//
//    private val scope = CoroutineScope(Dispatchers.IO)
//    private var stateObservationJob: Job? = null
//
//    override fun connect(device: BluetoothDevice) {
//        if (bluetoothManager?.device?.address != device.address) {
//            Log.d("GaitRepository", "New device selected. Cleaning up old manager.")
//            bluetoothManager?.close()
//            bluetoothManager = null
//        }
//
//        if (bluetoothManager == null) {
//            Log.d("GaitRepository", "Creating new BluetoothManager for ${device.address}")
//            bluetoothManager = BluetoothManager(context, device)
//            observeBluetoothManagerState() // Start listening to the new manager
//        }
//
//        Log.d("GaitRepository", "Calling connect on manager.")
//        bluetoothManager?.connect()
//    }
//
//    // This new function acts as a relay from the manager to the repository's state
//    private fun observeBluetoothManagerState() {
//        stateObservationJob?.cancel() // Cancel any old observers
//        stateObservationJob = scope.launch {
//            bluetoothManager?.let { manager ->
//                // Relay connection state
//                launch {
//                    manager.connectionState.collect { state ->
//                        _connectionState.value = state
//                    }
//                }
//                // Relay sensor data
//                launch {
//                    manager.sensorData.collect { data ->
//                        _sensorDataStream.value = data
//                    }
//                }
//                // Relay ack messages
//                launch {
//                    manager.ackMessage.collect { msg ->
//                        _ackMessage.value = msg
//                    }
//                }
//            }
//        }
//    }
//
//    override fun disconnect() {
//        Log.d("GaitRepository", "Disconnect command received. Closing manager.")
//        stateObservationJob?.cancel()
//        bluetoothManager?.close()
//        bluetoothManager = null
//        // Manually reset state to ensure UI updates
//        _connectionState.value = ConnectionState.DISCONNECTED
//    }
//
//    override fun sendCommand(command: String) {
//        if (_connectionState.value == ConnectionState.CONNECTED) {
//            bluetoothManager?.writeCommand(command)
//        } else {
//            Log.e("GaitRepository", "Cannot send command '$command'. Not connected.")
//        }
//    }
//}
//
package com.example.stride.data.repository

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.example.stride.data.ble.BluetoothManager
import com.example.stride.data.model.ConnectionState
import com.example.stride.data.model.SensorDataDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GaitRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GaitRepository {

    private var bluetoothManager: BluetoothManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var stateObservationJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorDataStream = MutableStateFlow<SensorDataDto?>(null)
    override val sensorDataStream: StateFlow<SensorDataDto?> = _sensorDataStream.asStateFlow()

    private val _ackMessage = MutableStateFlow<String?>(null)
    override val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()

    // ------------------------------------------------------------------------
    // CONNECT
    // ------------------------------------------------------------------------
    override fun connect(device: BluetoothDevice) {
        Log.d("GaitRepository", "🔗 connect() called for device: ${device.address}")

        // Avoid reconnecting to same device unnecessarily
        if (bluetoothManager?.device?.address == device.address &&
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            Log.i("GaitRepository", "Already connected to this device: ${device.address}")
            return
        }

        // Clean up old manager if connecting to a new one
        if (bluetoothManager?.device?.address != device.address) {
            Log.w("GaitRepository", "Different device detected. Cleaning up old connection.")
            disconnect()
        }

        // Create new manager and connect
        bluetoothManager = BluetoothManager(context, device)
        observeBluetoothManagerState()
        bluetoothManager?.connect()
    }

    // ------------------------------------------------------------------------
    // STATE OBSERVATION
    // ------------------------------------------------------------------------
    private fun observeBluetoothManagerState() {
        Log.d("GaitRepository", "📡 Starting to observe BluetoothManager state...")

        stateObservationJob?.cancel()
        stateObservationJob = scope.launch {
            bluetoothManager?.let { manager ->
                launch {
                    manager.connectionState.collect { state ->
                        Log.d("GaitRepository", "📶 Connection state updated: $state")
                        _connectionState.value = state
                        if (state == ConnectionState.DISCONNECTED) {
                            Log.w("GaitRepository", "⚠️ BLE device disconnected. Cleaning up.")
                            disconnect()
                        }
                    }
                }

                launch {
                    manager.sensorData.collect { data ->
                        if (data != null) {
                            Log.v("GaitRepository", "📊 Sensor data update: $data")
                        }
                        _sensorDataStream.value = data
                    }
                }

                launch {
                    manager.ackMessage.collect { msg ->
                        if (!msg.isNullOrBlank()) {
                            Log.i("GaitRepository", "✅ ACK received: $msg")
                        }
                        _ackMessage.value = msg
                    }
                }
            } ?: Log.e("GaitRepository", "❌ BluetoothManager not initialized.")
        }
    }

    // ------------------------------------------------------------------------
    // DISCONNECT
    // ------------------------------------------------------------------------
    override fun disconnect() {
        Log.d("GaitRepository", "🔌 disconnect() called")

        try {
            stateObservationJob?.cancel()
            bluetoothManager?.close()
            bluetoothManager = null

            if (_connectionState.value != ConnectionState.DISCONNECTED) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            Log.i("GaitRepository", "✅ Repository fully cleaned up and disconnected.")
        } catch (e: Exception) {
            Log.e("GaitRepository", "❌ Error during disconnect()", e)
        }
    }

    // ------------------------------------------------------------------------
    // COMMAND
    // ------------------------------------------------------------------------
    override fun sendCommand(command: String) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            Log.d("GaitRepository", "📤 Sending command: '$command'")
            bluetoothManager?.writeCommand(command)
        } else {
            Log.e(
                "GaitRepository",
                "⚠️ Cannot send command '$command' — Not connected (state=${_connectionState.value})"
            )
        }
    }
}


