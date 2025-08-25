package com.example.stride.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stride.ui.screens.HomeScreen
import com.example.stride.ui.screens.SensorDataScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController)
        }
        composable(
            "sensorData/{deviceAddress}",
            arguments = listOf(navArgument("deviceAddress") { type = NavType.StringType })
        ) {
            val deviceAddress = it.arguments?.getString("deviceAddress") ?: ""
            SensorDataScreen(deviceAddress)
        }
    }
}
