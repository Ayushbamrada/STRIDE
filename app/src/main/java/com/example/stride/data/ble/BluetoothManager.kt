//package com.example.stride.data.ble
//
//import android.annotation.SuppressLint
//import android.bluetooth.*
//import android.content.Context
//import android.util.Log
//import com.example.stride.data.model.ConnectionState
//import com.example.stride.data.model.SensorDataDto
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import java.util.*
//
//@SuppressLint("MissingPermission")
//class BluetoothManager(
//    private val context: Context,
//    val device: BluetoothDevice
//) {
//    private var bluetoothGatt: BluetoothGatt? = null
//    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
//    private var writeCharacteristic: BluetoothGattCharacteristic? = null
//
//    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
//    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
//
//    private val _sensorData = MutableStateFlow<SensorDataDto?>(null)
//    val sensorData: StateFlow<SensorDataDto?> = _sensorData.asStateFlow()
//
//    private val _ackMessage = MutableStateFlow<String?>(null)
//    val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()
//
//    fun connect() {
//        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
//            return
//        }
//        _connectionState.value = ConnectionState.CONNECTING
//        bluetoothGatt = device.connectGatt(context, false, gattCallback)
//    }
//
//    fun close() {
//        if (bluetoothGatt != null) {
//            if (_connectionState.value == ConnectionState.CONNECTED) {
//                writeCommand("game,0") // Use lowercase command
//            }
//            bluetoothGatt?.disconnect()
//            bluetoothGatt?.close()
//            bluetoothGatt = null
//            _connectionState.value = ConnectionState.DISCONNECTED
//            Log.d("BLEManager", "GATT connection closed and resources released.")
//        }
//    }
//    private val gattCallback = object : BluetoothGattCallback() {
//        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
//                    _connectionState.value = ConnectionState.CONNECTED
//                    Log.d("BLEManager", "Successfully connected to ${device.address}")
//                    Thread.sleep(500)
//                    gatt.discoverServices()
//                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    _connectionState.value = ConnectionState.DISCONNECTED
//                    Log.d("BLEManager", "Device disconnected.")
//                }
//            } else {
//                Log.e("BLEManager", "Connection state change error with status: $status")
//                _connectionState.value = ConnectionState.DISCONNECTED
//            }
//        }
//
//        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d("BLEManager", "Services discovered for ${device.address}")
//                var foundNotify = false
//                var foundWrite = false
//
//                gatt.services.forEach { service ->
//                    service.characteristics.forEach { characteristic ->
//                        val canNotify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
//                        val canWrite = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
//
//                        if (canNotify && canWrite) {
//                            Log.d("BLEManager", "Found ideal characteristic (Notify & Write): ${characteristic.uuid}")
//                            notifyCharacteristic = characteristic
//                            writeCharacteristic = characteristic
//                            foundNotify = true
//                            foundWrite = true
//                            return@forEach
//                        }
//                    }
//                    if(foundNotify) return@forEach
//                }
//
//                if (!foundNotify || !foundWrite) {
//                    gatt.services.forEach { service ->
//                        service.characteristics.forEach { characteristic ->
//                            if (!foundNotify && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
//                                notifyCharacteristic = characteristic
//                                foundNotify = true
//                            }
//                            if (!foundWrite && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
//                                writeCharacteristic = characteristic
//                                foundWrite = true
//                            }
//                        }
//                    }
//                }
//
//                if (foundNotify) {
//                    enableNotifications(gatt, notifyCharacteristic!!)
//                }
//            }
//        }
//
//        private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
//            gatt.setCharacteristicNotification(characteristic, true)
//            val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//            val descriptor = characteristic.getDescriptor(cccdUuid)
//            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//            gatt.writeDescriptor(descriptor)
//        }
//
//        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
//            val dataBytes = characteristic.value ?: return
//            val receivedString = String(dataBytes, Charsets.UTF_8).trim()
//            Log.d("LiveData",receivedString);
//
//            // --- THIS IS THE "TOUGH AS NAILS" PARSER ---
//            try {
//                val parts = receivedString.split(',')
//                val identifier = parts.getOrNull(0) ?: ""
//
//                // --- FIX 1: IGNORE CORRUPTED PACKETS ---
//                // If the identifier isn't 'm' or 's' (or contains 'master'/'slave'), it's garbage.
//                if (!identifier.contains("m") && !identifier.contains("s")) {
//                    Log.w("BLEManager", "Ignoring corrupted or status packet: $receivedString")
//                    return // Immediately stop processing this message
//                }
//
//                val finalIdentifier = if (identifier.contains("m")) "master" else "slave"
//
//                // --- FIX 2: PARSE & SCALE ROBUSTLY ---
//                // Divide by 100 as per engineer's instructions
//                val roll = (parts.getOrNull(1)?.toFloatOrNull() ?: 0f) / 100.0f
//                val pitch = (parts.getOrNull(2)?.toFloatOrNull() ?: 0f) / 100.0f
//                val yaw = (parts.getOrNull(3)?.toFloatOrNull() ?: 0f) / 100.0f
//                val acceleration = (parts.getOrNull(4)?.toFloatOrNull() ?: 0f) // Accel is not scaled
//
//                _sensorData.value = SensorDataDto(
//                    identifier = finalIdentifier,
//                    roll = roll,
//                    pitch = pitch,
//                    yaw = yaw,
//                    acceleration = acceleration
//                )
//
//            } catch (e: Exception) {
//                Log.e("BLEManager", "Could not parse incoming data: '$receivedString'", e)
//            }
//        }
//
//        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {}
//    }
//
//    fun writeCommand(command: String) {
//        if (writeCharacteristic == null) {
//            Log.e("BLEManager", "Cannot send command. Write characteristic is not available.")
//            return
//        }
//        writeCharacteristic?.let { char ->
//            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//            char.value = (command + "\n").toByteArray(Charsets.UTF_8)
//            bluetoothGatt?.writeCharacteristic(char)
//            Log.d("BLEManager", "➡️ Sending: '$command'")
//        }
//    }
//}
//
package com.example.stride.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.example.stride.data.model.ConnectionState
import com.example.stride.data.model.SensorDataDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothManager(
    private val context: Context,
    val device: BluetoothDevice
) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
//    private val buffer = StringBuilder()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorDataDto?>(null)
    val sensorData: StateFlow<SensorDataDto?> = _sensorData.asStateFlow()

    private val _ackMessage = MutableStateFlow<String?>(null)
    val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()

    // ------------------------- CONNECTION -------------------------
    fun connect() {
        Log.d("BLEManager", "Attempting to connect to device: ${device.address}")
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            Log.w("BLEManager", "Connection attempt ignored; already connecting/connected.")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        Log.d("BLEManager", "GATT connection initiated.")
    }

    fun close() {
        Log.d("BLEManager", "Closing GATT connection...")
        try {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                writeCommand("game,0")
            }
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            notifyCharacteristic = null
            writeCharacteristic = null
            _connectionState.value = ConnectionState.DISCONNECTED
//            buffer.clear()
            Log.i("BLEManager", "✅ GATT connection fully closed and resources released.")
        } catch (e: Exception) {
            Log.e("BLEManager", "❌ Error while closing GATT connection", e)
        }
    }

    // ------------------------- GATT CALLBACK -------------------------
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d("BLEManager", "onConnectionStateChange: status=$status, newState=$newState")

            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLEManager", "✅ Connected to ${device.address}")
                _connectionState.value = ConnectionState.CONNECTED

                Thread.sleep(500)
                Log.d("BLEManager", "Discovering services...")
                gatt.discoverServices()

            } else {
                Log.e("BLEManager", "⚠️ Disconnected from GATT or connection failed. Status=$status, newState=$newState")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("BLEManager", "onServicesDiscovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BLEManager", "✅ Services discovered (${gatt.services.size})")
                gatt.services.forEach { service ->
                    Log.d("BLEManager", "Service: ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        val canNotify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                        val canWrite = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0
                        Log.d(
                            "BLEManager",
                            "Characteristic: ${characteristic.uuid}, notify=$canNotify, write=$canWrite"
                        )
                        if (canNotify && canWrite) {
                            Log.i("BLEManager", "✅ Found ideal characteristic: ${characteristic.uuid}")
                            notifyCharacteristic = characteristic
                            writeCharacteristic = characteristic
                            enableNotifications(gatt, characteristic)
                            return
                        }
                    }
                }
            } else {
                Log.e("BLEManager", "❌ Service discovery failed: status=$status")
            }
        }

        private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            try {
                Log.d("BLEManager", "Enabling notifications for ${characteristic.uuid}")
                gatt.setCharacteristicNotification(characteristic, true)

                val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                val descriptor = characteristic.getDescriptor(cccdUuid)
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (descriptor != null) {
                    gatt.writeDescriptor(descriptor)
                    Log.i("BLEManager", "✅ Subscribed to notifications for ${characteristic.uuid}")
                } else {
                    Log.w("BLEManager", "⚠️ No descriptor found for ${characteristic.uuid}")
                }
            } catch (e: Exception) {
                Log.e("BLEManager", "❌ Failed to enable notifications", e)
            }
        }

