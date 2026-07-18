package com.example.signalchecker.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSignalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // A simple counter used as a key to force re-reading Wi-Fi info on refresh
    var refreshKey by remember { mutableIntStateOf(0) }
    val wifiInfo = remember(refreshKey) { getWifiSignalSummary(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wi-Fi Signal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Wi-Fi Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(wifiInfo)
                }
            }

            Button(onClick = { refreshKey++ }) {
                Text("Refresh")
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun getWifiSignalSummary(context: Context): String {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return "Wi-Fi service unavailable."

    // On API 31+ use ConnectivityManager to get WifiInfo (connectionInfo is deprecated/broken there)
    val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(network)
        caps?.transportInfo as? WifiInfo
    } else {
        @Suppress("DEPRECATION")
        wifiManager.connectionInfo
    }

    return if (wifiInfo == null) {
        "No Wi-Fi connection available."
    } else {
        val ssid = wifiInfo.ssid
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
            ?: "Unknown (grant Location permission)"
        val bssid = wifiInfo.bssid
            ?.takeIf { it != "02:00:00:00:00:00" }
            ?: "N/A"
        val rssi = wifiInfo.rssi
        val level = wifiManager.calculateSignalLevel(rssi)
        val linkSpeed = wifiInfo.linkSpeed
        val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) wifiInfo.frequency else null

        buildString {
            appendLine("SSID: $ssid")
            appendLine("BSSID: $bssid")
            appendLine("RSSI: $rssi dBm")
            appendLine("Signal level: $level / 5")
            appendLine("Link speed: $linkSpeed Mbps")
            if (frequency != null) appendLine("Frequency: $frequency MHz")
        }.trim()
    }
}
