package com.example.stride.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import com.example.stride.ui.screens.SensorDataScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "home") {
        composable("home") {
            SensorDataScreen()
        }

    }
}
