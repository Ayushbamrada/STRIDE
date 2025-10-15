//package com.example.stride.ui.navigation
//
//import androidx.compose.runtime.Composable
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.stride.ui.features.gait_test.GaitTestScreen
//import com.example.stride.ui.features.home.HomeScreen
//import com.example.stride.ui.features.vibration_config.VibrationConfigScreen
////import com.example.stride.ui.screens.
//
//@Composable
//fun AppNavHost() {
//    val navController = rememberNavController()
//    NavHost(navController = navController, startDestination = "home") {
//        composable("home") {
//            HomeScreen(
//                onNavigateToGaitTest = { navController.navigate("gait_test") },
//                onNavigateToVibrationConfig = { navController.navigate("vibration_config") },
//                onNavigateToCaveGame = { navController.navigate("cave_game") }
//            )
//        }
//        composable("gait_test") {
//            GaitTestScreen(onNavigateUp = { navController.popBackStack() })
//        }
//        composable("vibration_config") {
//            VibrationConfigScreen(onNavigateUp = { navController.popBackStack() })
//        }
////        composable("cave_game") {
////            SensorCaveGame()
////        }
//    }
//}
package com.example.stride.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.stride.ui.features.cave_game.SensorCaveGame
import com.example.stride.ui.features.gait_test.GaitTestScreen
import com.example.stride.ui.features.home.HomeScreen
import com.example.stride.ui.features.vibration_config.VibrationConfigScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToGaitTest = { navController.navigate("gait_test") },
                onNavigateToVibrationConfig = { navController.navigate("vibration_config") },
                onNavigateToCaveGame = { navController.navigate("cave_game") }
            )
        }
        composable("gait_test") {
            GaitTestScreen(onNavigateUp = { navController.popBackStack() })
        }
        composable("vibration_config") {
            VibrationConfigScreen(onNavigateUp = { navController.popBackStack() })
        }
        // --- THIS IS THE NEW NAVIGATION ROUTE ---
        composable("cave_game") {
            SensorCaveGame()
        }
    }
}

