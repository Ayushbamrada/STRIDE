//
//package com.example.stride.ui.features.gait_test
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.Stop
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.DrawScope
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun GaitTestScreen(
//    onNavigateUp: () -> Unit,
//    viewModel: GaitTestViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Gait Test") },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateUp) {
//                        Icon(Icons.Default.ArrowBack, "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//            GaitCanvas(
//                leftBallX = uiState.leftBallX,
//                leftBallY = uiState.leftBallY,
//                rightBallX = uiState.rightBallX,
//                rightBallY = uiState.rightBallY
//            )
//
//            // UI Controls at the bottom
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
//                    .padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                DebugDataDisplay(uiState)
//                Spacer(Modifier.height(16.dp))
//                TestControls(
//                    testState = uiState.testState,
//                    onStartTest = viewModel::startTest,
//                    onStopTest = viewModel::stopTest,
//                )
//            }
//
//            // Simple overlay for auto-calibration
//            AutoCalibrationOverlay(
//                message = uiState.overlayMessage
//            )
//        }
//    }
//}
//
//@Composable
//private fun TestControls(
//    testState: TestState,
//    onStartTest: () -> Unit,
//    onStopTest: () -> Unit,
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.Center // Centered single button
//    ) {
//        // START/STOP BUTTON
//        if (testState == TestState.RUNNING || testState == TestState.AUTO_CALIBRATING) {
//            Button(onClick = onStopTest, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
//                Icon(Icons.Default.Stop, contentDescription = "Stop Test")
//                Spacer(Modifier.width(8.dp))
//                Text("Stop Test")
//            }
//        } else {
//            Button(onClick = onStartTest, modifier = Modifier.fillMaxWidth(0.8f)) {
//                Icon(Icons.Default.PlayArrow, contentDescription = "Start Test")
//                Spacer(Modifier.width(8.dp))
//                Text("Start Test")
//            }
//        }
//    }
//}
//
//@Composable
//fun AutoCalibrationOverlay(message: String?) {
//    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.7f)),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = message ?: "",
//                color = Color.White,
//                fontSize = 22.sp,
//                textAlign = TextAlign.Center,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(32.dp)
//            )
//        }
//    }
//}
//
//
//@Composable
//private fun DebugDataDisplay(uiState: GaitTestUiState) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(8.dp),
//            horizontalArrangement = Arrangement.SpaceAround
//        ) {
//            Text("Leg: ${uiState.latestLeg}")
//            Text("X: ${"%.2f".format(uiState.latestAccX)}")
//            Text("Y: ${"%.2f".format(uiState.latestAccY)}")
//            Text("Z: ${"%.2f".format(uiState.latestAccZ)}")
//        }
//    }
//}
//
//@Composable
//private fun GaitCanvas(leftBallX: Float, leftBallY: Float, rightBallX: Float, rightBallY: Float) {
//    Canvas(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(8.dp)
//            .border(2.dp, MaterialTheme.colorScheme.outline)
//    ) {
//        drawGrid(20)
//
//        // Draw Left Ball (Blue)
//        drawCircle(
//            color = Color.Blue,
//            radius = 20f,
//            center = Offset(size.width * leftBallX, size.height * leftBallY)
//        )
//        // Draw Right Ball (Red)
//        drawCircle(
//            color = Color.Red,
//            radius = 20f,
//            center = Offset(size.width * rightBallX, size.height * rightBallY)
//        )
//    }
//}
//
//private fun DrawScope.drawGrid(steps: Int) {
//    val stepSize = size.width / steps
//    for (i in 1 until steps) {
//        val x = i * stepSize
//        drawLine(
//            color = Color.LightGray.copy(alpha = 0.5f),
//            start = Offset(x, 0f),
//            end = Offset(x, size.height),
//            strokeWidth = 1f
//        )
//    }
//    val yStepSize = size.height / (steps * (size.height / size.width))
//    var y = yStepSize
//    while (y < size.height) {
//        drawLine(
//            color = Color.LightGray.copy(alpha = 0.5f),
//            start = Offset(0f, y),
//            end = Offset(size.width, y),
//            strokeWidth = 1f
//        )
//        y += yStepSize
//    }
//}
package com.example.stride.ui.features.gait_test

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaitTestScreen(
    onNavigateUp: () -> Unit,
    viewModel: GaitTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gait Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GaitCanvas(
                leftBallX = uiState.leftBallX,
                leftBallY = uiState.leftBallY,
                rightBallX = uiState.rightBallX,
                rightBallY = uiState.rightBallY
            )

            // UI Controls at the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DebugDataDisplay(uiState)
                Spacer(Modifier.height(16.dp))
                TestControls(
                    testState = uiState.testState,
                    onStartTest = viewModel::startTest,
                    onStopTest = viewModel::stopTest,
                )
            }
        }
    }
}

@Composable
private fun DebugDataDisplay(uiState: GaitTestUiState) {
    Card(

        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Master (Left)", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Roll: ${"%.2f".format(uiState.latestMasterRoll)}")
                Text("Pitch: ${"%.2f".format(uiState.latestMasterPitch)}")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Slave (Right)", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Roll: ${"%.2f".format(uiState.latestSlaveRoll)}")
                Text("Pitch: ${"%.2f".format(uiState.latestSlavePitch)}")

            }
        }
    }
}

@Composable
private fun TestControls(testState: TestState, onStartTest: () -> Unit, onStopTest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (testState == TestState.RUNNING) {
            Button(
                onClick = onStopTest,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop Test")
                Spacer(Modifier.width(8.dp))
                Text("Stop Test")
            }
        } else {
            Button(onClick = onStartTest, modifier = Modifier.fillMaxWidth(0.8f)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start Test")
                Spacer(Modifier.width(8.dp))
                Text("Start Test")
            }
        }
    }
}

@Composable
private fun GaitCanvas(leftBallX: Float, leftBallY: Float, rightBallX: Float, rightBallY: Float) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .border(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        drawGrid(20)

        // Draw Left Ball (Blue)
        drawCircle(
            color = Color.Blue,
            radius = 30f,
            center = Offset(size.width * leftBallX, size.height * leftBallY)
        )
        // Draw Right Ball (Red)
        drawCircle(
            color = Color.Red,
            radius = 30f,
            center = Offset(size.width * rightBallX, size.height * rightBallY)
        )
    }
}

private fun DrawScope.drawGrid(steps: Int) {
    val stepSize = size.width / steps
    for (i in 1 until steps) {
        val x = i * stepSize
        drawLine(
            color = Color.LightGray.copy(alpha = 0.5f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
    }
    val yStepSize = size.height / (steps * (size.height / size.width))
    var y = yStepSize
    while (y < size.height) {
        drawLine(
            color = Color.LightGray.copy(alpha = 0.5f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += yStepSize
    }
}


