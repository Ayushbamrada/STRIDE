//
//package com.example.stride.ui.features.gait_test
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.example.stride.data.repository.GaitRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.flow.collectLatest
//import kotlinx.coroutines.flow.update
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//import kotlin.math.abs
//
//enum class TestState {
//    IDLE,
//    AUTO_CALIBRATING,
//    RUNNING,
//    FINISHED
//}
//
//data class GaitTestUiState(
//    val testState: TestState = TestState.IDLE,
//    val overlayMessage: String? = null,
//    val leftBallX: Float = 0.25f, // Start position for left
//    val leftBallY: Float = 0.9f,
//    val rightBallX: Float = 0.75f, // Start position for right
//    val rightBallY: Float = 0.9f,
//    // DEBUG DATA
//    val latestMasterPitch: Float = 0.0f,
//    val latestMasterRoll: Float = 0.0f,
//    val latestSlavePitch: Float = 0.0f,
//    val latestSlaveRoll: Float = 0.0f
//)
//
//@HiltViewModel
//class GaitTestViewModel @Inject constructor(
//    private val repository: GaitRepository
//) : ViewModel() {
//
//    private val _uiState = MutableStateFlow(GaitTestUiState())
//    val uiState = _uiState.asStateFlow()
//
//    private var dataCollectionJob: Job? = null
//    private var isCalibrated = false
//
//    // Calibration ranges for both legs
//    private var masterMinRoll = Float.MAX_VALUE; private var masterMaxRoll = Float.MIN_VALUE
//    private var masterMinPitch = Float.MAX_VALUE; private var masterMaxPitch = Float.MIN_VALUE
//    private var slaveMinRoll = Float.MAX_VALUE; private var slaveMaxRoll = Float.MIN_VALUE
//    private var slaveMinPitch = Float.MAX_VALUE; private var slaveMaxPitch = Float.MIN_VALUE
//
//    init {
//        startDataCollection()
//    }
//
//    private fun startDataCollection() {
//        dataCollectionJob?.cancel()
//        dataCollectionJob = viewModelScope.launch {
//            repository.sensorDataStream.collectLatest { data ->
//                data?.let {
//                    // Update debug data first
//                    _uiState.update { state ->
//                        if (it.identifier == "master") {
//                            state.copy(latestMasterRoll = it.roll, latestMasterPitch = it.pitch)
//                        } else {
//                            state.copy(latestSlaveRoll = it.roll, latestSlavePitch = it.pitch)
//                        }
//                    }
//
//                    if (_uiState.value.testState == TestState.AUTO_CALIBRATING) {
//                        if (it.identifier == "master") {
//                            masterMinRoll = minOf(masterMinRoll, it.roll)
//                            masterMaxRoll = maxOf(masterMaxRoll, it.roll)
//                            masterMinPitch = minOf(masterMinPitch, it.pitch)
//                            masterMaxPitch = maxOf(masterMaxPitch, it.pitch)
//                        } else if (it.identifier == "slave") {
//                            slaveMinRoll = minOf(slaveMinRoll, it.roll)
//                            slaveMaxRoll = maxOf(slaveMaxRoll, it.roll)
//                            slaveMinPitch = minOf(slaveMinPitch, it.pitch)
//                            slaveMaxPitch = maxOf(slaveMaxPitch, it.pitch)
//                        }
//                    } else if (_uiState.value.testState == TestState.RUNNING && isCalibrated) {
//                        updateBallPosition(it.identifier, it.roll, it.pitch)
//                    }
//                }
//            }
//        }
//    }
//
//    fun startTest() {
//        viewModelScope.launch {
//            resetCalibration()
//            isCalibrated = false
//            repository.sendCommand("game,1") // Use new lowercase command
//
//            _uiState.update { it.copy(
//                testState = TestState.AUTO_CALIBRATING,
//                overlayMessage = "Automatic Calibration...\nPlease move BOTH legs through their full range of motion for 5 seconds."
//            )}
//
//            delay(5000)
//            addCalibrationBuffer()
//            isCalibrated = true
//            _uiState.update { it.copy(
//                testState = TestState.RUNNING,
//                overlayMessage = null
//            )}
//        }
//    }
//
//    fun stopTest() {
//        repository.sendCommand("game,0") // Use new lowercase command
//        _uiState.update {
//            it.copy(
//                testState = TestState.FINISHED,
//                leftBallX = 0.25f, leftBallY = 0.9f,
//                rightBallX = 0.75f, rightBallY = 0.9f
//            )
//        }
//    }
//
//    private fun updateBallPosition(identifier: String, roll: Float, pitch: Float) {
//        if (identifier == "master") {
//            val rollRange = abs(masterMaxRoll - masterMinRoll)
//            val pitchRange = abs(masterMaxPitch - masterMinPitch)
//            if (rollRange < 1f || pitchRange < 1f) return // Avoid division by zero if no movement
//
//            // Normalize roll for X-axis (0.0 to 0.45 for left leg, with a gap in the middle)
//            val newX = ((roll - masterMinRoll) / rollRange) * 0.45f
//            // Normalize pitch for Y-axis and apply the "upside-down" fix
//            val newY = 1.0f - ((pitch - masterMinPitch) / pitchRange)
//
//            _uiState.update { it.copy(leftBallX = newX.coerceIn(0f, 1f), leftBallY = newY.coerceIn(0f, 1f)) }
//
//        } else if (identifier == "slave") {
//            val rollRange = abs(slaveMaxRoll - slaveMinRoll)
//            val pitchRange = abs(slaveMaxPitch - slaveMinPitch)
//            if (rollRange < 1f || pitchRange < 1f) return // Avoid division by zero if no movement
//
//            // Normalize roll for X-axis (0.55 to 1.0 for right leg, with a gap in the middle)
//            val newX = 0.55f + (((roll - slaveMinRoll) / rollRange) * 0.45f)
//            // Normalize pitch for Y-axis and apply the "upside-down" fix
//            val newY = 1.0f - ((pitch - slaveMinPitch) / pitchRange)
//
//            _uiState.update { it.copy(rightBallX = newX.coerceIn(0f, 1f), rightBallY = newY.coerceIn(0f, 1f)) }
//        }
//    }
//
//    private fun resetCalibration() {
//        masterMinRoll = Float.MAX_VALUE; masterMaxRoll = Float.MIN_VALUE
//        masterMinPitch = Float.MAX_VALUE; masterMaxPitch = Float.MIN_VALUE
//        slaveMinRoll = Float.MAX_VALUE; slaveMaxRoll = Float.MIN_VALUE
//        slaveMinPitch = Float.MAX_VALUE; slaveMaxPitch = Float.MIN_VALUE
//    }
//
//    private fun addCalibrationBuffer() {
//        val xBufferMaster = (masterMaxRoll - masterMinRoll) * 0.1f
//        masterMinRoll -= xBufferMaster
//        masterMaxRoll += xBufferMaster
//
//        val yBufferMaster = (masterMaxPitch - masterMinPitch) * 0.1f
//        masterMinPitch -= yBufferMaster
//        masterMaxPitch += yBufferMaster
//
//        val xBufferSlave = (slaveMaxRoll - slaveMinRoll) * 0.1f
//        slaveMinRoll -= xBufferSlave
//        slaveMaxRoll += xBufferSlave
//
//        val yBufferSlave = (slaveMaxPitch - slaveMinPitch) * 0.1f
//        slaveMinPitch -= yBufferSlave
//        slaveMaxPitch += yBufferSlave
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        repository.sendCommand("game,0") // Safety command
//        dataCollectionJob?.cancel()
//    }
//}
//
package com.example.stride.ui.features.gait_test

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stride.data.repository.GaitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

