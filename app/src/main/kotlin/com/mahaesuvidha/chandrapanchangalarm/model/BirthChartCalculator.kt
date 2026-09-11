package com.mahaesuvidha.chandrapanchangalarm.model

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Birth-chart helper used by Today's Prediction.
 * Uses the saved birth date/time and the birth-place coordinates resolved by the UI.
 * Houses are whole-sign houses from the sidereal ascendant.
 */
object BirthChartCalculator {
    private val INDIA_ZONE = ZoneId.of("Asia/Kolkata")
    private val formats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")
    )

    data class PlanetPosition(
        val rashiIndex: Int,
        val house: Int,
        val longitude: Double = 0.0,
        val nakshatraIndex: Int = 0,
        val pada: Int = 1
    )

    fun parseBirthTime(date: String, time: String): LocalDateTime? {
        val normalized = time.trim().uppercase()
            .replace("AM", " AM")
            .replace("PM", " PM")
            .replace(Regex("\\s+"), " ")
        val candidates = listOf(
            "$date ${time.trim()}",
            "$date $normalized"
        )
        for (candidate in candidates) {
            for (format in formats) {
                runCatching { return LocalDateTime.parse(candidate, format) }
            }
        }
        return runCatching {
            val d = date.trim().split("/")
            val t = time.trim().split(":")
            LocalDateTime.of(d[2].toInt(), d[1].toInt(), d[0].toInt(), t[0].toInt(), t[1].toInt())
        }.getOrNull()
    }

    fun calculate(
        birthDate: String,
        birthTime: String,
        latitude: Double,
        longitude: Double
    ): Map<Graha, PlanetPosition> {
        val local = parseBirthTime(birthDate, birthTime) ?: return emptyMap()
        val jd = julianDay(local)
        val asc = siderealAscendant(local, latitude, longitude)
        val ascRashi = rashiIndex(asc)
        val swe = swisseph.SwissEph().apply {
            swe_set_sid_mode(swisseph.SweConst.SE_SIDM_LAHIRI, 0.0, 0.0)
        }
        val bodies = listOf(
            Graha.SURYA to swisseph.SweConst.SE_SUN,
            Graha.CHANDRA to swisseph.SweConst.SE_MOON,
            Graha.MANGAL to swisseph.SweConst.SE_MARS,
            Graha.BUDH to swisseph.SweConst.SE_MERCURY,
            Graha.GURU to swisseph.SweConst.SE_JUPITER,
            Graha.SHUKRA to swisseph.SweConst.SE_VENUS,
            Graha.SHANI to swisseph.SweConst.SE_SATURN,
            Graha.RAHU to swisseph.SweConst.SE_TRUE_NODE
        )
        val result = linkedMapOf<Graha, PlanetPosition>()
        bodies.forEach { (graha, body) ->
            val lon = longitude(swe, jd, body)
            val idx = rashiIndex(lon)
            val nakIndex = (lon / (360.0 / 27.0)).toInt().coerceIn(0, 26)
            val pada = (((lon % (360.0 / 27.0)) / (360.0 / 108.0)).toInt() + 1).coerceIn(1, 4)
            result[graha] = PlanetPosition(idx, wholeSignHouse(ascRashi, idx), lon, nakIndex, pada)
        }
        val ketuRashi = ((result[Graha.RAHU]?.rashiIndex ?: 0) + 6) % 12
        val ketuLon = normalize((result[Graha.RAHU]?.longitude ?: 0.0) + 180.0)
        val ketuNak = (ketuLon / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val ketuPada = (((ketuLon % (360.0 / 27.0)) / (360.0 / 108.0)).toInt() + 1).coerceIn(1, 4)
        result[Graha.KETU] = PlanetPosition(
            ketuRashi,
            wholeSignHouse(ascRashi, ketuRashi),
            ketuLon,
            ketuNak,
            ketuPada
        )
        return result
    }

    private fun longitude(swe: swisseph.SwissEph, jd: Double, body: Int): Double {
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        swe.swe_calc_ut(jd, body, swisseph.SweConst.SEFLG_SWIEPH or swisseph.SweConst.SEFLG_SIDEREAL, xx, serr)
        return normalize(xx[0])
    }

    private fun rashiIndex(value: Double): Int = (normalize(value) / 30.0).toInt().coerceIn(0, 11)
    private fun wholeSignHouse(ascRashi: Int, planetRashi: Int): Int = (planetRashi - ascRashi + 12) % 12 + 1
    private fun normalize(value: Double): Double = ((value % 360.0) + 360.0) % 360.0

    private fun julianDay(time: LocalDateTime): Double {
        val zdt = time.atZone(INDIA_ZONE)
        val millis = zdt.toInstant().toEpochMilli()
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY) +
            cal.get(java.util.Calendar.MINUTE) / 60.0 +
            cal.get(java.util.Calendar.SECOND) / 3600.0
        return swisseph.SweDate.getJulDay(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            hour,
            swisseph.SweDate.SE_GREG_CAL
        )
    }

    private fun siderealAscendant(time: LocalDateTime, latitude: Double, longitude: Double): Double {
        val jd = julianDay(time)
        val t = (jd - 2451545.0) / 36525.0
        val gmst = normalize(
            280.46061837 + 360.98564736629 * (jd - 2451545.0) +
                0.000387933 * t * t - t * t * t / 38710000.0
        )
        val lst = normalize(gmst + longitude)
        val theta = Math.toRadians(lst)
        val phi = Math.toRadians(latitude)
        val epsilon = Math.toRadians(23.439291)
        // Ascendant ecliptic longitude. The previous implementation used the
        // opposite atan2 quadrant, shifting the ascendant by exactly 180°
        // (e.g. Taurus was being reported as Scorpio). Keep the standard
        // astronomical quadrant here before applying Lahiri ayanamsa.
        val tropical = normalize(Math.toDegrees(atan2(cos(theta), -(sin(theta) * cos(epsilon) + tan(phi) * sin(epsilon)))))
        val years = time.year - 2000.0
        val ayanamsa = 23.85675 + (50.29 / 3600.0) * years
        return normalize(tropical - ayanamsa)
    }
}
