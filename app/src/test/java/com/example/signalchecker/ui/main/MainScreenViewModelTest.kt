package com.example.signalchecker.ui.main

import com.example.signalchecker.data.SignalData
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class MainScreenViewModelTest {

    @Test
    fun signalData_networkType_defaultsCorrectly() {
        val data = SignalData(
            timestamp = 0L,
            networkType = "5G NR",
            nrRsrp = -85,
            nrSinr = 12
        )
        assertEquals("5G NR", data.networkType)
        assertEquals(-85, data.nrRsrp)
        assertEquals(12, data.nrSinr)
    }

    @Test
    fun signalData_nullableFields_areNullByDefault() {
        val data = SignalData(timestamp = 0L, networkType = "Other")
        assertTrue(data.rsrp == null)
        assertTrue(data.dbm == null)
        assertTrue(data.lteRsrp == null)
    }

    @Test
    fun csvRow_formatsCorrectly() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val data = SignalData(
            timestamp = 0L,
            networkType = "4G LTE",
            lteRsrp = -95,
            lteSinr = 8
        )
        val rsrp = data.nrRsrp ?: data.lteRsrp ?: data.rsrp ?: ""
        val sinr = data.nrSinr ?: data.lteSinr ?: data.sinr ?: ""
        val row = "${format.format(java.util.Date(data.timestamp))},${data.networkType},$rsrp,$sinr"
        assertTrue(row.contains("4G LTE"))
        assertTrue(row.contains("-95"))
        assertTrue(row.contains("8"))
    }
}
