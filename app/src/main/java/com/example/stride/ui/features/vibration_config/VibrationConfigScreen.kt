package com.example.stride.ui.features.vibration_config

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.stride.data.model.ConnectionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibrationConfigScreen(
    onNavigateUp: () -> Unit,
    viewModel: VibrationConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.connectionState) {
        Log.d("VibScreen",uiState.connectionState.toString())
    }

    // --- Bluetooth Permission Handling ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("Bluetooth permission is required.")
            }
        }
    }
    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // --- THIS IS THE FIX ---
                // The title now changes based on the connection state.
                title = {
                    Text(
                        if (uiState.connectionState == ConnectionState.CONNECTED) {
                            "Vibration Settings"
                        } else {
                            "Connect to Device"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.connectionState == ConnectionState.CONNECTED) {
                        TextButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).animateContentSize(),
            contentAlignment = Alignment.Center
        ) {
            when (uiState.connectionState) {
                ConnectionState.CONNECTED -> ConnectedContent(viewModel = viewModel)
                ConnectionState.CONNECTING -> ConnectingContent()
                ConnectionState.DISCONNECTED -> DisconnectedContent(
                    onConnectClick = {
                        viewModel.updatePairedDevices()
                        showBottomSheet = true
                    }
                )
            }
        }
    }

    if (showBottomSheet) {
        DeviceSelectionBottomSheet(
            sheetState = sheetState,
            pairedDevices = uiState.pairedDevices,
            onDismiss = { showBottomSheet = false },
            onDeviceSelected = { device ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) showBottomSheet = false
                }
                viewModel.connect(device)
            }
        )
    }
}


@Composable
private fun ConnectedContent(viewModel: VibrationConfigViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Card(
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Button(
                    onClick = { viewModel.calibrate() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Calibrate Sensor")
                }
            }
        }
        item {
            ParameterControlCard(
                sliderValues = listOf(uiState.vibTime, uiState.vibTimeGap, uiState.vibCount, uiState.delayToStartVib),
                onValueChange = { index, value ->
                    when (index) {
                        0 -> viewModel.onVibTimeChanged(value)
                        1 -> viewModel.onVibTimeGapChanged(value)
                        2 -> viewModel.onVibCountChanged(value)
                        3 -> viewModel.onVibDelayChanged(value)
                    }
                },
                onSave = { index, value -> viewModel.onSaveParameter(index, value) }
            )
        }
    }
}

// THIS IS THE NEW, DETAILED CARD THAT FIXES YOUR UI PROBLEM
@Composable
private fun ParameterControlCard(
    sliderValues: List<Float>,
    onValueChange: (index: Int, value: Float) -> Unit,
    onSave: (index: Int, value: Float) -> Unit
) {
    val paramLabels = listOf(
        "Vibration Time (Seconds)" to "How long the motor vibrates.",
        "Vibration Gap (Seconds)" to "The delay between each vibration.",
        "Number of Vibrations" to "How many times the motor vibrates.",
        "Initial Delay (Seconds)" to "How long to wait before starting."
    )

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Text(
                "Vibration Configuration",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            paramLabels.forEachIndexed { index, (label, description) ->
                // Custom configuration for the "Number of Vibrations" slider
                val valueRange: ClosedFloatingPointRange<Float>
                val steps: Int
                val valueFormat: String

                when (index) {
                    2 -> { // Number of Vibrations
                        valueRange = 1f..10f
                        steps = 8
                        valueFormat = "%.0f"
                    }
                    3 -> { // Initial Delay
                        valueRange = 3f..5f
                        steps = 3
                        valueFormat = "%.1f"
                    }
                    else -> { // Time-based sliders
                        valueRange = 0.5f..5.0f
                        steps = 8
                        valueFormat = "%.1f"
                    }
                }

                ParameterSliderItem(
                    label = label,
                    description = description,
                    value = sliderValues[index],
                    onValueChange = { onValueChange(index, it) },
                    onSave = { onSave(index, sliderValues[index]) },
                    valueRange = valueRange,
                    steps = steps,
                    valueFormat = valueFormat
                )

                if (index < paramLabels.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// THIS IS THE HELPER COMPOSABLE FOR EACH SLIDER, RESTORED FROM YOUR OLD CODE
@Composable
private fun ParameterSliderItem(
    label: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onSave: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormat: String
) {
    var isBeingDragged by remember { mutableStateOf(false) }
    val valueColor by animateColorAsState(
        targetValue = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "value color"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = String.format(valueFormat, value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = Modifier.width(60.dp)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            onValueChangeFinished = { isBeingDragged = false },
            interactionSource = remember { MutableInteractionSource() }.also { source ->
                LaunchedEffect(source) {
                    source.interactions.collect { interaction ->
                        isBeingDragged = interaction is DragInteraction.Start
                    }
                }
            }
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            OutlinedIconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = "Save $label")
            }
        }
    }
}


@Composable
private fun ConnectingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connecting...")
    }
}

@Composable
private fun DisconnectedContent(onConnectClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothDisabled,
            contentDescription = "Disconnected",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("No Device Connected", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Please connect to a paired STRIDE device to continue. Once connected, you will be able to configure vibration settings.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConnectClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Link, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Connect to Device")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelectionBottomSheet(
    sheetState: SheetState,
    pairedDevices: List<BluetoothDevice>,
    onDismiss: () -> Unit,
    onDeviceSelected: (BluetoothDevice) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text("Paired Devices", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            if (pairedDevices.isNotEmpty()) {
                LazyColumn {
                    items(pairedDevices) { device ->
                        DeviceListItem(device = device, onClick = { onDeviceSelected(device) })
                    }
                }
            } else {
                Text(
                    "No paired devices found. Please pair in system settings.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DeviceListItem(device: BluetoothDevice, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name ?: "Unknown Device") },
        supportingContent = { Text(device.address) },
        leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )

}

