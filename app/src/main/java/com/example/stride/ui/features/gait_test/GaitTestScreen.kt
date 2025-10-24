//package com.example.stride.ui.features.gait_test
//
//import android.util.Log
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.*
//import androidx.compose.animation.fadeIn
//import androidx.compose.animation.fadeOut
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.PlayArrow
//import androidx.compose.material.icons.filled.Stop
//import androidx.compose.material.icons.filled.Tune
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.drawscope.Stroke
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.compose.foundation.Canvas
//import androidx.compose.ui.layout.onGloballyPositioned
//import androidx.compose.ui.unit.IntSize
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun GaitTestScreen(
//    onNavigateUp: () -> Unit,
//    viewModel: GaitTestViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//
//    // blinking animation (shared)
//    val infiniteTransition = rememberInfiniteTransition()
//    val blinkAlpha by infiniteTransition.animateFloat(
//        initialValue = 0.25f,
//        targetValue = 1f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 600, easing = LinearEasing),
//            repeatMode = RepeatMode.Reverse
//        )
//    )
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Gait Test") },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateUp) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
//                }
//            )
//        }
//    ) { padding ->
//        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
//            // background canvas (goal circles)
//            GaitCanvas(leftBallY = uiState.leftBallPosition, rightBallY = uiState.rightBallPosition)
//
//            // Left / Right small moving balls + blinking indicator
//            Row(modifier = Modifier.fillMaxSize()) {
//                // Left column (50% width)
//                Box(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .fillMaxWidth(0.5f)
//                        .padding(16.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Column(
//                        modifier = Modifier.fillMaxHeight(),
//                        verticalArrangement = Arrangement.SpaceBetween,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        // Goal circle top (already drawn by GaitCanvas)
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        // moving ball area - align vertically by leftBallY (0..1)
//                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//                            // vertical offset using leftBallY
//                            val leftYFraction = uiState.leftBallPosition.coerceIn(0f, 1f)
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(300.dp) // fixed area for moving ball; tweak as needed
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .offset(y = (leftYFraction * 240).dp) // 240 fits inside 300 dp (tweak if needed)
//                                        .size(56.dp)
//                                        .clip(CircleShape)
//                                        .background(Color.Blue)
//                                        .alpha(if (uiState.activeLeg == "left") blinkAlpha else 1f)
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(8.dp))
//                    }
//                }
//
//                // Right column (50% width)
//                Box(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Column(
//                        modifier = Modifier.fillMaxHeight(),
//                        verticalArrangement = Arrangement.SpaceBetween,
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Spacer(modifier = Modifier.height(8.dp))
//
//                        val rightYFraction = uiState.rightBallPosition.coerceIn(0f, 1f)
//                        Box(
//                            modifier = Modifier.fillMaxWidth(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Box(modifier = Modifier
//                                .fillMaxWidth()
//                                .height(300.dp)
//                            ) {
//                                Box(
//                                    modifier = Modifier
//                                        .offset(y = (rightYFraction * 240).dp)
//                                        .size(56.dp)
//                                        .clip(CircleShape)
//                                        .background(Color.Red)
//                                        .alpha(if (uiState.activeLeg == "right") blinkAlpha else 1f)
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(8.dp))
//                    }
//                }
//            }
//
//            // Bottom controls & results
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.BottomCenter)
//                    .padding(16.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                ResultsDisplay(results = uiState.results)
//                Spacer(Modifier.height(12.dp))
//                TestControls(
//                    testState = uiState.testState,
//                    onCalibrationAction = {
//                        Log.d("GaitTestScreen", "Capture clicked - state=${uiState.testState}")
//                        viewModel.onCalibrationAction()
//                    },
//                    onStartTest = {
//                        Log.d("GaitTestScreen", "Start clicked - state=${uiState.testState}")
//                        viewModel.startTest()
//                    },
//                    onStopTest = {
//                        Log.d("GaitTestScreen", "Stop clicked - state=${uiState.testState}")
//                        viewModel.stopTest()
//                    }
//                )
//            }
//
//            // Calibration overlay
//            CalibrationOverlay(
//                testState = uiState.testState,
//                onCapture = {
//                    Log.d("GaitTestScreen", "Overlay Capture clicked - state=${uiState.testState}")
//                    viewModel.onCalibrationAction()
//                }
//            )
//        }
//    }
//}
//
//@Composable
//private fun TestControls(
//    testState: TestState,
//    onCalibrationAction: () -> Unit,
//    onStartTest: () -> Unit,
//    onStopTest: () -> Unit
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.Center
//    ) {
//        when (testState) {
//            TestState.IDLE, TestState.FINISHED -> {
//                Button(onClick = onCalibrationAction) {
//                    Icon(Icons.Default.Tune, contentDescription = null)
//                    Spacer(Modifier.width(8.dp))
//                    Text(if (testState == TestState.FINISHED) "Recalibrate" else "Start Calibration")
//                }
//            }
//            TestState.READY -> {
//                Button(onClick = onStartTest) {
//                    Icon(Icons.Default.PlayArrow, contentDescription = null)
//                    Spacer(Modifier.width(8.dp))
//                    Text("Start Test")
//                }
//            }
//            TestState.RUNNING -> {
//                Button(
//                    onClick = onStopTest,
//                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//                ) {
//                    Icon(Icons.Default.Stop, contentDescription = null)
//                    Spacer(Modifier.width(8.dp))
//                    Text("Stop Test")
//                }
//            }
//            else -> {}
//        }
//    }
//}
//
//@Composable
//private fun CalibrationOverlay(
//    testState: TestState,
//    onCapture: () -> Unit
//) {
//    val isVisible = when (testState) {
//        TestState.CALIBRATING_L_SURFACE,
//        TestState.CALIBRATING_L_PEAK,
//        TestState.CALIBRATING_R_SURFACE,
//        TestState.CALIBRATING_R_PEAK,
//        TestState.READY -> true
//        else -> false
//    }
//
//    val message = when (testState) {
//        TestState.CALIBRATING_L_SURFACE -> "Place LEFT foot on the ground and press 'Capture'."
//        TestState.CALIBRATING_L_PEAK -> "Lift LEFT leg to its highest point and press 'Capture'."
//        TestState.CALIBRATING_R_SURFACE -> "Place RIGHT foot on the ground and press 'Capture'."
//        TestState.CALIBRATING_R_PEAK -> "Lift RIGHT leg to its highest point and press 'Capture'."
//        TestState.READY -> "Calibration Complete! Press Start to begin the test."
//        else -> null
//    }
//
//    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.8f)),
//            contentAlignment = Alignment.Center
//        ) {
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.padding(32.dp)
//            ) {
//                if (message != null) {
//                    Text(
//                        text = message,
//                        color = Color.White,
//                        fontSize = 22.sp,
//                        textAlign = TextAlign.Center,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//                Spacer(Modifier.height(32.dp))
//                if (testState != TestState.READY && testState != TestState.FINISHED) {
//                    Button(
//                        onClick = onCapture,
//                        modifier = Modifier.size(100.dp),
//                        shape = CircleShape
//                    ) {
//                        Text("Capture", fontSize = 18.sp, textAlign = TextAlign.Center)
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun ResultsDisplay(results: List<String>) {
//    AnimatedVisibility(visible = results.isNotEmpty()) {
//        Card(modifier = Modifier.fillMaxWidth()) {
//            LazyColumn(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .heightIn(max = 150.dp)
//            ) {
//                items(results) { result ->
//                    Text(result)
//                    Divider(modifier = Modifier.padding(vertical = 4.dp))
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun GaitCanvas(leftBallY: Float, rightBallY: Float) {
//    val goalColor = Color.Green.copy(alpha = 0.5f)
//    val startColor = MaterialTheme.colorScheme.surfaceVariant
//    val density = LocalDensity.current
//
//    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
//
//        val circleSize = maxWidth * 0.25f
//        val strokeWidthPx = with(density) { 4.dp.toPx() }
//
//        // Left goal
//        Box(
//            modifier = Modifier
//                .fillMaxHeight()
//                .fillMaxWidth(0.5f)
//                .align(Alignment.CenterStart)
//                .padding(16.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Canvas(modifier = Modifier.size(circleSize)) {
//                drawCircle(color = goalColor, style = Stroke(width = strokeWidthPx))
//            }
//        }
//
//        // Right goal
//        Box(
//            modifier = Modifier
//                .fillMaxHeight()
//                .fillMaxWidth(0.5f)
//                .align(Alignment.CenterEnd)
//                .padding(16.dp),
//            contentAlignment = Alignment.Center
//        ) {
//            Canvas(modifier = Modifier.size(circleSize)) {
//                drawCircle(color = goalColor, style = Stroke(width = strokeWidthPx))
//            }
//        }
//    }
//}
package com.example.stride.ui.features.gait_test

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaitTestScreen(
    onNavigateUp: () -> Unit,
    viewModel: GaitTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gait Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Main layout for the two movement lanes
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Lane
                GaitLane(
                    modifier = Modifier.weight(1f),
                    ballPositionFraction = uiState.leftBallPosition,
                    ballColor = Color.Blue,
                    isBlinking = uiState.activeLeg == "left"
                )
                // Right Lane
                GaitLane(
                    modifier = Modifier.weight(1f),
                    ballPositionFraction = uiState.rightBallPosition,
                    ballColor = Color.Red,
                    isBlinking = uiState.activeLeg == "right"
                )
            }

            // Bottom controls & results, aligned to the bottom of the screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ResultsDisplay(results = uiState.results)
                Spacer(Modifier.height(12.dp))
                TestControls(
                    testState = uiState.testState,
                    onCalibrationAction = { viewModel.onCalibrationAction() },
                    onStartTest = { viewModel.startTest() },
                    onStopTest = { viewModel.stopTest() }
                )
            }

            // Full-screen calibration overlay
            CalibrationOverlay(
                testState = uiState.testState,
                onCapture = { viewModel.onCalibrationAction() }
            )
        }
    }
}

