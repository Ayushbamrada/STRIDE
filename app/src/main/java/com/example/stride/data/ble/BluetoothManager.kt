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

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow<SensorDataDto?>(null)
    val sensorData: StateFlow<SensorDataDto?> = _sensorData.asStateFlow()

    private val _ackMessage = MutableStateFlow<String?>(null)
    val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()

    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            return
        }
        _connectionState.value = ConnectionState.CONNECTING
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun close() {
        if (bluetoothGatt != null) {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                writeCommand("game,0") // Use lowercase command
            }
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _connectionState.value = ConnectionState.DISCONNECTED
            Log.d("BLEManager", "GATT connection closed and resources released.")
        }
    }
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.d("BLEManager", "Successfully connected to ${device.address}")
                    Thread.sleep(500)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    Log.d("BLEManager", "Device disconnected.")
                }
            } else {
                Log.e("BLEManager", "Connection state change error with status: $status")
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLEManager", "Services discovered for ${device.address}")
                var foundNotify = false
                var foundWrite = false

                gatt.services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        val canNotify = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                        val canWrite = characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0

                        if (canNotify && canWrite) {
                            Log.d("BLEManager", "Found ideal characteristic (Notify & Write): ${characteristic.uuid}")
                            notifyCharacteristic = characteristic
                            writeCharacteristic = characteristic
                            foundNotify = true
                            foundWrite = true
                            return@forEach
                        }
                    }
                    if(foundNotify) return@forEach
                }

                if (!foundNotify || !foundWrite) {
                    gatt.services.forEach { service ->
                        service.characteristics.forEach { characteristic ->
                            if (!foundNotify && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                                notifyCharacteristic = characteristic
                                foundNotify = true
                            }
                            if (!foundWrite && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                                writeCharacteristic = characteristic
                                foundWrite = true
                            }
                        }
                    }
                }

                if (foundNotify) {
                    enableNotifications(gatt, notifyCharacteristic!!)
                }
            }
        }

        private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(characteristic, true)
            val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(cccdUuid)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val dataBytes = characteristic.value ?: return
            val receivedString = String(dataBytes, Charsets.UTF_8).trim()
            Log.d("LiveData",receivedString);

            // --- THIS IS THE "TOUGH AS NAILS" PARSER ---
            try {
                val parts = receivedString.split(',')
                val identifier = parts.getOrNull(0) ?: ""

                // --- FIX 1: IGNORE CORRUPTED PACKETS ---
                // If the identifier isn't 'm' or 's' (or contains 'master'/'slave'), it's garbage.
                if (!identifier.contains("m") && !identifier.contains("s")) {
                    Log.w("BLEManager", "Ignoring corrupted or status packet: $receivedString")
                    return // Immediately stop processing this message
                }

                val finalIdentifier = if (identifier.contains("m")) "master" else "slave"

                // --- FIX 2: PARSE & SCALE ROBUSTLY ---
                // Divide by 100 as per engineer's instructions
                val roll = (parts.getOrNull(1)?.toFloatOrNull() ?: 0f) / 100.0f
                val pitch = (parts.getOrNull(2)?.toFloatOrNull() ?: 0f) / 100.0f
                val yaw = (parts.getOrNull(3)?.toFloatOrNull() ?: 0f) / 100.0f
                val acceleration = (parts.getOrNull(4)?.toFloatOrNull() ?: 0f) // Accel is not scaled

                _sensorData.value = SensorDataDto(
                    identifier = finalIdentifier,
                    roll = roll,
                    pitch = pitch,
                    yaw = yaw,
                    acceleration = acceleration
                )

            } catch (e: Exception) {
                Log.e("BLEManager", "Could not parse incoming data: '$receivedString'", e)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {}
    }

    fun writeCommand(command: String) {
        if (writeCharacteristic == null) {
            Log.e("BLEManager", "Cannot send command. Write characteristic is not available.")
            return
        }
        writeCharacteristic?.let { char ->
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            char.value = (command + "\n").toByteArray(Charsets.UTF_8)
            bluetoothGatt?.writeCharacteristic(char)
            Log.d("BLEManager", "➡️ Sending: '$command'")
        }
    }
}

