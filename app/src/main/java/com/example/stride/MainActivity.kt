package com.example.stride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.stride.ui.navigation.AppNavHost
import com.example.stride.ui.theme.StrideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Marks this Activity to be set up for Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrideTheme {
                AppNavHost()
            }
        }
    }
}
