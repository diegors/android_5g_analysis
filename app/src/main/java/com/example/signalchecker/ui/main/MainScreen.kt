package com.example.signalchecker.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
    var plainTextPreview by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedCellIndex by remember { mutableStateOf<Int?>(null) }

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
                    
                    val displayCell = currentSignal?.let { signal ->
                        selectedCellIndex?.let { signal.allCells.getOrNull(it) }
                    }
                    
                    if (displayCell != null) {
                        // Display selected cell from table
                        SignalInfoRow("Type", displayCell.type)
                        SignalInfoRow("Registered", if (displayCell.registered) "Yes" else "No")
                        SignalInfoRow("MCC", displayCell.mcc ?: "N/A")
                        SignalInfoRow("MNC", displayCell.mnc ?: "N/A")
                        SignalInfoRow("CI/CID", displayCell.ci?.toString() ?: "N/A")
                        SignalInfoRow("PCI/PSC", displayCell.pci?.toString() ?: "N/A")
                        SignalInfoRow("TAC/LAC", displayCell.tac?.toString() ?: (displayCell.lac?.toString() ?: "N/A"))
                        SignalInfoRow("ARFCN", displayCell.arfcn?.toString() ?: "N/A")
                        displayCell.bandwidth?.let { SignalInfoRow("Bandwidth", "$it kHz") }
                        displayCell.bsic?.let { SignalInfoRow("BSIC", it.toString()) }
                        SignalInfoRow("DBm", displayCell.dbm?.let { "$it dBm" } ?: "N/A")
                        displayCell.asu?.let { SignalInfoRow("ASU", it.toString()) }
                        displayCell.level?.let { SignalInfoRow("Level", it.toString()) }
                        displayCell.rsrp?.let { SignalInfoRow("RSRP", "$it dBm") }
                        displayCell.rsrq?.let { SignalInfoRow("RSRQ", "$it dB") }
                        displayCell.sinr?.let { SignalInfoRow("SINR", "$it dB") }
                        displayCell.csiRsrp?.let { SignalInfoRow("CSI-RSRP", "$it dBm") }
                        displayCell.csiRsrq?.let { SignalInfoRow("CSI-RSRQ", "$it dB") }
                        displayCell.csiSinr?.let { SignalInfoRow("CSI-SINR", "$it dB") }
                        displayCell.rssi?.let { SignalInfoRow("RSSI", "$it dBm") }
                        displayCell.cqi?.let { SignalInfoRow("CQI", it.toString()) }
                        displayCell.timingAdvance?.let { SignalInfoRow("Timing Advance", it.toString()) }
                        if (displayCell.bands.isNotEmpty()) SignalInfoRow("Bands", displayCell.bands.joinToString(", "))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = { selectedCellIndex = null }) {
                                Text("Clear Selection")
                            }
                        }
                    } else if (currentSignal != null) {
                        // Display current signal (original behavior)
                        val it = currentSignal!!
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
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = { viewModel.captureAndAppendCurrentSignalToCsv() }) {
                                Text("Capture Signal")
                            }
                            Button(onClick = onNavigateToWifi) {
                                Text("View Wi-Fi Signal")
                            }
                        }
                    } else {
                        Text("Detecting...")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            currentSignal?.let { signal ->
                if (signal.allCells.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("All Detected Cells (${signal.allCells.size})", style = MaterialTheme.typography.titleMedium)
                        Button(
                            onClick = {
                                isRefreshing = true
                                viewModel.captureAndAppendCurrentSignalToCsv()
                                isRefreshing = false
                                Toast.makeText(context, "Refreshed and saved to CellularSignalResults.csv", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isRefreshing
                        ) {
                            Text("Refresh & Save")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AllCellsTable(cells = signal.allCells, selectedIndex = selectedCellIndex, onCellSelected = { selectedCellIndex = it })
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
private fun AllCellsTable(cells: List<CellEntry>, selectedIndex: Int? = null, onCellSelected: (Int) -> Unit = {}) {
    val horizontalScroll = rememberScrollState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
        ) {
            // Header
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                TableCell("#",            isHeader = true, width = 36.dp)
                TableCell("Type",         isHeader = true, width = 110.dp)
                TableCell("Registered",   isHeader = true)
                TableCell("MCC",          isHeader = true, width = 60.dp)
                TableCell("MNC",          isHeader = true, width = 60.dp)
                TableCell("CI/CID",       isHeader = true)
                TableCell("PCI/PSC",      isHeader = true)
                TableCell("TAC/LAC",      isHeader = true)
                TableCell("ARFCN",        isHeader = true)
                TableCell("BW (kHz)",     isHeader = true)
                TableCell("BSIC",         isHeader = true, width = 60.dp)
                TableCell("DBm",          isHeader = true, width = 80.dp)
                TableCell("ASU",          isHeader = true, width = 60.dp)
                TableCell("Level",        isHeader = true, width = 60.dp)
                TableCell("RSRP",         isHeader = true)
                TableCell("RSRQ",         isHeader = true)
                TableCell("SINR",         isHeader = true)
                TableCell("CSI-RSRP",     isHeader = true)
                TableCell("CSI-RSRQ",     isHeader = true)
                TableCell("CSI-SINR",     isHeader = true)
                TableCell("RSSI",         isHeader = true)
                TableCell("CQI",          isHeader = true, width = 60.dp)
                TableCell("TA",           isHeader = true, width = 60.dp)
                TableCell("Bands",        isHeader = true)
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                items(cells.size) { index ->
                    val c = cells[index]
                    val ci = c.ci ?: c.cid
                    val pci = c.pci
                    val tac = c.tac ?: c.lac ?: c.lac_gsm
                    val isSelected = selectedIndex == index
                    Row(
                        modifier = Modifier
                            .padding(vertical = 6.dp)
                            .clickable { onCellSelected(index) }
                            .background(if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer else androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    ) {
                        TableCell((index + 1).toString(),                       width = 36.dp)
                        TableCell(c.type,                                        width = 110.dp)
                        TableCell(if (c.registered) "Yes" else "No")
                        TableCell(c.mcc ?: "N/A",                               width = 60.dp)
                        TableCell(c.mnc ?: "N/A",                               width = 60.dp)
                        TableCell(ci?.toString() ?: "N/A")
                        TableCell(pci?.toString() ?: "N/A")
                        TableCell(tac?.toString() ?: "N/A")
                        TableCell(c.arfcn?.toString() ?: "N/A")
                        TableCell(c.bandwidth?.toString() ?: "N/A")
                        TableCell(c.bsic?.toString() ?: "N/A",                  width = 60.dp)
                        TableCell(c.dbm?.toString() ?: "N/A",                   width = 80.dp)
                        TableCell(c.asu?.toString() ?: "N/A",                   width = 60.dp)
                        TableCell(c.level?.toString() ?: "N/A",                 width = 60.dp)
                        TableCell(c.rsrp?.toString() ?: "N/A")
                        TableCell(c.rsrq?.toString() ?: "N/A")
                        TableCell(c.sinr?.toString() ?: "N/A")
                        TableCell(c.csiRsrp?.toString() ?: "N/A")
                        TableCell(c.csiRsrq?.toString() ?: "N/A")
                        TableCell(c.csiSinr?.toString() ?: "N/A")
                        TableCell(c.rssi?.toString() ?: "N/A")
                        TableCell(c.cqi?.toString() ?: "N/A",                   width = 60.dp)
                        TableCell(c.timingAdvance?.toString() ?: "N/A",         width = 60.dp)
                        TableCell(c.bands.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "N/A")
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
