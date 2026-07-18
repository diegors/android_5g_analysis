package com.example.signalchecker

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.*
import com.example.signalchecker.data.SignalData

class SignalManager(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @SuppressLint("MissingPermission")
    fun getCurrentSignalData(): SignalData {
        val allCellInfo = telephonyManager.allCellInfo ?: emptyList()
        var nrRsrp: Int? = null
        var nrRsrq: Int? = null
        var nrSinr: Int? = null
        var nrDbm: Int? = null
        var lteRsrp: Int? = null
        var lteRsrq: Int? = null
        var lteSinr: Int? = null
        var lteDbm: Int? = null

        for (info in allCellInfo) {
            if (info is CellInfoNr) {
                val signalStrength = info.cellSignalStrength as CellSignalStrengthNr
                nrRsrp = signalStrength.ssRsrp
                nrRsrq = signalStrength.ssRsrq
                nrSinr = signalStrength.ssSinr
                nrDbm = signalStrength.dbm
            }

            if (info is CellInfoLte) {
                val signalStrength = info.cellSignalStrength as CellSignalStrengthLte
                lteRsrp = signalStrength.rsrp
                lteRsrq = signalStrength.rsrq
                lteSinr = signalStrength.rssnr
                lteDbm = signalStrength.dbm
            }
        }

        val networkType = when {
            nrRsrp != null && lteRsrp != null -> "5G NR + 4G LTE"
            nrRsrp != null -> "5G NR"
            lteRsrp != null -> "4G LTE"
            telephonyManager.networkType == TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            telephonyManager.networkType == TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            else -> "Other"
        }

        return SignalData(
            timestamp = System.currentTimeMillis(),
            networkType = networkType,
            rsrp = nrRsrp ?: lteRsrp,
            rsrq = nrRsrq ?: lteRsrq,
            sinr = nrSinr ?: lteSinr,
            dbm = nrDbm ?: lteDbm,
            nrRsrp = nrRsrp,
            nrRsrq = nrRsrq,
            nrSinr = nrSinr,
            lteRsrp = lteRsrp,
            lteRsrq = lteRsrq,
            lteSinr = lteSinr
        )
    }
}
