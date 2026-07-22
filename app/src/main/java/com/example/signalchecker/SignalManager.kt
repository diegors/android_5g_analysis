package com.example.signalchecker

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.telephony.*
import com.example.signalchecker.data.CellEntry
import com.example.signalchecker.data.SignalData

class SignalManager(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    fun getCurrentSignalData(): SignalData {
        val allCellInfo = telephonyManager.allCellInfo ?: emptyList()
        val cellEntries = mutableListOf<CellEntry>()
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

        // Best available last-known location
        @SuppressLint("MissingPermission")
        val location = sequenceOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
            .filter { locationManager.isProviderEnabled(it) }
            .mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }

        for (info in allCellInfo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is CellInfoNr) {
                val sig = info.cellSignalStrength as CellSignalStrengthNr
                val id = info.cellIdentity as CellIdentityNr
                cellEntries.add(CellEntry(
                    type = "5G NR",
                    registered = info.isRegistered,
                    dbm = sig.dbm,
                    level = sig.level,
                    rsrp = sig.ssRsrp,
                    rsrq = sig.ssRsrq,
                    sinr = sig.ssSinr,
                    pci = id.pci.takeIf { it != Int.MAX_VALUE },
                    bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) id.bands.toList() else emptyList()
                ))
                val preferThis = nrRsrp == null || info.isRegistered
                if (preferThis) {
                    nrRsrp = sig.ssRsrp
                    nrRsrq = sig.ssRsrq
                    nrSinr = sig.ssSinr
                    nrDbm = sig.dbm
                }
            }

            if (info is CellInfoLte) {
                val sig = info.cellSignalStrength
                val id = info.cellIdentity
                cellEntries.add(CellEntry(
                    type = "4G LTE",
                    registered = info.isRegistered,
                    dbm = sig.dbm,
                    level = sig.level,
                    rsrp = sig.rsrp,
                    rsrq = sig.rsrq,
                    sinr = sig.rssnr,
                    rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) sig.rssi.takeIf { it != Int.MAX_VALUE } else null,
                    pci = id.pci.takeIf { it != Int.MAX_VALUE },
                    ci = id.ci.takeIf { it != Int.MAX_VALUE }?.toLong(),
                    tac = id.tac.takeIf { it != Int.MAX_VALUE },
                    mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
                    mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
                    bands = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) id.bands.toList() else emptyList()
                ))
                val preferThis = lteRsrp == null || info.isRegistered
                if (preferThis) {
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

            if (info is CellInfoWcdma) {
                val sig = info.cellSignalStrength
                val id = info.cellIdentity
                cellEntries.add(CellEntry(
                    type = "3G WCDMA",
                    registered = info.isRegistered,
                    dbm = sig.dbm,
                    level = sig.level,
                    rssi = sig.dbm,
                    pci = id.psc.takeIf { it != Int.MAX_VALUE },
                    ci = id.cid.takeIf { it != Int.MAX_VALUE }?.toLong(),
                    mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
                    mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null
                ))
            }

            if (info is CellInfoGsm) {
                val sig = info.cellSignalStrength
                val id = info.cellIdentity
                cellEntries.add(CellEntry(
                    type = "2G GSM",
                    registered = info.isRegistered,
                    dbm = sig.dbm,
                    level = sig.level,
                    rssi = sig.dbm,
                    ci = id.cid.takeIf { it != Int.MAX_VALUE }?.toLong(),
                    mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
                    mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null
                ))
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
            bands = bands,
            latitude = location?.latitude,
            longitude = location?.longitude,
            locationAccuracy = location?.accuracy,
            allCells = cellEntries
        )
    }
}