/**
 * A reusable composable that renders a single vertical lane, including the
 * top/bottom goal circles and the moving ball. It is fully responsive.
 */
@Composable
private fun GaitLane(
    modifier: Modifier = Modifier,
    ballPositionFraction: Float, // 0.0 (top) to 1.0 (bottom)
    ballColor: Color,
    isBlinking: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    val ballSize = 56.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.TopCenter // Align children to the top
    ) {
        val laneHeight = maxHeight
        val laneWidth = maxWidth
        val goalRadius = (laneWidth * 0.4f).coerceAtMost(80.dp) // Responsive radius

        // Draw goal circles using a Canvas that fills the lane
        GoalMarkers(
            peakGoalColor = Color.Green.copy(alpha = 0.5f),
            surfaceGoalColor = MaterialTheme.colorScheme.surfaceVariant,
            goalRadius = goalRadius
        )

        // Calculate the total vertical distance the ball can travel
        val travelDistance = laneHeight - ballSize

        // The Y offset is the fraction of the total travel distance
        val yOffset = travelDistance * ballPositionFraction

        // The moving ball itself
        Box(
            modifier = Modifier
                .offset(y = yOffset) // Apply the calculated offset from the top
                .size(ballSize)
                .clip(CircleShape)
                .background(ballColor)
                .alpha(if (isBlinking) blinkAlpha else 1.0f)
        )
    }
}

