//package com.example.stride.ui.features.cave_game
//
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Rect
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.example.stride.ui.features.gait_test.GaitTestViewModel
//import com.example.stride.ui.features.gait_test.TestState
//import kotlinx.coroutines.delay
//import kotlin.random.Random
//
//// --- Constants for game appearance ---
//private const val PLAYER_SIZE_DP = 24f
//private const val OBSTACLE_WIDTH_DP = 50f
//private const val OBSTACLE_MIN_HEIGHT_DP = 150f
//private const val OBSTACLE_MAX_HEIGHT_DP = 400f
//private const val OBSTACLE_SPEED = -7f // How fast obstacles move left
//private const val OBSTACLE_SPAWN_INTERVAL_MS = 1500L
//
//// --- Data classes for game state ---
//private data class Player(val y: Float)
//private data class Obstacle(val x: Float, val height: Float, val type: ObstacleType, var passed: Boolean = false)
//private enum class ObstacleType { TOP, BOTTOM }
//
//
//@Composable
//fun SensorCaveGame(
//    viewModel: GaitTestViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//
//    // Pass the relevant state and functions from the ViewModel to the game screen
//    GameScreen(
//        testState = uiState.testState,
//        sensorValue = uiState.leftBallY, // We use the normalized Y value of the left leg
//        startTest = { viewModel.startTest() },
//        stopTest = { viewModel.stopTest() }
//    )
//}
//
//@Composable
//private fun GameScreen(
//    testState: TestState,
//    sensorValue: Float,
//    startTest: () -> Unit,
//    stopTest: () -> Unit
//) {
//    var player by remember { mutableStateOf(Player(y = 0f)) }
//    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
//    var score by remember { mutableStateOf(0) }
//    var lastObstacleSpawnTime by remember { mutableStateOf(0L) }
//
//    val density = LocalDensity.current
//
//    // Reset game elements when the test restarts
//    LaunchedEffect(testState) {
//        if (testState == TestState.IDLE || testState == TestState.FINISHED) {
//            obstacles = emptyList()
//            score = 0
//            lastObstacleSpawnTime = 0L
//        }
//    }
//
//    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
//        val screenHeightPx = with(density) { maxHeight.toPx() }
//        val screenWidthPx = with(density) { maxWidth.toPx() }
//
//        // Initialize player position once screen dimensions are known
//        LaunchedEffect(screenHeightPx) {
//            if (player.y == 0f) {
//                player = player.copy(y = screenHeightPx / 2)
//            }
//        }
//
//        // --- The main game loop ---
//        LaunchedEffect(testState, sensorValue) {
//            while (testState == TestState.RUNNING) {
//                if (screenHeightPx > 0 && screenWidthPx > 0) {
//                    val playerSize = with(density) { PLAYER_SIZE_DP.dp.toPx() }
//                    val obstacleWidth = with(density) { OBSTACLE_WIDTH_DP.dp.toPx() }
//
//                    // 1. Update Player Position
//                    val newY = sensorValue * (screenHeightPx - playerSize)
//                    player = player.copy(y = newY)
//
//                    // 2. Move Obstacles and check for scoring
//                    val updatedObstacles = obstacles.map {
//                        val newX = it.x + OBSTACLE_SPEED
//                        // Check if player passed the obstacle
//                        if (!it.passed && (screenWidthPx / 2) > (newX + obstacleWidth)) {
//                            score++
//                            it.copy(x = newX, passed = true)
//                        } else {
//                            it.copy(x = newX)
//                        }
//                    }.filter { it.x > -obstacleWidth } // Remove obstacles that are off-screen
//
//                    // 3. Spawn New Obstacles
//                    val currentTime = System.currentTimeMillis()
//                    if (currentTime - lastObstacleSpawnTime > OBSTACLE_SPAWN_INTERVAL_MS) {
//                        val newObstacleType = if (Random.nextBoolean()) ObstacleType.TOP else ObstacleType.BOTTOM
//                        val minHeight = with(density) { OBSTACLE_MIN_HEIGHT_DP.dp.toPx() }
//                        val maxHeight = with(density) { OBSTACLE_MAX_HEIGHT_DP.dp.toPx() }
//                        val newObstacle = Obstacle(
//                            x = screenWidthPx,
//                            height = Random.nextFloat() * (maxHeight - minHeight) + minHeight,
//                            type = newObstacleType
//                        )
//                        obstacles = updatedObstacles + newObstacle
//                        lastObstacleSpawnTime = currentTime
//                    } else {
//                        obstacles = updatedObstacles
//                    }
//
//                    // 4. Check for Collisions
//                    val playerRect = Rect(
//                        left = screenWidthPx / 2 - playerSize / 2,
//                        top = player.y,
//                        right = screenWidthPx / 2 + playerSize / 2,
//                        bottom = player.y + playerSize
//                    )
//
//                    obstacles.forEach { obstacle ->
//                        val obstacleRect = when (obstacle.type) {
//                            ObstacleType.TOP -> Rect(obstacle.x, 0f, obstacle.x + obstacleWidth, obstacle.height)
//                            ObstacleType.BOTTOM -> Rect(obstacle.x, screenHeightPx - obstacle.height, obstacle.x + obstacleWidth, screenHeightPx)
//                        }
//                        if (playerRect.overlaps(obstacleRect)) {
//                            stopTest()
//                        }
//                    }
//                }
//                delay(16) // Aim for ~60 FPS
//            }
//        }
//
//        GameCanvas(player, obstacles, screenWidthPx, screenHeightPx)
//
//        GameUI(
//            testState = testState,
//            score = score,
//            onRestart = startTest
//        )
//    }
//}
//
//@Composable
//private fun GameCanvas(player: Player, obstacles: List<Obstacle>, screenWidth: Float, screenHeight: Float) {
//    val playerSize = with(LocalDensity.current) { PLAYER_SIZE_DP.dp.toPx() }
//    val obstacleWidth = with(LocalDensity.current) { OBSTACLE_WIDTH_DP.dp.toPx() }
//
//    Canvas(modifier = Modifier.fillMaxSize()) {
//        drawRect(color = Color(0xFF2C3E50))
//
//        obstacles.forEach { obstacle ->
//            val path = Path()
//            when (obstacle.type) {
//                ObstacleType.TOP -> {
//                    path.moveTo(obstacle.x, 0f)
//                    path.lineTo(obstacle.x + obstacleWidth, 0f)
//                    path.lineTo(obstacle.x + obstacleWidth / 2, obstacle.height)
//                    path.close()
//                }
//                ObstacleType.BOTTOM -> {
//                    path.moveTo(obstacle.x, screenHeight)
//                    path.lineTo(obstacle.x + obstacleWidth, screenHeight)
//                    path.lineTo(obstacle.x + obstacleWidth / 2, screenHeight - obstacle.height)
//                    path.close()
//                }
//            }
//            drawPath(path = path, color = Color(0xFF1ABC9C))
//        }
//
//        drawCircle(
//            color = Color(0xFFF1C40F),
//            radius = playerSize / 2,
//            center = Offset(screenWidth / 2, player.y + playerSize / 2)
//        )
//    }
//}
//
//@Composable
//private fun BoxScope.GameUI(
//    testState: TestState,
//    score: Int,
//    onRestart: () -> Unit
//) {
//    when (testState) {
//        TestState.IDLE, TestState.FINISHED -> {
//            Column(
//                modifier = Modifier.align(Alignment.Center),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                if (testState == TestState.FINISHED) {
//                    Text("Game Over", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
//                    Text("Score: $score", fontSize = 36.sp, color = Color.White)
//                } else {
//                    Text("Sensor Cave", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
//                    Text("Move sensor to avoid obstacles", fontSize = 20.sp, color = Color.White)
//                }
//                Spacer(modifier = Modifier.height(24.dp))
//                Button(
//                    onClick = onRestart,
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
//                ) {
//                    Text(if (testState == TestState.FINISHED) "Restart" else "Start Game", fontSize = 24.sp)
//                }
//            }
//        }
//        TestState.RUNNING -> {
//            Text(
//                text = score.toString(),
//                modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
//                style = TextStyle(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
//            )
//        }
//        // AUTO_CALIBRATING state is removed as it's no longer used
//        else -> {}
//    }
//}
//
package com.example.stride.ui.features.cave_game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stride.ui.features.gait_test.GaitTestViewModel
import com.example.stride.ui.features.gait_test.TestState
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- Constants for game appearance ---
private const val PLAYER_SIZE_DP = 24f
private const val OBSTACLE_WIDTH_DP = 50f
private const val OBSTACLE_MIN_HEIGHT_DP = 150f
private const val OBSTACLE_MAX_HEIGHT_DP = 400f
private const val OBSTACLE_SPEED = -7f // How fast obstacles move left
private const val OBSTACLE_SPAWN_INTERVAL_MS = 1500L

