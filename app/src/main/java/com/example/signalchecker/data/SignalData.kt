package com.example.signalchecker.data

import kotlinx.serialization.Serializable

@Serializable
data class SignalData(
    val timestamp: Long,
    val networkType: String,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val dbm: Int? = null,
    val nrRsrp: Int? = null,
    val nrRsrq: Int? = null,
    val nrSinr: Int? = null,
    val lteRsrp: Int? = null,
    val lteRsrq: Int? = null,
    val lteSinr: Int? = null,
    // Registration & signal quality
    val registered: Boolean = false,
    val asu: Int? = null,
    val level: Int? = null,
    val rssi: Int? = null,
    val timingAdvance: Int? = null,
    // Cell identity
    val ci: Long? = null,
    val pci: Int? = null,
    val tac: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val bands: List<Int> = emptyList(),
    // GPS location
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracy: Float? = null
)
