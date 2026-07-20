package com.example.signalchecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.signalchecker.data.SignalData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SignalWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val signalManager = SignalManager(applicationContext)
        val signalData = signalManager.getCurrentSignalData()

        saveSignalData(signalData)
        showNotification(signalData)

        return Result.success()
    }

    private fun saveSignalData(data: SignalData) {
        val prefs = applicationContext.getSharedPreferences("signal_prefs", Context.MODE_PRIVATE)
        val historyJson = prefs.getString("history", "[]") ?: "[]"
        val history = Json.decodeFromString<MutableList<SignalData>>(historyJson).toMutableList()
        
        history.add(0, data) // Add new at the beginning
        if (history.size > 50) history.removeAt(history.size - 1) // Keep last 50

        prefs.edit().putString("history", Json.encodeToString(history)).apply()
    }

    private fun showNotification(data: SignalData) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "signal_checker_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "5G Signal Checker", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val message = "Type: ${data.networkType}, RSRP: ${data.rsrp ?: "N/A"} dBm"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("5G Signal Check")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}
