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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalchecker.data.SignalData
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
                        SignalInfoRow("Network", it.networkType)
                        SignalInfoRow("Registered", if (it.registered) "Yes" else "No")
                        SignalInfoRow("Level", it.level?.toString() ?: "N/A")
                        SignalInfoRow("DBm", it.dbm?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("ASU", it.asu?.toString() ?: "N/A")
                        SignalInfoRow("5G RSRP", it.nrRsrp?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("5G RSRQ", it.nrRsrq?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("5G SINR", it.nrSinr?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("4G RSRP", it.lteRsrp?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("4G RSRQ", it.lteRsrq?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("4G SINR", it.lteSinr?.let { v -> "$v dB" } ?: "N/A")
                        SignalInfoRow("RSSI", it.rssi?.let { v -> "$v dBm" } ?: "N/A")
                        SignalInfoRow("Timing Advance", it.timingAdvance?.toString() ?: "N/A")
                        SignalInfoRow("CI", it.ci?.toString() ?: "N/A")
                        SignalInfoRow("PCI", it.pci?.toString() ?: "N/A")
                        SignalInfoRow("TAC", it.tac?.toString() ?: "N/A")
                        SignalInfoRow("MCC", it.mcc ?: "N/A")
                        SignalInfoRow("MNC", it.mnc ?: "N/A")
                        SignalInfoRow("Bands", it.bands.takeIf { b -> b.isNotEmpty() }?.joinToString(", ") ?: "N/A")
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
private fun TableCell(value: String, isHeader: Boolean = false) {
    Text(
        text = value,
        modifier = Modifier
            .width(150.dp)
            .padding(horizontal = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal
    )
}
