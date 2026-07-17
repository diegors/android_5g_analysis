package com.example.signalchecker.ui.main

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val PREFS_NAME = "signal_prefs"
        const val HISTORY_KEY = "history"
        const val LAST_EXPORTED_TIMESTAMP_KEY = "last_exported_timestamp"
        const val EXPORT_FILE_NAME = "signal_results.csv"
    }

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
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyJson = prefs.getString(HISTORY_KEY, "[]") ?: "[]"
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
        sb.append(csvHeader())
        sb.append('\n')
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        _history.value.forEach { data ->
            sb.append(csvRow(data, format))
            sb.append('\n')
        }
        return sb.toString()
    }

    fun hasCsvViewer(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "text/csv"
        }
        return intent.resolveActivity(context.packageManager) != null
    }

    fun exportCsvToDownloads(context: Context): Result<String> {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastExportedTimestamp = prefs.getLong(LAST_EXPORTED_TIMESTAMP_KEY, 0L)
        val pendingRows = _history.value
            .filter { it.timestamp > lastExportedTimestamp }
            .sortedBy { it.timestamp }

        if (pendingRows.isEmpty()) {
            return Result.success("No new rows to append to $EXPORT_FILE_NAME")
        }

        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return try {
            val appendedCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appendForAndroidQAndAbove(context, pendingRows, format)
            } else {
                appendForPreQ(pendingRows, format)
            }

            val newestTimestamp = pendingRows.maxOf { it.timestamp }
            prefs.edit().putLong(LAST_EXPORTED_TIMESTAMP_KEY, newestTimestamp).apply()

            Result.success("Appended $appendedCount rows to $EXPORT_FILE_NAME")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun appendForAndroidQAndAbove(
        context: Context,
        rows: List<SignalData>,
        format: SimpleDateFormat,
    ): Int {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID, OpenableColumns.SIZE)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(EXPORT_FILE_NAME, "${Environment.DIRECTORY_DOWNLOADS}/")

        var existingUri: android.net.Uri? = null
        var existingSize = 0L
        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
                val id = cursor.getLong(idIndex)
                existingUri = ContentUris.withAppendedId(collection, id)
                existingSize = cursor.getLong(sizeIndex)
            }
        }

        val targetUri = existingUri ?: run {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, EXPORT_FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            resolver.insert(collection, values)
                ?: throw IllegalStateException("Could not create download entry")
        }

        val shouldWriteHeader = existingUri == null || existingSize == 0L
        resolver.openOutputStream(targetUri, "wa")?.bufferedWriter().use { writer ->
            if (writer == null) {
                throw IllegalStateException("Could not open output stream")
            }

            if (shouldWriteHeader) {
                writer.append(csvHeader())
                writer.newLine()
            }

            for (row in rows) {
                writer.append(csvRow(row, format))
                writer.newLine()
            }
        }

        return rows.size
    }

    private fun appendForPreQ(rows: List<SignalData>, format: SimpleDateFormat): Int {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val outputFile = File(downloadsDir, EXPORT_FILE_NAME)
        if (!outputFile.exists() || outputFile.length() == 0L) {
            outputFile.appendText(csvHeader() + "\n")
        }

        rows.forEach { row ->
            outputFile.appendText(csvRow(row, format) + "\n")
        }

        return rows.size
    }

    private fun csvHeader(): String {
        return "Timestamp,Network Type,5G RSRP (dBm),5G SINR (dB),4G RSRP (dBm),4G SINR (dB)"
    }

    private fun csvRow(data: SignalData, format: SimpleDateFormat): String {
        val formattedTime = format.format(Date(data.timestamp))
        return "$formattedTime,${data.networkType},${data.nrRsrp ?: data.rsrp ?: ""},${data.nrSinr ?: data.sinr ?: ""},${data.lteRsrp ?: ""},${data.lteSinr ?: ""}"
    }
}
