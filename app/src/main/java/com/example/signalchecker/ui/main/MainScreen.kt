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
fun MainScreen(viewModel: MainScreenViewModel = viewModel()) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val currentSignal by viewModel.currentSignal.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    var intervalText by remember { mutableStateOf("15") }
    var plainTextPreview by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("5G Signal Checker") },
                actions = {
                    IconButton(onClick = {
                        val csv = viewModel.getCsvData()
                        val exportResult = viewModel.exportCsvToDownloads(context)
                        if (exportResult.isSuccess) {
                            Toast.makeText(
                                context,
                                "Saved ${exportResult.getOrNull()} to Downloads",
                                Toast.LENGTH_LONG
                            ).show()
                            if (!viewModel.hasCsvViewer(context)) {
                                plainTextPreview = csv
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
                        Text("Network: ${it.networkType}")
                        Text("5G RSRP: ${it.nrRsrp ?: "N/A"} dBm")
                        Text("5G SINR: ${it.nrSinr ?: "N/A"} dB")
                        Text("4G RSRP: ${it.lteRsrp ?: "N/A"} dBm")
                        Text("4G SINR: ${it.lteSinr ?: "N/A"} dB")
                    } ?: Text("Detecting...")
                    
                    Button(onClick = { viewModel.refreshCurrentSignal() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Refresh Now")
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
                    Button(onClick = { viewModel.startMonitoring(intervalText.toLongOrNull() ?: 15) }) {
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
                TableCell("Network", isHeader = true)
                TableCell("5G RSRP", isHeader = true)
                TableCell("5G SINR", isHeader = true)
                TableCell("4G RSRP", isHeader = true)
                TableCell("4G SINR", isHeader = true)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(history) { data ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        TableCell(format.format(Date(data.timestamp)))
                        TableCell(data.networkType)
                        TableCell(data.nrRsrp?.toString() ?: "N/A")
                        TableCell(data.nrSinr?.toString() ?: "N/A")
                        TableCell(data.lteRsrp?.toString() ?: "N/A")
                        TableCell(data.lteSinr?.toString() ?: "N/A")
                    }
                    HorizontalDivider()
                }
            }
        }
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
