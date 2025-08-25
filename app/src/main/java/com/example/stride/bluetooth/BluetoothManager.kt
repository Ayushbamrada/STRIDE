package com.example.stride.bluetooth

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.example.stride.data.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothManager(
    private val context: Context,
    private val device: BluetoothDevice
) {
    private var bluetoothGatt: BluetoothGatt? = null

    // Initially null — you’ll set them once you identify the correct ones
    private var serviceUuid: String? = null
    private var characteristicUuid: String? = null

    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun connect() {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun stop() {
        disconnect()
        scope.cancel()
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BLEManager", "✅ Connected to GATT server")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BLEManager", "❌ Disconnected from GATT server")
                    disconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLEManager", "🔍 Services discovered, scanning characteristics...")

                for (service in gatt.services) {
                    for (characteristic in service.characteristics) {
                        val props = characteristic.properties

                        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 && notifyCharacteristic == null) {
                            notifyCharacteristic = characteristic
                            enableNotifications(gatt, characteristic)
                            Log.d("BLEManager", "📡 Subscribed to NOTIFY characteristic: ${characteristic.uuid}")
                        }

                        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 && writeCharacteristic == null) {
                            writeCharacteristic = characteristic
                            Log.d("BLEManager", "✍️ Found WRITE characteristic: ${characteristic.uuid}")
                        }
                    }
                }

                if (notifyCharacteristic == null) {
                    Log.e("BLEManager", "⚠️ No NOTIFY characteristic found!")
                }
            }
        }

        private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(characteristic, true)

            val descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // Client Characteristic Config Descriptor (CCCD)
            )
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value?.decodeToString() ?: return
            Log.d("BLEManager", "📥 Received: $data")

            if (data.startsWith("R:")) {
                val parts = data.removePrefix("R:").split(",")
                if (parts.size == 4) {
                    val acc = parts[0].toFloatOrNull() ?: return
                    val la = parts[1].toFloatOrNull() ?: return
                    val gy = parts[2].toFloatOrNull() ?: return
                    val roll = parts[3].toFloatOrNull() ?: return

                    scope.launch {
                        _sensorData.value = SensorData(acc, la, gy, roll)
                    }
                }
            }
        }
    }

    /** Helper to show readable properties */
    private fun parseProperties(props: Int): String {
        val list = mutableListOf<String>()
        if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0) list.add("READ")
        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) list.add("WRITE")
        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) list.add("NOTIFY")
        if (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) list.add("INDICATE")
        return list.joinToString("|")
    }

    /** Example of sending commands (start/stop) */
    fun writeCommand(command: String) {
        val characteristic = writeCharacteristic
        if (characteristic != null) {
            characteristic.value = command.toByteArray(Charsets.UTF_8)
            bluetoothGatt?.writeCharacteristic(characteristic)
            Log.d("BLEManager", "✍️ Command sent: $command")
        } else {
            Log.e("BLEManager", "⚠️ No writable characteristic available!")
        }
    }

    fun startReading() {
        writeCommand("start\n")
    }

    fun stopReading() {
        writeCommand("stop\n")
    }
}