// Re-introducing AUTO_CALIBRATING state
enum class TestState {
    IDLE,
    AUTO_CALIBRATING,
    RUNNING,
    FINISHED
}

data class GaitTestUiState(
    val testState: TestState = TestState.IDLE,
    val overlayMessage: String? = null,
    val leftBallX: Float = 0.25f,
    val leftBallY: Float = 0.9f,
    val rightBallX: Float = 0.75f,
    val rightBallY: Float = 0.9f,
    val latestMasterPitch: Float = 0.0f,
    val latestMasterRoll: Float = 0.0f,
    val latestSlavePitch: Float = 0.0f,
    val latestSlaveRoll: Float = 0.0f
)

@HiltViewModel
class GaitTestViewModel @Inject constructor(
    private val repository: GaitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GaitTestUiState())
    val uiState = _uiState.asStateFlow()

    private var dataCollectionJob: Job? = null
    private var isCalibrated = false

    private var masterMinRoll = Float.MAX_VALUE; private var masterMaxRoll = Float.MIN_VALUE
    private var masterMinPitch = Float.MAX_VALUE; private var masterMaxPitch = Float.MIN_VALUE
    private var slaveMinRoll = Float.MAX_VALUE; private var slaveMaxRoll = Float.MIN_VALUE
    private var slaveMinPitch = Float.MAX_VALUE; private var slaveMaxPitch = Float.MIN_VALUE

    init {
        startDataCollection()
    }

    private fun startDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = viewModelScope.launch {
            repository.sensorDataStream.collectLatest { data ->
                data?.let {
                    if (_uiState.value.testState == TestState.AUTO_CALIBRATING) {
                        // --- SMART CALIBRATION: IGNORE OUTLIERS ---
                        // Only accept values within a reasonable range (e.g., -1000 to 1000)
                        if (it.pitch in -1000f..1000f && it.roll in -1000f..1000f) {
                            if (it.identifier == "master") {
                                masterMinRoll = minOf(masterMinRoll, it.roll)
                                masterMaxRoll = maxOf(masterMaxRoll, it.roll)
                                masterMinPitch = minOf(masterMinPitch, it.pitch)
                                masterMaxPitch = maxOf(masterMaxPitch, it.pitch)
                            } else {
                                slaveMinRoll = minOf(slaveMinRoll, it.roll)
                                slaveMaxRoll = maxOf(slaveMaxRoll, it.roll)
                                slaveMinPitch = minOf(slaveMinPitch, it.pitch)
                                slaveMaxPitch = maxOf(slaveMaxPitch, it.pitch)
                            }
                        }
                    } else if (_uiState.value.testState == TestState.RUNNING && isCalibrated) {
                        updateBallPosition(it.identifier, it.roll, it.pitch)
                    }

                    _uiState.update { state ->
                        if (it.identifier == "master") {
                            state.copy(latestMasterRoll = it.roll, latestMasterPitch = it.pitch)
                        } else {
                            state.copy(latestSlaveRoll = it.roll, latestSlavePitch = it.pitch)
                        }
                    }
                }
            }
        }
    }

    fun startTest() {
        viewModelScope.launch {
            resetCalibration()
            isCalibrated = false
            repository.sendCommand("game,1")

            _uiState.update { it.copy(
                testState = TestState.AUTO_CALIBRATING,
                overlayMessage = "Calibrating...\nPlease move BOTH legs through their full range of motion for 5 seconds."
            )}

            delay(5000)
            addCalibrationBuffer()
            isCalibrated = true
            _uiState.update { it.copy(
                testState = TestState.RUNNING,
                overlayMessage = null
            )}
        }
    }

    fun stopTest() {
        repository.sendCommand("game,0")
        _uiState.update {
            it.copy(
                testState = TestState.FINISHED,
                leftBallX = 0.25f, leftBallY = 0.9f,
                rightBallX = 0.75f, rightBallY = 0.9f
            )
        }
    }

    private fun updateBallPosition(identifier: String, roll: Float, pitch: Float) {
        if (identifier == "master") {
            val rollRange = abs(masterMaxRoll - masterMinRoll)
            val pitchRange = abs(masterMaxPitch - masterMinPitch)
            if (rollRange < 1f || pitchRange < 1f) return

            val newX = ((roll - masterMinRoll) / rollRange) * 0.45f
            val newY = 1.0f - ((pitch - masterMinPitch) / pitchRange)
            _uiState.update { it.copy(leftBallX = newX.coerceIn(0f, 1f), leftBallY = newY.coerceIn(0f, 1f)) }

        } else if (identifier == "slave") {
            val rollRange = abs(slaveMaxRoll - slaveMinRoll)
            val pitchRange = abs(slaveMaxPitch - slaveMinPitch)
            if (rollRange < 1f || pitchRange < 1f) return

            val newX = 0.55f + (((roll - slaveMinRoll) / rollRange) * 0.45f)
            val newY = 1.0f - ((pitch - slaveMinPitch) / pitchRange)
            _uiState.update { it.copy(rightBallX = newX.coerceIn(0f, 1f), rightBallY = newY.coerceIn(0f, 1f)) }
        }
    }


    private fun resetCalibration() {
        masterMinRoll = Float.MAX_VALUE; masterMaxRoll = Float.MIN_VALUE
        masterMinPitch = Float.MAX_VALUE; masterMaxPitch = Float.MIN_VALUE
        slaveMinRoll = Float.MAX_VALUE; slaveMaxRoll = Float.MIN_VALUE
        slaveMinPitch = Float.MAX_VALUE; slaveMaxPitch = Float.MIN_VALUE
    }
        private fun addCalibrationBuffer() {
        val xBufferMaster = (masterMaxRoll - masterMinRoll) * 0.1f
        masterMinRoll -= xBufferMaster
        masterMaxRoll += xBufferMaster

        val yBufferMaster = (masterMaxPitch - masterMinPitch) * 0.1f
        masterMinPitch -= yBufferMaster
        masterMaxPitch += yBufferMaster

        val xBufferSlave = (slaveMaxRoll - slaveMinRoll) * 0.1f
        slaveMinRoll -= xBufferSlave
        slaveMaxRoll += xBufferSlave

        val yBufferSlave = (slaveMaxPitch - slaveMinPitch) * 0.1f
        slaveMinPitch -= yBufferSlave
        slaveMaxPitch += yBufferSlave
    }

    override fun onCleared() {
        super.onCleared()
        repository.sendCommand("game,0")
        dataCollectionJob?.cancel()
    }
}

