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
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class ConnectionState{
    Connected,
    Connecting,
    Disconnected
}

class BluetoothManager(
    private val context: Context,
    private val device: BluetoothDevice
) {
    private var bluetoothGatt: BluetoothGatt? = null

    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _ackMessage = MutableStateFlow<String?>(null)
    val ackMessage: StateFlow<String?> = _ackMessage.asStateFlow()

    // ✅ Holds last single value
    private val _data = MutableStateFlow("")
    val data: StateFlow<String> = _data.asStateFlow()

    // Rolling buffer of max 10 received values
    private val _dataList = MutableStateFlow<List<String>>(emptyList())
    val dataList: StateFlow<List<String>> = _dataList.asStateFlow()

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
                    _connectionState.value=ConnectionState.Connected
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value=ConnectionState.Disconnected
                    Log.d("BLEManager", "❌ Disconnected from GATT server")
                    disconnect()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value=ConnectionState.Connecting
                    Log.d("BLEManager","Connecting")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLEManager", "✅ Write confirmed by device: ${String(characteristic.value)}")
            } else {
                Log.e("BLEManager", "❌ Write failed, status=$status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLEManager", "🔍 Services discovered, scanning characteristics...")

                for (service in gatt.services) {
                    for (characteristic in service.characteristics) {
                        val props = characteristic.properties

                        Log.d("Properties",props.toString())

                        if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 && notifyCharacteristic == null) {
                            notifyCharacteristic = characteristic
                            enableNotifications(gatt, characteristic)
                            writeCharacteristic = characteristic
                            Log.d("BLEManager", "📡 Subscribed to NOTIFY characteristic: ${characteristic.uuid}")
                        }

                        if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 && writeCharacteristic == null) {
//                            writeCharacteristic = characteristic
//                            Log.d("BLEManager", "✍️ Found WRITE characteristic: ${characteristic.uuid}")
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
            val dataBytes = characteristic.value ?: return
            // Convert the byte array to a String using a specific charset (usually UTF-8)
            val receivedString = String(dataBytes, Charsets.UTF_8)

            Log.d("BLEManager", "📥 Received: $receivedString")

            _data.value = receivedString

            if (receivedString.startsWith("cal")||
                receivedString.startsWith("vibtime")||
                receivedString.startsWith("vibtimegap")||
                receivedString.startsWith("vibcount")||
                receivedString.startsWith("maxdelay"))
            {
                if(receivedString == "cal,1"){

                }
                _ackMessage.value=receivedString
            }

            // Append to rolling buffer of 10
            val currentList = _dataList.value.toMutableList()
            if (currentList.size >= 10) {
                currentList.removeAt(0) // remove oldest
            }
            currentList.add(receivedString)
            _dataList.value = currentList
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
        characteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val commandBytes = (command + "\n").toByteArray(Charsets.UTF_8)

        characteristic?.value = commandBytes

        val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
        Log.d("BLEManager", "➡️ Sending: '$command' (success queued = $success)")
    }

    fun clearDataList() {
        _dataList.value = emptyList()
    }

    fun startReading() {
        writeCommand("start\n")
    }

    fun stopReading() {
        writeCommand("stop\n")
    }

}
