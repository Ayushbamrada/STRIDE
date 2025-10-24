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
//// Re-introducing AUTO_CALIBRATING state
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
//    val leftBallX: Float = 0.25f,
//    val leftBallY: Float = 0.9f,
//    val rightBallX: Float = 0.75f,
//    val rightBallY: Float = 0.9f,
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
//                    if (_uiState.value.testState == TestState.AUTO_CALIBRATING) {
//                        // --- SMART CALIBRATION: IGNORE OUTLIERS ---
//                        // Only accept values within a reasonable range (e.g., -1000 to 1000)
//                        if (it.pitch in -1000f..1000f && it.roll in -1000f..1000f) {
//                            if (it.identifier == "master") {
//                                masterMinRoll = minOf(masterMinRoll, it.roll)
//                                masterMaxRoll = maxOf(masterMaxRoll, it.roll)
//                                masterMinPitch = minOf(masterMinPitch, it.pitch)
//                                masterMaxPitch = maxOf(masterMaxPitch, it.pitch)
//                            } else {
//                                slaveMinRoll = minOf(slaveMinRoll, it.roll)
//                                slaveMaxRoll = maxOf(slaveMaxRoll, it.roll)
//                                slaveMinPitch = minOf(slaveMinPitch, it.pitch)
//                                slaveMaxPitch = maxOf(slaveMaxPitch, it.pitch)
//                            }
//                        }
//                    } else if (_uiState.value.testState == TestState.RUNNING && isCalibrated) {
//                        updateBallPosition(it.identifier, it.roll, it.pitch)
//                    }
//
//                    _uiState.update { state ->
//                        if (it.identifier == "master") {
//                            state.copy(latestMasterRoll = it.roll, latestMasterPitch = it.pitch)
//                        } else {
//                            state.copy(latestSlaveRoll = it.roll, latestSlavePitch = it.pitch)
//                        }
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
//            repository.sendCommand("game,1")
//
//            _uiState.update { it.copy(
//                testState = TestState.AUTO_CALIBRATING,
//                overlayMessage = "Calibrating...\nPlease move BOTH legs through their full range of motion for 5 seconds."
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
//        repository.sendCommand("game,0")
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
//            if (rollRange < 1f || pitchRange < 1f) return
//
//            val newX = ((roll - masterMinRoll) / rollRange) * 0.45f
//            val newY = 1.0f - ((pitch - masterMinPitch) / pitchRange)
//            _uiState.update { it.copy(leftBallX = newX.coerceIn(0f, 1f), leftBallY = newY.coerceIn(0f, 1f)) }
//
//        } else if (identifier == "slave") {
//            val rollRange = abs(slaveMaxRoll - slaveMinRoll)
//            val pitchRange = abs(slaveMaxPitch - slaveMinPitch)
//            if (rollRange < 1f || pitchRange < 1f) return
//
//            val newX = 0.55f + (((roll - slaveMinRoll) / rollRange) * 0.45f)
//            val newY = 1.0f - ((pitch - slaveMinPitch) / pitchRange)
//            _uiState.update { it.copy(rightBallX = newX.coerceIn(0f, 1f), rightBallY = newY.coerceIn(0f, 1f)) }
//        }
//    }
//
//
//    private fun resetCalibration() {
//        masterMinRoll = Float.MAX_VALUE; masterMaxRoll = Float.MIN_VALUE
//        masterMinPitch = Float.MAX_VALUE; masterMaxPitch = Float.MIN_VALUE
//        slaveMinRoll = Float.MAX_VALUE; slaveMaxRoll = Float.MIN_VALUE
//        slaveMinPitch = Float.MAX_VALUE; slaveMaxPitch = Float.MIN_VALUE
//    }
//        private fun addCalibrationBuffer() {
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
//        repository.sendCommand("game,0")
//        dataCollectionJob?.cancel()
//    }
//}
//
package com.example.stride.ui.features.gait_test

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stride.data.model.SensorDataDto
import com.example.stride.data.repository.GaitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class TestState {
    IDLE,
    CALIBRATING_L_SURFACE,
    CALIBRATING_L_PEAK,
    CALIBRATING_R_SURFACE,
    CALIBRATING_R_PEAK,
    READY,
    RUNNING,
    FINISHED
}

