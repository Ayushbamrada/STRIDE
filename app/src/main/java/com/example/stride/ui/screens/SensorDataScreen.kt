package com.example.stride.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stride.bluetooth.ConnectionState
import com.example.stride.ui.viewmodel.SensorViewModel
import com.example.stride.ui.viewmodel.SensorViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDataScreen() {
    val context = LocalContext.current
    // Use the updated factory
    val viewModel: SensorViewModel = viewModel(factory = SensorViewModelFactory(context))

    val connectionState by viewModel.connectionState.collectAsState()
    val commandText by viewModel.commandText.collectAsState()
    val ackMessage by viewModel.ackMessage.collectAsState()

    // State for the bottom sheet
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // State for the list of paired devices
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    val bluetoothAdapter: BluetoothAdapter? = remember { BluetoothAdapter.getDefaultAdapter() }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Bluetooth Permission Handling ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission is granted, you can now access paired devices
            } else {
                // Handle the case where the user denies the permission
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Bluetooth permission is required to connect.")
                }
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    // Function to update the paired devices list (requires permission)
    @SuppressLint("MissingPermission")
    fun updatePairedDevices() {
        pairedDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    LaunchedEffect(ackMessage) {
        ackMessage?.let { msg ->
            snackbarHostState.showSnackbar("Device: $msg")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sensor Dashboard", fontWeight = FontWeight.Medium) },
                actions = {
                    if (connectionState == ConnectionState.Connected) {
                        TextButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .animateContentSize(), // Animate screen changes
            contentAlignment = Alignment.Center
        ) {
            // --- Show controls when connected ---
            if (connectionState == ConnectionState.Connected) {
                val vibTime by viewModel.vibTime.collectAsState()
                val vibTimeGap by viewModel.vibTimeGap.collectAsState()
                val vibCount by viewModel.vibCount.collectAsState()
                val delayToStartVib by viewModel.delayToStartVib.collectAsState()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        DeviceControlsCard(
                            commandText = commandText,
                            onCommandTextChanged = viewModel::onCommandTextChanged,
                            onSendCommand = viewModel::sendCommand,
                            onCalibrate = viewModel::calibrate,
                        )
                    }
                    item {
                        ParameterControlCard(
                            sliderValues = listOf(vibTime, vibTimeGap, vibCount, delayToStartVib),
                            onValueChange = { index, value ->
                                when (index) {
                                    0 -> viewModel.onVibTimeChanged(value)
                                    1 -> viewModel.onVibTimeGapChanged(value)
                                    2 -> viewModel.onVibCountChanged(value)
                                    3 -> viewModel.onVibDelayChanged(value)
                                }
                            },
                            onSave = { index, value ->
                                val (paramName, formattedValue) = when (index) {
                                    0 -> "vibtime" to "%.1f".format(value)
                                    1 -> "vibtimegap" to "%.1f".format(value)
                                    2 -> "vibcount" to "%.0f".format(value)
                                    else -> "maxdelay" to "%.1f".format(value)
                                }
                                val command = if(index==2) "$paramName,${value.toInt()}" else "$paramName,${(value*1000).toInt()}"

                                viewModel.onCommandTextChanged(command)
                                viewModel.sendCommand()
                            }
                        )
                    }
                }
            }
            // --- Show connection UI when not connected ---
            else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (connectionState == ConnectionState.Connecting) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = "Disconnected",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Disconnected",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please connect to a paired device to continue.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                updatePairedDevices()
                                showBottomSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Connect to Device")
                        }

                    }
                }
            }
        }
    }

    // --- Modal Bottom Sheet for Device Selection ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Paired Devices",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                if (pairedDevices.isNotEmpty()) {
                    LazyColumn {
                        items(pairedDevices) { device ->
                            DeviceListItem(device = device) {
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        showBottomSheet = false
                                    }
                                }
                                viewModel.connect(device)
                            }
                        }
                    }
                } else {
                    Text(
                        "No paired devices found. Please pair a device in your phone's Bluetooth settings.",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// A simple composable for each device in the bottom sheet list
@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(device: BluetoothDevice, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(device.name ?: "Unknown Device") },
        supportingContent = { Text(device.address) },
        leadingContent = { Icon(Icons.Default.Bluetooth, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun DeviceControlsCard(
    commandText: String,
    onCommandTextChanged: (String) -> Unit,
    onSendCommand: () -> Unit,
    onCalibrate: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onCalibrate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Tune, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Calibrate Sensor")
            }
//            OutlinedTextField(
//                value = commandText,
//                onValueChange = onCommandTextChanged,
//                modifier = Modifier.fillMaxWidth(),
//                label = { Text("Custom Command") },
//                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) },
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
//                keyboardActions = KeyboardActions(onSend = {
//                    onSendCommand()
//                    keyboardController?.hide()
//                }),
//                singleLine = true
//            )
//            Button(onClick = onSendCommand, modifier = Modifier.fillMaxWidth()) {
//                Icon(Icons.Default.Send, contentDescription = null, Modifier.size(18.dp))
//                Spacer(Modifier.width(8.dp))
//                Text("Send Command")
//            }
        }
    }
}

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
                if (index == 2) {
                    ParameterSliderItem(
                        label = label,
                        description = description,
                        value = sliderValues[index],
                        onValueChange = { onValueChange(index, it) },
                        onSave = { onSave(index, sliderValues[index]) },
                        valueRange = 1f..10f, // Integer range
                        steps = 8, // Creates 9 steps for 10 values (1, 2, ..., 10)
                        valueFormat = "%.0f" // Format as a whole number
                    )
                }else if(index == 3){
                    ParameterSliderItem(
                        label = label,
                        description = description,
                        value = sliderValues[index],
                        onValueChange = { onValueChange(index, it) },
                        onSave = { onSave(index, sliderValues[index]) },
                        valueRange = 3f..5f, // Integer range
                        steps = 3,
                    )
                } else {
                    ParameterSliderItem(
                        label = label,
                        description = description,
                        value = sliderValues[index],
                        onValueChange = { onValueChange(index, it) },
                        onSave = { onSave(index, sliderValues[index]) }
                    )
                }
                if (index < paramLabels.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun ParameterSliderItem(
    label: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onSave: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0.5f..5.0f,
    steps: Int = 8,
    valueFormat: String = "%.1f"
) {
    var isBeingDragged by remember { mutableStateOf(false) }
    val valueColor by animateColorAsState(
        targetValue = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "value color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onValueChangeFinished = { isBeingDragged = false },
        interactionSource = remember { MutableInteractionSource() }.also { source ->
            LaunchedEffect(source) {
                source.interactions.collect { interaction ->
                    isBeingDragged = interaction is DragInteraction.Start
                }
            }
        }
    )
    Box(modifier = Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 12.dp), contentAlignment = Alignment.CenterEnd) {
        OutlinedIconButton(onClick = onSave) {
            Icon(Icons.Default.Save, contentDescription = "Save $label")
        }
    }
}