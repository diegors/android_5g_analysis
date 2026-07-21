package com.example.signalchecker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
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

        var registered = false
        var asu: Int? = null
        var level: Int? = null
        var rssi: Int? = null
        var timingAdvance: Int? = null
        var ci: Long? = null
        var pci: Int? = null
        var tac: Int? = null
        var mcc: String? = null
        var mnc: String? = null
        var bands: List<Int> = emptyList()

        for (info in allCellInfo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                val preferThis = nrRsrp == null || info.isRegistered
                if (preferThis) {
                    val sig = info.cellSignalStrength as CellSignalStrengthNr
                    nrRsrp = sig.ssRsrp
                    nrRsrq = sig.ssRsrq
                    nrSinr = sig.ssSinr
                    nrDbm = sig.dbm
                }
            }

            if (info is CellInfoLte) {
                val preferThis = lteRsrp == null || info.isRegistered
                if (preferThis) {
                    val sig = info.cellSignalStrength
                    val id = info.cellIdentity
                    lteRsrp = sig.rsrp
                    lteRsrq = sig.rsrq
                    lteSinr = sig.rssnr
                    lteDbm = sig.dbm
                    registered = info.isRegistered
                    asu = sig.asuLevel
                    level = sig.level
                    timingAdvance = sig.timingAdvance.takeIf { it != Int.MAX_VALUE }
                    ci = id.ci.takeIf { it != Int.MAX_VALUE }?.toLong()
                    pci = id.pci.takeIf { it != Int.MAX_VALUE }
                    tac = id.tac.takeIf { it != Int.MAX_VALUE }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        mcc = id.mccString
                        mnc = id.mncString
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rssi = sig.rssi.takeIf { it != Int.MAX_VALUE }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        bands = id.bands.toList()
                    }
                }
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
            lteSinr = lteSinr,
            registered = registered,
            asu = asu,
            level = level,
            rssi = rssi,
            timingAdvance = timingAdvance,
            ci = ci,
            pci = pci,
            tac = tac,
            mcc = mcc,
            mnc = mnc,
            bands = bands
        )
    }
}
