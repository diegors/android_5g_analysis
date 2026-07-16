package com.example.signalchecker.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.signalchecker.SignalManager
import com.example.signalchecker.SignalWorker
import com.example.signalchecker.data.SignalData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val _history = MutableStateFlow<List<SignalData>>(emptyList())
    val history: StateFlow<List<SignalData>> = _history

    private val _currentSignal = MutableStateFlow<SignalData?>(null)
    val currentSignal: StateFlow<SignalData?> = _currentSignal

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring

    private val signalManager = SignalManager(application)
    private val workManager = WorkManager.getInstance(application)

    init {
        loadHistory()
        refreshCurrentSignal()
        checkMonitoringStatus()
    }

    fun refreshCurrentSignal() {
        _currentSignal.value = signalManager.getCurrentSignalData()
    }

    private fun loadHistory() {
        val prefs = getApplication<Application>().getSharedPreferences("signal_prefs", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("history", "[]") ?: "[]"
        _history.value = try {
            Json.decodeFromString<List<SignalData>>(historyJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun checkMonitoringStatus() {
        viewModelScope.launch {
            val workInfos = workManager.getWorkInfosForUniqueWork("SignalMonitoring").get()
            _isMonitoring.value = workInfos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }

    fun startMonitoring(intervalMinutes: Long) {
        val workRequest = PeriodicWorkRequestBuilder<SignalWorker>(
            intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES // WorkManager min is 15m
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "SignalMonitoring",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        _isMonitoring.value = true
    }

    fun stopMonitoring() {
        workManager.cancelUniqueWork("SignalMonitoring")
        _isMonitoring.value = false
    }

    fun getCsvData(): String {
        val sb = StringBuilder()
        sb.append("Timestamp,Network Type,RSRP (dBm),SINR (dB)\n")
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        _history.value.forEach { data ->
            val date = java.util.Date(data.timestamp)
            sb.append("${format.format(date)},${data.networkType},${data.rsrp ?: ""},${data.sinr ?: ""}\n")
        }
        return sb.toString()
    }
}