// --- Data classes for game state ---
private data class Player(val y: Float)
private data class Obstacle(val x: Float, val height: Float, val type: ObstacleType, var passed: Boolean = false)
private enum class ObstacleType { TOP, BOTTOM }


@Composable
fun SensorCaveGame(
    viewModel: GaitTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GameScreen(
        testState = uiState.testState,
        // --- THIS IS THE FIX ---
        // We now use the correct variable name: leftBallPosition
        sensorValue = uiState.leftBallPosition,
        startTest = { viewModel.startTest() },
        stopTest = { viewModel.stopTest() }
    )
}

@Composable
private fun GameScreen(
    testState: TestState,
    sensorValue: Float,
    startTest: () -> Unit,
    stopTest: () -> Unit
) {
    var player by remember { mutableStateOf(Player(y = 0f)) }
    var obstacles by remember { mutableStateOf(listOf<Obstacle>()) }
    var score by remember { mutableStateOf(0) }
    var lastObstacleSpawnTime by remember { mutableStateOf(0L) }

    val density = LocalDensity.current

    LaunchedEffect(testState) {
        if (testState == TestState.IDLE || testState == TestState.FINISHED) {
            obstacles = emptyList()
            score = 0
            lastObstacleSpawnTime = 0L
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }

        LaunchedEffect(screenHeightPx) {
            if (player.y == 0f) {
                player = player.copy(y = screenHeightPx / 2)
            }
        }

        LaunchedEffect(testState, sensorValue) {
            while (testState == TestState.RUNNING) {
                if (screenHeightPx > 0 && screenWidthPx > 0) {
                    val playerSize = with(density) { PLAYER_SIZE_DP.dp.toPx() }
                    val obstacleWidth = with(density) { OBSTACLE_WIDTH_DP.dp.toPx() }

                    val newY = sensorValue * (screenHeightPx - playerSize)
                    player = player.copy(y = newY)

                    val updatedObstacles = obstacles.map {
                        val newX = it.x + OBSTACLE_SPEED
                        if (!it.passed && (screenWidthPx / 2) > (newX + obstacleWidth)) {
                            score++
                            it.copy(x = newX, passed = true)
                        } else {
                            it.copy(x = newX)
                        }
                    }.filter { it.x > -obstacleWidth }

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastObstacleSpawnTime > OBSTACLE_SPAWN_INTERVAL_MS) {
                        val newObstacleType = if (Random.nextBoolean()) ObstacleType.TOP else ObstacleType.BOTTOM
                        val minHeight = with(density) { OBSTACLE_MIN_HEIGHT_DP.dp.toPx() }
                        val maxHeight = with(density) { OBSTACLE_MAX_HEIGHT_DP.dp.toPx() }
                        val newObstacle = Obstacle(
                            x = screenWidthPx,
                            height = Random.nextFloat() * (maxHeight - minHeight) + minHeight,
                            type = newObstacleType
                        )
                        obstacles = updatedObstacles + newObstacle
                        lastObstacleSpawnTime = currentTime
                    } else {
                        obstacles = updatedObstacles
                    }

                    val playerRect = Rect(
                        left = screenWidthPx / 2 - playerSize / 2,
                        top = player.y,
                        right = screenWidthPx / 2 + playerSize / 2,
                        bottom = player.y + playerSize
                    )

                    obstacles.forEach { obstacle ->
                        val obstacleRect = when (obstacle.type) {
                            ObstacleType.TOP -> Rect(obstacle.x, 0f, obstacle.x + obstacleWidth, obstacle.height)
                            ObstacleType.BOTTOM -> Rect(obstacle.x, screenHeightPx - obstacle.height, obstacle.x + obstacleWidth, screenHeightPx)
                        }
                        if (playerRect.overlaps(obstacleRect)) {
                            stopTest()
                        }
                    }
                }
                delay(16)
            }
        }

        GameCanvas(player, obstacles, screenWidthPx, screenHeightPx)

        GameUI(
            testState = testState,
            score = score,
            onRestart = startTest
        )
    }
}

