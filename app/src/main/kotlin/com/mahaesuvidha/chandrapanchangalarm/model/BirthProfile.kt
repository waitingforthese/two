package com.mahaesuvidha.chandrapanchangalarm.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BirthProfile(
    val name: String,
    val birthDate: String,
    val gender: String,
    val birthTime: String,
    val birthPlace: String,
    val birthMoonRashi: String,
    val birthNakshatra: String = ""
)

/**
 * Persistent multi-user birth-profile store.
 *
 * The currently selected profile is kept separately from the saved profile list,
 * so Logout only switches the active user; it never deletes previously saved users.
 */
object BirthProfileStore {
    private const val PREFS = "birth_profile"
    private const val KEY_ACTIVE_ID = "active_profile_id"
    private const val KEY_PROFILES = "saved_profiles_json"

    // Legacy single-profile keys kept for migration.
    private const val KEY_NAME = "name"
    private const val KEY_DATE = "birth_date"
    private const val KEY_GENDER = "gender"
    private const val KEY_TIME = "birth_time"
    private const val KEY_PLACE = "birth_place"
    private const val KEY_MOON_RASHI = "birth_moon_rashi"
    private const val KEY_NAKSHATRA = "birth_nakshatra"

    fun save(context: Context, profile: BirthProfile) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = loadAllInternal(p).toMutableList()
        val existingIndex = list.indexOfFirst { samePerson(it, profile) }
        if (existingIndex >= 0) list[existingIndex] = profile else list.add(profile)
        val id = profileId(profile)
        p.edit()
            .putString(KEY_PROFILES, encode(list))
            .putString(KEY_ACTIVE_ID, id)
            // Keep legacy fields synchronized for backward compatibility.
            .putString(KEY_NAME, profile.name)
            .putString(KEY_DATE, profile.birthDate)
            .putString(KEY_GENDER, profile.gender)
            .putString(KEY_TIME, profile.birthTime)
            .putString(KEY_PLACE, profile.birthPlace)
            .putString(KEY_MOON_RASHI, profile.birthMoonRashi)
            .putString(KEY_NAKSHATRA, profile.birthNakshatra)
            .apply()
    }

    fun load(context: Context): BirthProfile? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = loadAllInternal(p)
        if (list.isNotEmpty()) {
            val activeId = p.getString(KEY_ACTIVE_ID, null)
            val active = list.firstOrNull { profileId(it) == activeId }
            if (active != null) return active
            return list.lastOrNull()
        }
        return loadLegacy(p)
    }

    fun savedProfiles(context: Context): List<BirthProfile> =
        loadAllInternal(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)).asReversed()

    fun activate(context: Context, profile: BirthProfile) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = loadAllInternal(p).toMutableList()
        if (list.none { samePerson(it, profile) }) list.add(profile)
        p.edit()
            .putString(KEY_PROFILES, encode(list))
            .putString(KEY_ACTIVE_ID, profileId(profile))
            .putString(KEY_NAME, profile.name)
            .putString(KEY_DATE, profile.birthDate)
            .putString(KEY_GENDER, profile.gender)
            .putString(KEY_TIME, profile.birthTime)
            .putString(KEY_PLACE, profile.birthPlace)
            .putString(KEY_MOON_RASHI, profile.birthMoonRashi)
            .putString(KEY_NAKSHATRA, profile.birthNakshatra)
            .apply()
    }

    /** Logout only deactivates the current user; saved users remain available. */
    fun deactivate(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACTIVE_ID).apply()
    }

    /** Update an existing saved profile while preserving its identity when possible. */
    fun update(context: Context, oldProfile: BirthProfile, newProfile: BirthProfile) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val list = loadAllInternal(p).toMutableList()
        val index = list.indexOfFirst { samePerson(it, oldProfile) }
        if (index < 0) { save(context, newProfile); return }
        list[index] = newProfile
        val activeId = p.getString(KEY_ACTIVE_ID, null)
        val oldId = profileId(oldProfile)
        val edit = p.edit().putString(KEY_PROFILES, encode(list))
        if (activeId == oldId) {
            edit.putString(KEY_ACTIVE_ID, profileId(newProfile))
                .putString(KEY_NAME, newProfile.name)
                .putString(KEY_DATE, newProfile.birthDate)
                .putString(KEY_GENDER, newProfile.gender)
                .putString(KEY_TIME, newProfile.birthTime)
                .putString(KEY_PLACE, newProfile.birthPlace)
                .putString(KEY_MOON_RASHI, newProfile.birthMoonRashi)
                .putString(KEY_NAKSHATRA, newProfile.birthNakshatra)
        }
        edit.apply()
    }

    /** Explicitly remove one saved profile if the user later chooses to delete it. */
    fun remove(context: Context, profile: BirthProfile) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val remaining = loadAllInternal(p).filterNot { samePerson(it, profile) }
        val activeId = p.getString(KEY_ACTIVE_ID, null)
        val edit = p.edit().putString(KEY_PROFILES, encode(remaining))
        if (activeId == profileId(profile)) edit.remove(KEY_ACTIVE_ID)
        if (remaining.isEmpty()) {
            edit.remove(KEY_NAME).remove(KEY_DATE).remove(KEY_GENDER).remove(KEY_TIME)
                .remove(KEY_PLACE).remove(KEY_MOON_RASHI).remove(KEY_NAKSHATRA)
        }
        edit.apply()
    }

    /** Legacy compatibility: do not erase the profile data. */
    fun clear(context: Context) = deactivate(context)

    private fun samePerson(a: BirthProfile, b: BirthProfile): Boolean =
        a.name.trim().equals(b.name.trim(), ignoreCase = true) &&
            a.birthDate == b.birthDate && a.birthTime == b.birthTime &&
            a.birthPlace.trim().equals(b.birthPlace.trim(), ignoreCase = true)

    private fun profileId(p: BirthProfile): String =
        listOf(
            p.name.trim().lowercase(),
            p.birthDate,
            p.birthTime,
            p.birthPlace.trim().lowercase()
        ).joinToString("\u001f")

    private fun encode(list: List<BirthProfile>): String {
        val array = JSONArray()
        list.forEach { p ->
            array.put(JSONObject().apply {
                put("name", p.name)
                put("birthDate", p.birthDate)
                put("gender", p.gender)
                put("birthTime", p.birthTime)
                put("birthPlace", p.birthPlace)
                put("birthMoonRashi", p.birthMoonRashi)
                put("birthNakshatra", p.birthNakshatra)
            })
        }
        return array.toString()
    }

    private fun loadAllInternal(p: android.content.SharedPreferences): List<BirthProfile> {
        val json = p.getString(KEY_PROFILES, null)
        if (!json.isNullOrBlank()) {
            return runCatching {
                val array = JSONArray(json)
                buildList {
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        add(BirthProfile(
                            o.optString("name"), o.optString("birthDate"),
                            o.optString("gender", "Male"), o.optString("birthTime"),
                            o.optString("birthPlace"), o.optString("birthMoonRashi"),
                            o.optString("birthNakshatra")
                        ))
                    }
                }.filter { it.name.isNotBlank() && it.birthDate.isNotBlank() }
            }.getOrDefault(emptyList())
        }
        val legacy = loadLegacy(p)
        return if (legacy != null) listOf(legacy) else emptyList()
    }

    private fun loadLegacy(p: android.content.SharedPreferences): BirthProfile? {
        val name = p.getString(KEY_NAME, null) ?: return null
        val date = p.getString(KEY_DATE, null) ?: return null
        val gender = p.getString(KEY_GENDER, null) ?: return null
        val time = p.getString(KEY_TIME, null) ?: return null
        val place = p.getString(KEY_PLACE, null) ?: return null
        val moonRashi = p.getString(KEY_MOON_RASHI, null) ?: return null
        val nakshatra = p.getString(KEY_NAKSHATRA, null) ?: ""
        return BirthProfile(name, date, gender, time, place, moonRashi, nakshatra)
    }
}
