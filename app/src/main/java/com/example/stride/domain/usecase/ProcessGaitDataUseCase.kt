package com.example.stride.domain.usecase

import com.example.stride.data.model.SensorDataDto
import com.example.stride.domain.model.GaitMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * A use case that transforms a raw stream of sensor data into a stream of meaningful GaitMetrics.
 * This is where the core logic for peak detection, timing, and coordinate mapping will live.
 */
class ProcessGaitDataUseCase @Inject constructor() {

    // In a real implementation, this would contain complex logic.
    // For now, it performs a simple mapping from the DTO to our domain model.
    operator fun invoke(sensorDataFlow: Flow<SensorDataDto?>): Flow<GaitMetrics?> {
        return sensorDataFlow.map { data ->
            if (data == null) return@map null

            // TODO: Implement actual gait analysis logic here:
            // 1. Apply calibration values to scale pitch and roll.
            // 2. Implement a state machine to detect when a step starts (using acceleration).
            // 3. Detect the peak (highest pitch value) within a step.
            // 4. Calculate the time from step start to peak.
            // 5. Set isPeakReached = true for one emission when the peak is found.

            // --- THIS IS THE FIX ---
            // We now correctly use `pitch` for vertical movement and `roll` for horizontal movement.
            GaitMetrics(
                verticalDisplacement = data.pitch, // Placeholder: should be scaled
                horizontalSway = data.roll,     // Placeholder: should be scaled
                peakTimeMillis = 0L,              // Placeholder
                isPeakReached = false             // Placeholder
            )
        }
    }
}