@Composable
private fun GameCanvas(player: Player, obstacles: List<Obstacle>, screenWidth: Float, screenHeight: Float) {
    val playerSize = with(LocalDensity.current) { PLAYER_SIZE_DP.dp.toPx() }
    val obstacleWidth = with(LocalDensity.current) { OBSTACLE_WIDTH_DP.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color(0xFF2C3E50))

        obstacles.forEach { obstacle ->
            val path = Path()
            when (obstacle.type) {
                ObstacleType.TOP -> {
                    path.moveTo(obstacle.x, 0f)
                    path.lineTo(obstacle.x + obstacleWidth, 0f)
                    path.lineTo(obstacle.x + obstacleWidth / 2, obstacle.height)
                    path.close()
                }
                ObstacleType.BOTTOM -> {
                    path.moveTo(obstacle.x, screenHeight)
                    path.lineTo(obstacle.x + obstacleWidth, screenHeight)
                    path.lineTo(obstacle.x + obstacleWidth / 2, screenHeight - obstacle.height)
                    path.close()
                }
            }
            drawPath(path = path, color = Color(0xFF1ABC9C))
        }

        drawCircle(
            color = Color(0xFFF1C40F),
            radius = playerSize / 2,
            center = Offset(screenWidth / 2, player.y + playerSize / 2)
        )
    }
}

@Composable
private fun BoxScope.GameUI(
    testState: TestState,
    score: Int,
    onRestart: () -> Unit
) {
    // This UI is now simplified
    when (testState) {
        TestState.IDLE, TestState.FINISHED -> {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (testState == TestState.FINISHED) {
                    Text("Game Over", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Score: $score", fontSize = 36.sp, color = Color.White)
                } else {
                    Text("Sensor Cave", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Move your leg to avoid the obstacles", fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                ) {
                    Text(if (testState == TestState.FINISHED) "Restart" else "Start Game", fontSize = 24.sp)
                }
            }
        }
        TestState.RUNNING -> {
            Text(
                text = score.toString(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
                style = TextStyle(color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            )
        }
        else -> {
            // All other states (like calibration) are handled in the Gait Test screen
            // and do not show an overlay here.
        }
    }
}