data class GaitTestUiState(
    val testState: TestState = TestState.IDLE,
    val leftBallPosition: Float = 1f,
    val rightBallPosition: Float = 1f,
    val activeLeg: String? = null,
    val results: List<String> = emptyList()
)

@HiltViewModel
class GaitTestViewModel @Inject constructor(
    private val repository: GaitRepository
) : ViewModel() {

    companion object { private const val TAG = "GaitTestViewModel" }

    private val _uiState = MutableStateFlow(GaitTestUiState())
    val uiState = _uiState.asStateFlow()

    private var dataCollectionJob: Job? = null

    private val latestMasterData = MutableStateFlow<SensorDataDto?>(null)
    private val latestSlaveData = MutableStateFlow<SensorDataDto?>(null)

    private var leftSurfacePitch: Float = 0f
    private var leftPeakPitch: Float = 0f
    private var rightSurfacePitch: Float = 0f
    private var rightPeakPitch: Float = 0f

    private var isWaitingForMovement = false
    private var movementStartTime = 0L

    init {
        Log.d(TAG, "init - starting dataCollection from repository")
        startDataSorting()
    }

    private fun startDataSorting() {
        // collect repository.sensorDataStream, route master/slave to latest... flows,
        // and update visual positions while RUNNING
        dataCollectionJob = repository.sensorDataStream
            .onEach { data ->
                try {
                    if (data == null) {
                        Log.v(TAG, "stream emitted null")
                        return@onEach
                    }

                    Log.d(TAG, "stream item id=${data.identifier} pitch=${data.pitch} acc=${data.acceleration}")

                    if (data.identifier == "master") {
                        latestMasterData.value = data
                    } else {
                        latestSlaveData.value = data
                    }

                    if (_uiState.value.testState == TestState.RUNNING) {
                        updateBallPosition(data.identifier, data.pitch, data.acceleration)
                    } else {
                        // Also update small preview when not running (optional)
                        // _uiState.update { it.copy(leftBallPosition = ...) } // avoid frequent UI updates when not needed
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Exception while processing stream: ${t.message}", t)
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    fun onCalibrationAction() {
        Log.d(TAG, "onCalibrationAction() called (state=${_uiState.value.testState})")
        viewModelScope.launch {
            val currentTestState = _uiState.value.testState

            if (currentTestState == TestState.IDLE || currentTestState == TestState.FINISHED) {
                Log.d(TAG, "Starting calibration: sending 'game,1' and switching to CALIBRATING_L_SURFACE")
                // ask device to start sending calibration-friendly data (if required by device)
                repository.sendCommand("game,1")
                _uiState.update { it.copy(testState = TestState.CALIBRATING_L_SURFACE) }
                return@launch
            }

            try {
                when (currentTestState) {
                    TestState.CALIBRATING_L_SURFACE -> {
                        Log.d(TAG, "Awaiting latestMasterData for L surface calibration...")
                        Log.d(TAG, latestMasterData.toString())

                        val data = latestMasterData.filterNotNull().first()
                        Log.d(TAG, "Captured L surface data = $data")
                        leftSurfacePitch = data.pitch
                        _uiState.update { it.copy(testState = TestState.CALIBRATING_L_PEAK) }
                    }
                    TestState.CALIBRATING_L_PEAK -> {
                        Log.d(TAG, "Awaiting latestMasterData for L peak calibration...")
                        val data = latestMasterData.filterNotNull().first()
                        Log.d(TAG, "Captured L peak data = $data")
                        leftPeakPitch = data.pitch
                        _uiState.update { it.copy(testState = TestState.CALIBRATING_R_SURFACE) }
                    }
                    TestState.CALIBRATING_R_SURFACE -> {
                        Log.d(TAG, "Awaiting latestSlaveData for R surface calibration...")
                        val data = latestSlaveData.filterNotNull().first()
                        Log.d(TAG, "Captured R surface data = $data")
                        rightSurfacePitch = data.pitch
                        _uiState.update { it.copy(testState = TestState.CALIBRATING_R_PEAK) }
                    }
                    TestState.CALIBRATING_R_PEAK -> {
                        Log.d(TAG, "Awaiting latestSlaveData for R peak calibration...")
                        val data = latestSlaveData.filterNotNull().first()
                        Log.d(TAG, "Captured R peak data = $data")
                        rightPeakPitch = data.pitch
                        _uiState.update { it.copy(testState = TestState.READY) }
                        Log.d(TAG, "Calibration complete — READY")
                    }
                    else -> {}
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Exception during calibration action: ${t.message}", t)
            }
        }
    }

    fun startTest() {
        Log.d(TAG, "startTest() called (state=${_uiState.value.testState})")
        if (_uiState.value.testState != TestState.READY) {
            Log.w(TAG, "startTest() aborted: not READY")
            return
        }
        _uiState.update { it.copy(testState = TestState.RUNNING, results = emptyList()) }
        triggerVibration("left")
    }

    private fun triggerVibration(leg: String) {
        Log.d(TAG, "Simulating vibration for $leg leg.")
        isWaitingForMovement = true
        movementStartTime = System.currentTimeMillis()
        _uiState.update { it.copy(activeLeg = leg) }
        // In your real app you should call repository/bleManager.writeCommand("vibrate,...") here
    }

    private fun updateBallPosition(identifier: String, pitch: Float, acceleration: Float) {
        try {
            val isMaster = identifier == "master"
            Log.v(TAG, "updateBallPosition id=$identifier pitch=$pitch acc=$acceleration")

            if (isWaitingForMovement && acceleration > 0.5f) {
                val reactionTime = System.currentTimeMillis() - movementStartTime
                Log.d(TAG, "$identifier movement detected! Reaction time: $reactionTime ms")
                isWaitingForMovement = false
            }

            val surfacePitch = if (isMaster) leftSurfacePitch else rightSurfacePitch
            val peakPitch = if (isMaster) leftPeakPitch else rightPeakPitch
            val range = peakPitch - surfacePitch

            if (abs(range) > 0.1f) {
                val normalizedPosition = ((pitch - surfacePitch) / range).coerceIn(0f, 1f)
                val screenPosition = 1.0f - normalizedPosition

                if (isMaster) _uiState.update { it.copy(leftBallPosition = screenPosition) }
                else _uiState.update { it.copy(rightBallPosition = screenPosition) }

                Log.v(TAG, "id=$identifier normalizedPosition=$normalizedPosition screenPosition=$screenPosition")

                if (normalizedPosition >= 0.98f && !isWaitingForMovement) {
                    handlePeakReached(identifier)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Exception in updateBallPosition: ${t.message}", t)
        }
    }

    private fun handlePeakReached(identifier: String) {
        try {
            if (movementStartTime > 0) {
                val movementTime = System.currentTimeMillis() - movementStartTime
                val legName = if (identifier == "master") "Left" else "Right"
                val result = "$legName Leg: Reached peak in $movementTime ms"
                Log.d(TAG, "handlePeakReached -> $result")
                _uiState.update { it.copy(results = it.results + result) }
                movementStartTime = 0L

                viewModelScope.launch {
                    try {
                        kotlinx.coroutines.delay(2000)
                        val nextLeg = if (legName == "Left") "right" else "left"
                        triggerVibration(nextLeg)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Exception scheduling next vibration: ${t.message}", t)
                    }
                }
            } else {
                Log.w(TAG, "handlePeakReached called but movementStartTime == 0 (ignored)")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Exception in handlePeakReached: ${t.message}", t)
        }
    }

    fun stopTest() {
        Log.d(TAG, "stopTest() called")
        repository.sendCommand("game,0")
        _uiState.update { it.copy(testState = TestState.FINISHED, activeLeg = null) }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared() - cancelling data collection")
        dataCollectionJob?.cancel()
    }
}



