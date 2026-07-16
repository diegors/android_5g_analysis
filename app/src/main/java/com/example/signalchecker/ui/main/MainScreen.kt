package com.example.signalchecker.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.signalchecker.data.SignalData
import android.content.Intent
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("5G Signal Checker") },
                actions = {
                    IconButton(onClick = {
                        val csv = viewModel.getCsvData()
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            type = "text/csv"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
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
                        Text("RSRP: ${it.rsrp ?: "N/A"} dBm")
                        Text("SINR: ${it.sinr ?: "N/A"} dB")
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
            LazyColumn {
                items(history) { data ->
                    SignalItem(data)
                }
            }
        }
    }
}

@Composable
fun SignalItem(data: SignalData) {
    val date = Date(data.timestamp)
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    ListItem(
        headlineContent = { Text("${data.networkType} - ${data.rsrp ?: "N/A"} dBm") },
        supportingContent = { Text("SINR: ${data.sinr ?: "N/A"} dB | Time: ${format.format(date)}") }
    )
}
