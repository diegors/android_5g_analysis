package com.example.signalchecker.data

import kotlinx.serialization.Serializable

@Serializable
data class SignalData(
    val timestamp: Long,
    val networkType: String,
    val rsrp: Int? = null, // SS-RSRP
    val rsrq: Int? = null, // SS-RSRQ
    val sinr: Int? = null, // SS-SINR
    val dbm: Int? = null,
    val nrRsrp: Int? = null,
    val nrRsrq: Int? = null,
    val nrSinr: Int? = null,
    val lteRsrp: Int? = null,
    val lteRsrq: Int? = null,
    val lteSinr: Int? = null
)
