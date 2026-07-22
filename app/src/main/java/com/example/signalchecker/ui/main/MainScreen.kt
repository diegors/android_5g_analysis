package com.example.signalchecker.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalchecker.data.SignalData
import com.example.signalchecker.data.CellEntry
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel(),
    onNavigateToWifi: () -> Unit = {}
) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val currentSignal by viewModel.currentSignal.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    var intervalText by remember { mutableStateOf("15") }
    var plainTextPreview by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cellular Signal Checker") },
                actions = {
                    IconButton(onClick = {
                        val exportResult = viewModel.exportCsvToDownloads(context)
                        if (exportResult.isSuccess) {
                            Toast.makeText(
                                context,
                                "Saved ${exportResult.getOrNull()} to Downloads",
                                Toast.LENGTH_LONG
                            ).show()
                            if (!viewModel.hasCsvViewer(context)) {
                                plainTextPreview = viewModel.getCsvData()
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Export failed: ${exportResult.exceptionOrNull()?.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Signal", style = MaterialTheme.typography.titleMedium)
                    currentSignal?.let {
                        val tsFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()) }
                        SignalInfoRow("Timestamp", tsFormat.format(java.util.Date(it.timestamp)))
                        SignalInfoRow("Network", it.networkType)
                        SignalInfoRow("Registered", if (it.registered) "Yes" else "No")
                        SignalInfoRow("Level", it.level?.toString() ?: "N/A")
                        SignalInfoRow("DBm", it.dbm?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("ASU", it.asu?.toString() ?: "N/A")
                        SignalInfoRow("RSRP", it.nrRsrp?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("RSRQ", it.nrRsrq?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("SINR", it.nrSinr?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("RSRP", it.lteRsrp?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("RSRQ", it.lteRsrq?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("SINR", it.lteSinr?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("RSSI", it.rssi?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("Timing Advance", it.timingAdvance?.toString() ?: "N/A")
                        SignalInfoRow("CI", it.ci?.toString() ?: "N/A")
                        SignalInfoRow("PCI", it.pci?.toString() ?: "N/A")
                        SignalInfoRow("TAC", it.tac?.toString() ?: "N/A")
                        SignalInfoRow("MCC", it.mcc ?: "N/A")
                        SignalInfoRow("MNC", it.mnc ?: "N/A")
                        SignalInfoRow("Bands", it.bands.takeIf { b -> b.isNotEmpty() }?.joinToString(", ") ?: "N/A")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Location", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        SignalInfoRow("Latitude", it.latitude?.let { v -> "%.6f".format(v) } ?: "N/A")
                        SignalInfoRow("Longitude", it.longitude?.let { v -> "%.6f".format(v) } ?: "N/A")
                        SignalInfoRow("Accuracy", it.locationAccuracy?.let { v -> "%.1f m".format(v) } ?: "N/A")
                    } ?: Text("Detecting...")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { viewModel.captureAndAppendCurrentSignalToCsv() }) {
                            Text("Capture Signal")
                        }
                        Button(onClick = onNavigateToWifi) {
                            Text("View Wi-Fi Signal")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Monitoring Settings", style = MaterialTheme.typography.titleMedium)
            TextField(
                value = intervalText,
                onValueChange = { intervalText = it },
                label = { Text("Interval (Minutes, min 15)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                if (isMonitoring) {
                    Button(onClick = { viewModel.stopMonitoring() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Stop Background Monitoring")
                    }
                } else {
                    Button(onClick = {
                        viewModel.startMonitoring(intervalText.toLongOrNull() ?: 15)
                        viewModel.captureAndAppendCurrentSignalToCsv()
                    }) {
                        Text("Start Background Monitoring")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("History (Last 50 checks)", style = MaterialTheme.typography.titleMedium)
            HistoryTable(history = history)

            Spacer(modifier = Modifier.height(16.dp))

            currentSignal?.let { signal ->
                if (signal.allCells.isNotEmpty()) {
                    Text("All Detected Cells (${signal.allCells.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    AllCellsTable(cells = signal.allCells)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Divider()
            val currentTime = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()) }
            Text(
                text = "App Version 1.0.0 | Built: $currentTime",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (plainTextPreview != null) {
            AlertDialog(
                onDismissRequest = { plainTextPreview = null },
                title = { Text("CSV Preview (Plain Text)") },
                text = {
                    Text(
                        text = plainTextPreview ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    TextButton(onClick = { plainTextPreview = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryTable(history: List<SignalData>) {
    val horizontalScroll = rememberScrollState()
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
        ) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                TableCell("Timestamp", isHeader = true)
                TableCell("Network Type", isHeader = true)
                TableCell("RSRP (dBm)", isHeader = true)
                TableCell("SINR (dB)", isHeader = true)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(history) { data ->
                    val rsrp = data.nrRsrp ?: data.lteRsrp ?: data.rsrp
                    val sinr = data.nrSinr ?: data.lteSinr ?: data.sinr
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        TableCell(format.format(Date(data.timestamp)))
                        TableCell(data.networkType)
                        TableCell(rsrp?.toString() ?: "N/A")
                        TableCell(sinr?.toString() ?: "N/A")
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AllCellsTable(cells: List<CellEntry>) {
    val horizontalScroll = rememberScrollState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
        ) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                TableCell("#", isHeader = true, width = 40.dp)
                TableCell("Type", isHeader = true)
                TableCell("Status", isHeader = true)
                TableCell("Signal (dBm)", isHeader = true)
                TableCell("Level", isHeader = true)
                TableCell("RSRP", isHeader = true)
                TableCell("RSRQ", isHeader = true)
                TableCell("SINR", isHeader = true)
                TableCell("PCI", isHeader = true)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(cells.size) { index ->
                    val cell = cells[index]
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        TableCell((index + 1).toString(), width = 40.dp)
                        TableCell(cell.type)
                        TableCell(if (cell.registered) "✓ Connected" else "—")
                        TableCell(cell.dbm?.toString() ?: "N/A")
                        TableCell(cell.level?.toString() ?: "N/A")
                        TableCell(cell.rsrp?.toString() ?: "N/A")
                        TableCell(cell.rsrq?.toString() ?: "N/A")
                        TableCell(cell.sinr?.toString() ?: "N/A")
                        TableCell(cell.pci?.toString() ?: "N/A")
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SignalInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TableCell(value: String, isHeader: Boolean = false, width: Dp = 150.dp) {
    Text(
        text = value,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
    )
}
