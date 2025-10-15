package com.example.stride.domain.model

/**
 * Represents the final, summarized result of a single Gait Test session.
 */
data class TestResult(
    val timestamp: Long,
    val averagePeakHeight: Float,
    val averageTimeToPeak: Long,
    val repetitions: Int
)
