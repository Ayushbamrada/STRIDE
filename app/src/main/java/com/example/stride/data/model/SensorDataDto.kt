//package com.example.stride.data.model
//
//data class SensorDataDto(
//    val leg: String,
//    val accX: Float,
//    val accY: Float,
//    val accZ: Float
//)
//


package com.example.stride.data.model

/**
 * Represents a single packet of sensor data received from either device.
 * Now includes all the new fields.
 */
data class SensorDataDto(
    val identifier: String, // "master" or "slave"
    val roll: Float,
    val pitch: Float,
    val yaw: Float,
    val acceleration: Float
)

