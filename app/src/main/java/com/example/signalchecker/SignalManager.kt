package com.example.signalchecker

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.*
import com.example.signalchecker.data.SignalData

class SignalManager(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @SuppressLint("MissingPermission")
    fun getCurrentSignalData(): SignalData {
        val allCellInfo = telephonyManager.allCellInfo
        var nrSignal: SignalData? = null

        for (info in allCellInfo) {
            if (info is CellInfoNr) {
                val signalStrength = info.cellSignalStrength as CellSignalStrengthNr
                nrSignal = SignalData(
                    timestamp = System.currentTimeMillis(),
                    networkType = "5G NR",
                    rsrp = signalStrength.ssRsrp,
                    rsrq = signalStrength.ssRsrq,
                    sinr = signalStrength.ssSinr,
                    dbm = signalStrength.dbm
                )
                break
            }
        }

        if (nrSignal == null) {
            // Fallback for non-5G or if not detected
            val networkType = when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                else -> "Other"
            }
            nrSignal = SignalData(
                timestamp = System.currentTimeMillis(),
                networkType = networkType
            )
        }

        return nrSignal
    }
}
