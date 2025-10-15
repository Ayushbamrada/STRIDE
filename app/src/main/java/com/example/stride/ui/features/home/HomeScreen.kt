//package com.example.stride.ui.features.home
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.BluetoothConnected
//import androidx.compose.material.icons.filled.BluetoothDisabled
//import androidx.compose.material.icons.filled.Settings
//import androidx.compose.material.icons.filled.Timeline
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.example.stride.data.ble.ConnectionState
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun HomeScreen(
//    onNavigateToGaitTest: () -> Unit,
//    onNavigateToVibrationConfig: () -> Unit,
//    viewModel: HomeViewModel = hiltViewModel()
//) {
//    val connectionState by viewModel.connectionState.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("STRIDE Dashboard") },
//                actions = {
//                    val icon = if (connectionState == ConnectionState.Connected)
//                        Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = "Connection Status",
//                        modifier = Modifier.padding(end = 16.dp)
//                    )
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                "Welcome to STRIDE",
//                style = MaterialTheme.typography.headlineMedium,
//                textAlign = TextAlign.Center
//            )
//            Spacer(Modifier.height(8.dp))
//            Text(
//                "Select an option below to begin.",
//                style = MaterialTheme.typography.bodyLarge,
//                textAlign = TextAlign.Center,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Spacer(Modifier.height(48.dp))
//
//            FeatureButton(
//                icon = Icons.Default.Timeline,
//                title = "Start Gait Test",
//                description = "Analyze leg movement and timing.",
//                onClick = onNavigateToGaitTest
//            )
//
//            Spacer(Modifier.height(24.dp))
//
//            FeatureButton(
//                icon = Icons.Default.Settings,
//                title = "Vibration Settings",
//                description = "Configure device connection and vibration parameters.",
//                onClick = onNavigateToVibrationConfig
//            )
//        }
//    }
//}
//
//@Composable
//private fun FeatureButton(
//    icon: ImageVector,
//    title: String,
//    description: String,
//    onClick: () -> Unit
//) {
//    OutlinedButton(
//        onClick = onClick,
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp))
//            Spacer(Modifier.width(16.dp))
//            Column {
//                Text(title, style = MaterialTheme.typography.titleLarge)
//                Text(description, style = MaterialTheme.typography.bodyMedium)
//            }
//        }
//
//    }
//}

package com.example.stride.ui.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stride.data.model.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToGaitTest: () -> Unit,
    onNavigateToVibrationConfig: () -> Unit,
    onNavigateToCaveGame: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STRIDE Dashboard") },
                actions = {
                    val icon = if (connectionState == ConnectionState.CONNECTED) // THIS IS THE FIX
                        Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled
                    Icon(
                        imageVector = icon,
                        contentDescription = "Connection Status",
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome to STRIDE",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select an option below to begin.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(48.dp))

            FeatureButton(
                icon = Icons.Default.Timeline,
                title = "Start Gait Test",
                description = "Analyze leg movement and timing.",
                onClick = onNavigateToGaitTest
            )

            Spacer(Modifier.height(24.dp))

            FeatureButton(
                icon = Icons.Default.Timeline,
                title = "Start Game",
                description = "Play Game",
                onClick = onNavigateToCaveGame
            )

            Spacer(Modifier.height(24.dp))

            FeatureButton(
                icon = Icons.Default.Settings,
                title = "Vibration Settings",
                description = "Configure device connection and vibration parameters.",
                onClick = onNavigateToVibrationConfig
            )
        }
    }
}

@Composable
private fun FeatureButton(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