/**
 * Draws the top (peak) and bottom (surface) goal circles within a lane.
 */
@Composable
private fun BoxScope.GoalMarkers(
    peakGoalColor: Color,
    surfaceGoalColor: Color,
    goalRadius: Dp
) {
    val strokeWidth = 4.dp
    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radiusPx = with(density) { goalRadius.toPx() }
        val strokeWidthPx = with(density) { strokeWidth.toPx() }

        // Top "Peak" Goal Circle
        drawCircle(
            color = peakGoalColor,
            radius = radiusPx,
            center = Offset(x = size.width / 2, y = goalRadius.toPx()),
            style = Stroke(width = strokeWidthPx)
        )

        // Bottom "Surface" Goal Circle
        drawCircle(
            color = surfaceGoalColor,
            radius = radiusPx,
            center = Offset(x = size.width / 2, y = size.height - goalRadius.toPx()),
            style = Stroke(width = strokeWidthPx)
        )
    }
}


@Composable
private fun TestControls(
    testState: TestState,
    onCalibrationAction: () -> Unit,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        when (testState) {
            TestState.IDLE, TestState.FINISHED -> {
                Button(onClick = onCalibrationAction) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (testState == TestState.FINISHED) "Recalibrate" else "Start Calibration")
                }
            }
            TestState.READY -> {
                Button(onClick = onStartTest) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Test")
                }
            }
            TestState.RUNNING -> {
                Button(
                    onClick = onStopTest,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Test")
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun CalibrationOverlay(
    testState: TestState,
    onCapture: () -> Unit
) {
    val isVisible = testState in listOf(
        TestState.CALIBRATING_L_SURFACE,
        TestState.CALIBRATING_L_PEAK,
        TestState.CALIBRATING_R_SURFACE,
        TestState.CALIBRATING_R_PEAK,
        TestState.READY
    )

    val message = when (testState) {
        TestState.CALIBRATING_L_SURFACE -> "Place LEFT foot on the ground and press 'Capture'."
        TestState.CALIBRATING_L_PEAK -> "Lift LEFT leg to its highest point and press 'Capture'."
        TestState.CALIBRATING_R_SURFACE -> "Place RIGHT foot on the ground and press 'Capture'."
        TestState.CALIBRATING_R_PEAK -> "Lift RIGHT leg to its highest point and press 'Capture'."
        TestState.READY -> "Calibration Complete!\nPress Start to begin the test."
        else -> null
    }

    AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                if (message != null) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp
                    )
                }
                Spacer(Modifier.height(32.dp))
                if (testState != TestState.READY) {
                    Button(
                        onClick = onCapture,
                        modifier = Modifier.size(120.dp), // Increased size for better touch
                        shape = CircleShape
                    ) {
                        Text("Capture", fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsDisplay(results: List<String>) {
    AnimatedVisibility(visible = results.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 150.dp)
            ) {
                items(results) { result ->
                    Text(result)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}