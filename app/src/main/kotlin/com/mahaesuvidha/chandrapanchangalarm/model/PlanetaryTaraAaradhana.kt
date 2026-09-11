package com.mahaesuvidha.chandrapanchangalarm.model

import swisseph.SweConst
import swisseph.SweDate
import swisseph.SwissEph
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Computes each natal user's Tara relationship for every transit planet.
 * Vipat, Pratyari and Vadha are the three warning Tara positions.
 * This is a devotional/observational feature, not medical or event certainty.
 */
object PlanetaryTaraAaradhanaCalculator {
    private const val NAK_SIZE = 360.0 / 27.0
    private const val DAY = 86_400_000L
    private val tz = TimeZone.getTimeZone("Asia/Kolkata")
    private val names = arrayOf(
        "अश्विनी", "भरणी", "कृत्तिका", "रोहिणी", "मृगशीर्ष", "आर्द्रा", "पुनर्वसू", "पुष्य", "आश्लेषा",
        "मघा", "पूर्वाफाल्गुनी", "उत्तराफाल्गुनी", "हस्त", "चित्रा", "स्वाती", "विशाखा", "अनुराधा", "ज्येष्ठा",
        "मूळ", "पूर्वाषाढा", "उत्तराषाढा", "श्रवण", "धनिष्ठा", "शतभिषा", "पूर्वाभाद्रपदा", "उत्तराभाद्रपदा", "रेवती"
    )

    data class PlanetTransit(
        val planet: Graha,
        val longitude: Double,
        val rashi: String,
        val degreeInRashi: Double,
        val nakshatra: String,
        val pada: Int,
        val tara: String,
        val isWarning: Boolean,
        val startMillis: Long,
        val endMillis: Long,
        val nextWarningStartMillis: Long
    )

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

    private fun eph() = SwissEph().also { it.swe_set_sid_mode(SweConst.SE_SIDM_LAHIRI, 0.0, 0.0) }

    private fun jd(millis: Long): Double {
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
        val hour = c.get(java.util.Calendar.HOUR_OF_DAY) + c.get(java.util.Calendar.MINUTE) / 60.0 +
            c.get(java.util.Calendar.SECOND) / 3600.0 + c.get(java.util.Calendar.MILLISECOND) / 3600000.0
        return SweDate.getJulDay(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH), hour, SweDate.SE_GREG_CAL)
    }

    private fun longitude(g: Graha, millis: Long, e: SwissEph): Double {
        if (g == Graha.KETU) return (longitude(Graha.RAHU, millis, e) + 180.0) % 360.0
        val body = bodies.firstOrNull { it.first == g }?.second ?: return 0.0
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        e.swe_calc_ut(jd(millis), body, SweConst.SEFLG_SWIEPH or SweConst.SEFLG_SIDEREAL, xx, serr)
        return ((xx[0] % 360.0) + 360.0) % 360.0
    }

    private fun nakIndex(lon: Double) = (lon / NAK_SIZE).toInt().coerceIn(0, 26)

    private fun tara(birthNak: String, currentIndex: Int): String {
        val b = names.indexOf(birthNak)
        if (b < 0) return "जन्म"
        val distance = (currentIndex - b + 27) % 27 + 1
        return arrayOf("जन्म", "संपत", "विपत", "क्षेम", "प्रत्यारी", "साधक", "वध", "मित्र", "परम मित्र")[(distance - 1) % 9]
    }

    private fun isWarning(t: String) = t == "विपत" || t == "प्रत्यारी" || t == "वध"

    private fun rashi(lon: Double): Pair<String, Double> {
        val rs = arrayOf("मेष", "वृषभ", "मिथुन", "कर्क", "सिंह", "कन्या", "तुला", "वृश्चिक", "धनु", "मकर", "कुंभ", "मीन")
        val i = (lon / 30.0).toInt().coerceIn(0, 11)
        return rs[i] to (lon - i * 30.0)
    }

    private fun pada(lon: Double): Int = ((lon % NAK_SIZE) / (NAK_SIZE / 4.0)).toInt() + 1

    private fun stepFor(g: Graha): Long = when (g) {
        Graha.CHANDRA -> 2L * 60 * 60 * 1000
        Graha.SURYA, Graha.BUDH, Graha.SHUKRA -> 12L * 60 * 60 * 1000
        Graha.MANGAL -> DAY
        Graha.GURU -> 3L * DAY
        Graha.SHANI -> 7L * DAY
        Graha.RAHU, Graha.KETU -> 14L * DAY
    }

    private fun sameNak(g: Graha, birthNak: String, at: Long, e: SwissEph): Boolean =
        tara(birthNak, nakIndex(longitude(g, at, e))).let(::isWarning)

    private fun boundary(g: Graha, at: Long, e: SwissEph, forward: Boolean): Long {
        val target = nakIndex(longitude(g, at, e))
        val step = stepFor(g)
        var low: Long
        var high: Long
        if (forward) {
            low = at; high = at + step
            while (nakIndex(longitude(g, high, e)) == target) high += step
        } else {
            high = at; low = at - step
            while (nakIndex(longitude(g, low, e)) == target) low -= step
        }
        repeat(30) {
            val mid = low + (high - low) / 2
            if (nakIndex(longitude(g, mid, e)) == target) {
                if (forward) low = mid else high = mid
            } else {
                if (forward) high = mid else low = mid
            }
        }
        return if (forward) high else high
    }

    private fun findNextWarning(g: Graha, birthNak: String, now: Long, e: SwissEph): Long {
        var cursor = now
        val max = when (g) {
            Graha.SHANI -> now + 15L * 365 * DAY
            Graha.GURU -> now + 5L * 365 * DAY
            Graha.RAHU, Graha.KETU -> now + 8L * 365 * DAY
            else -> now + 2L * 365 * DAY
        }
        while (cursor < max) {
            val currentWarning = sameNak(g, birthNak, cursor, e)
            val end = boundary(g, cursor, e, true)
            if (currentWarning) {
                cursor = end + 1000L
                continue
            }
            val nextNak = nakIndex(longitude(g, end + 1000L, e))
            val nextTara = tara(birthNak, nextNak)
            if (isWarning(nextTara)) return end
            cursor = end + 1000L
        }
        return 0L
    }

    fun calculate(birthNakshatra: String, now: Long = System.currentTimeMillis()): List<PlanetTransit> {
        if (birthNakshatra.isBlank()) return emptyList()
        val e = eph()
        return (bodies.map { it.first } + Graha.KETU).map { g ->
            val lon = longitude(g, now, e)
            val idx = nakIndex(lon)
            val t = tara(birthNakshatra, idx)
            val r = rashi(lon)
            val start = boundary(g, now, e, false)
            val end = boundary(g, now, e, true)
            PlanetTransit(g, lon, r.first, r.second, names[idx], pada(lon), t, isWarning(t), start, end,
                findNextWarning(g, birthNakshatra, now, e))
        }
    }

    fun format(millis: Long): String = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).apply { timeZone = tz }.format(Date(millis))
}
