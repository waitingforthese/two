package com.mahaesuvidha.chandrapanchangalarm.settings

import android.content.Context
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfileStore

class AaradhanaPrefs(private val context: Context) {
    private val prefs get() = context.getSharedPreferences("life_alarm_aaradhana", Context.MODE_PRIVATE)
    private fun key(suffix: String): String {
        val p = BirthProfileStore.load(context.applicationContext)
        val id = p?.let { "${it.name}|${it.birthDate}|${it.birthTime}|${it.birthPlace}" } ?: "default"
        return "${id.hashCode()}_$suffix"
    }
    var specialHourly: Boolean
        get() = prefs.getBoolean(key("special_hourly"), false)
        set(value) = prefs.edit().putBoolean(key("special_hourly"), value).apply()

    /** Number of repetitions for each mantra in special Aaradhana. */
    var specialJapaCount: Int
        get() = prefs.getInt(key("special_japa_count"), 11).coerceIn(1, 108)
        set(value) = prefs.edit().putInt(key("special_japa_count"), value.coerceIn(1, 108)).apply()

    /**
     * Fixed daily clock times for Special Aaradhana.
     * Default is every 3 hours from 05:00 through 23:00.
     * Stored as comma-separated HH:mm values.
     */
    var specialFixedTimes: String
        get() = prefs.getString(key("special_fixed_times"), "05:00,08:00,11:00,14:00,17:00,20:00,23:00")
            ?: "05:00,08:00,11:00,14:00,17:00,20:00,23:00"
        set(value) = prefs.edit().putString(key("special_fixed_times"), value).apply()

    /** Legacy interval kept for backward compatibility with older saved data. */
    var specialIntervalHours: Int
        get() = prefs.getInt(key("special_interval_hours"), 3).coerceIn(1, 24)
        set(value) = prefs.edit().putInt(key("special_interval_hours"), value.coerceIn(1, 24)).apply()

    /** Independent change-triggered Aaradhana switches. These do not depend on Alarm Settings. */
    var nakshatraChangeAaradhana: Boolean
        get() = prefs.getBoolean(key("nakshatra_change_aaradhana"), true)
        set(value) = prefs.edit().putBoolean(key("nakshatra_change_aaradhana"), value).apply()

    var yogaChangeAaradhana: Boolean
        get() = prefs.getBoolean(key("yoga_change_aaradhana"), true)
        set(value) = prefs.edit().putBoolean(key("yoga_change_aaradhana"), value).apply()

    var karanaChangeAaradhana: Boolean
        get() = prefs.getBoolean(key("karana_change_aaradhana"), true)
        set(value) = prefs.edit().putBoolean(key("karana_change_aaradhana"), value).apply()

    /** Independent per-planet Vipat/Pratyari/Vadha Tara Aaradhana switches. */
    var planetaryTaraAaradhana: Boolean
        get() = prefs.getBoolean(key("planetary_tara_aaradhana"), false)
        set(value) = prefs.edit().putBoolean(key("planetary_tara_aaradhana"), value).apply()

    fun isPlanetaryTaraEnabled(graha: String): Boolean =
        prefs.getBoolean(key("planetary_tara_${graha}"), false)

    fun setPlanetaryTaraEnabled(graha: String, enabled: Boolean) {
        prefs.edit().putBoolean(key("planetary_tara_${graha}"), enabled).apply()
    }

    /** TTS speech rate for Aaradhana; lower values produce slower, clearer chanting. */
    var speechRate: Float
        get() = prefs.getFloat(key("speech_rate"), 0.72f).coerceIn(0.35f, 0.90f)
        set(value) = prefs.edit().putFloat(key("speech_rate"), value.coerceIn(0.35f, 0.90f)).apply()
}