//        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
//            val receivedChunk = String(characteristic.value, Charsets.UTF_8)
//            Log.v("BLEManager", "📩 Received chunk: '$receivedChunk'")
//            buffer.append(receivedChunk)
//
//            var newlineIndex = buffer.indexOf('\n')
//            while (newlineIndex != -1) {
//                val completeMessage = buffer.substring(0, newlineIndex).trim()
//                buffer.delete(0, newlineIndex + 1)
//                Log.d("BLEManager", "📦 Complete message received: '$completeMessage'")
//                processCompleteMessage(completeMessage)
//                newlineIndex = buffer.indexOf('\n')
//            }
//        }
override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
    // The incoming value is a complete message, so we process it directly.
    val completeMessage = String(characteristic.value, Charsets.UTF_8).trim()
    Log.v("BLEManager", "📩 Received message: '$completeMessage'")

    // Only process if the message isn't blank.
    if (completeMessage.isNotBlank()) {
        Log.d("BLEManager", "📦 Processing message: '$completeMessage'")
        processCompleteMessage(completeMessage)
    }
}

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Log.d("BLEManager", "✉️ Write completed for ${characteristic.uuid}, status=$status")
        }
    }

    // ------------------------- MESSAGE PARSING -------------------------
    private fun processCompleteMessage(message: String) {
        if (message.isBlank()) return
        Log.d("BLEManager", "🔍 Processing message: '$message'")

        try {
            val parts = message.split(',')
            val identifier = parts.getOrNull(0) ?: ""

            when {
                identifier.startsWith("m") || identifier.startsWith("s") -> {
                    val finalIdentifier = if (identifier.startsWith("m")) "master" else "slave"

                    val roll = decodeValue(parts.getOrNull(1))
                    val pitch = decodeValue(parts.getOrNull(2))
                    val yaw = decodeValue(parts.getOrNull(3))
                    val acceleration = parts.getOrNull(4)?.toFloatOrNull() ?: 0f

                    _sensorData.value = SensorDataDto(finalIdentifier, roll, pitch, yaw, acceleration)
                    Log.i("BLEManager", "📊 SensorData [$finalIdentifier] → roll=$roll, pitch=$pitch, yaw=$yaw, acc=$acceleration")
                }

                message.startsWith("cal") || message.startsWith("vibtime") -> {
                    _ackMessage.value = message
                    Log.i("BLEManager", "✅ ACK received: '$message'")
                }

                else -> {
                    Log.w("BLEManager", "⚠️ Unrecognized message: '$message'")
                }
            }

        } catch (e: Exception) {
            Log.e("BLEManager", "❌ Error parsing message: '$message'", e)
        }
    }

    private fun decodeValue(valueStr: String?): Float {
        val rawValue = valueStr?.toFloatOrNull()?.toInt() ?: 0
        val decodedValue = if (rawValue > 32767) rawValue - 65536 else rawValue
        return decodedValue / 100.0f
    }

    // ------------------------- WRITE COMMAND -------------------------
    fun writeCommand(command: String) {
        if (writeCharacteristic == null || bluetoothGatt == null) {
            Log.w("BLEManager", "⚠️ Cannot send command '$command' - no active characteristic or GATT connection")
            return
        }

        try {
            writeCharacteristic?.let { char ->
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                char.value = (command + "\n").toByteArray(Charsets.UTF_8)
                val success = bluetoothGatt?.writeCharacteristic(char) ?: false
                Log.i("BLEManager", "➡️ Sending: '$command' (success=$success)")
            }
        } catch (e: Exception) {
            Log.e("BLEManager", "❌ Failed to send command: '$command'", e)
        }
    }
}


