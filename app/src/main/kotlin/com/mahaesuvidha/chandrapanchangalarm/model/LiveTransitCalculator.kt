package com.mahaesuvidha.chandrapanchangalarm.model

import swisseph.SweConst
import swisseph.SwissEph
import swisseph.SweDate
import java.util.Calendar
import java.util.TimeZone

/**
 * Single source of truth for current sidereal transit longitudes.
 *
 * All screens that show live planetary positions must use this calculator so
 * the displayed rashi/degree cannot drift because of different time handling.
 * Lahiri (Chitrapaksha) sidereal mode is used, with TRUE_NODE for Rahu and
 * Ketu exactly 180 degrees from Rahu.
 */
object LiveTransitCalculator {

    private val bodies = listOf(
        Graha.SURYA to SweConst.SE_SUN,
        Graha.CHANDRA to SweConst.SE_MOON,
        Graha.MANGAL to SweConst.SE_MARS,
        Graha.BUDH to SweConst.SE_MERCURY,
        Graha.GURU to SweConst.SE_JUPITER,
        Graha.SHUKRA to SweConst.SE_VENUS,
        Graha.SHANI to SweConst.SE_SATURN,
        Graha.RAHU to SweConst.SE_TRUE_NODE
    )

    fun positionsAt(millis: Long = System.currentTimeMillis()): Map<Graha, Double> {
        val swe = SwissEph().apply {
            swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        }
        val jd = julianDayUtc(millis)
        val result = linkedMapOf<Graha, Double>()
        bodies.forEach { (graha, body) ->
            result[graha] = longitude(swe, jd, body)
        }
        result[Graha.KETU] = ((result[Graha.RAHU] ?: 0.0) + 180.0) % 360.0
        return result
    }

    fun longitudeAt(graha: Graha, millis: Long = System.currentTimeMillis()): Double {
        return positionsAt(millis)[graha] ?: 0.0
    }

    fun sameLocalClockMillis(baseMillis: Long, dayOffset: Long): Long {
        val local = java.time.Instant.ofEpochMilli(baseMillis)
            .atZone(java.time.ZoneId.of("Asia/Kolkata"))
            .plusDays(dayOffset)
        return local.toInstant().toEpochMilli()
    }

    private fun longitude(swe: SwissEph, jd: Double, body: Int): Double {
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        swe.swe_calc_ut(
            jd,
            body,
            SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SIDEREAL,
            xx,
            serr
        )
        return ((xx[0] % 360.0) + 360.0) % 360.0
    }

    private fun julianDayUtc(millis: Long): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = millis
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY) +
            cal.get(Calendar.MINUTE) / 60.0 +
            cal.get(Calendar.SECOND) / 3600.0 +
            cal.get(Calendar.MILLISECOND) / 3600000.0
        return SweDate.getJulDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            hour,
            SweDate.SE_GREG_CAL
        )
    }
}
