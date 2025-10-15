package com.example.stride.domain.model

/**
 * Represents processed, meaningful metrics derived from raw sensor data.
 * This is a core business model, independent of any data source.
 */
data class GaitMetrics(
    val verticalDisplacement: Float, // Y-axis movement on the 2D plane
    val horizontalSway: Float,     // X-axis movement on the 2D plane
    val peakTimeMillis: Long,        // Time taken to reach the peak of a step
    val isPeakReached: Boolean       // A flag to indicate a peak was just detected
)
