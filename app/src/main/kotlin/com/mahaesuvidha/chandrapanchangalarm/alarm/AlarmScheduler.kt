package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

import com.mahaesuvidha.chandrapanchangalarm.model.LiveMoonCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LiveSunCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LivePanchangCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfileStore
import com.mahaesuvidha.chandrapanchangalarm.model.NakshatraGuidanceCalculator
import com.mahaesuvidha.chandrapanchangalarm.settings.AlarmPrefs
import com.mahaesuvidha.chandrapanchangalarm.settings.LocationPrefs

class AlarmScheduler(
    private val context: Context
) {

    private val alarmManager =
        context.getSystemService(
            AlarmManager::class.java
        )

    /**
     * Stores the exact event currently represented by each alarm ID.
     * This makes scheduleAll() idempotent: repeated location/UI/boot
     * refreshes do not recreate the same alarm or produce duplicates.
     */
    private val scheduledPrefs =
        context.getSharedPreferences(
            "life_alarm_scheduled_events",
            Context.MODE_PRIVATE
        )

    // ==========================================
    // TEST ALARMS
    // ==========================================

    fun scheduleRashiAlarm(
        delayMillis: Long
    ) {
        schedule(
            id = 101,
            at = System.currentTimeMillis() + delayMillis,
            title = "🌙 चंद्र राशी बदल",
            message = "ही राशी बदलाची टेस्ट सूचना आहे.",
            soundResource = "rashi"
        )
    }

    fun scheduleNakshatraAlarm(
        delayMillis: Long
    ) {
        schedule(
            id = 102,
            at = System.currentTimeMillis() + delayMillis,
            title = "🌙 चंद्र नक्षत्र बदल",
            message = "ही नक्षत्र बदलाची टेस्ट सूचना आहे.",
            soundResource = "nakshatra"
        )
    }

    fun scheduleCharanAlarm(
        delayMillis: Long
    ) {
        schedule(
            id = 103,
            at = System.currentTimeMillis() + delayMillis,
            title = "🌙 चंद्र चरण बदल",
            message = "ही चरण बदलाची टेस्ट सूचना आहे.",
            soundResource = "charan"
        )
    }

    fun schedulePraharAlarm(
        delayMillis: Long
    ) {
        schedule(
            id = 104,
            at = System.currentTimeMillis() + delayMillis,
            title = "⌛ प्रहर बदल",
            message = "ही प्रहर बदलाची टेस्ट सूचना आहे."
        )
    }

    fun scheduleLagnaAlarm(
        delayMillis: Long
    ) {
        schedule(
            id = 105,
            at = System.currentTimeMillis() + delayMillis,
            title = "⭐ लग्न बदल",
            message = "ही लग्न बदलाची टेस्ट सूचना आहे."
        )
    }

    // ==========================================
    // SCHEDULE ALL ALARMS
    // ==========================================

    fun scheduleAll() {
        synchronized(GLOBAL_SCHEDULE_LOCK) {
            scheduleAllInternal()
        }
    }

    private fun scheduleAllInternal() {

        val prefs = AlarmPrefs(context)
        if (!prefs.masterAlarm) {
            // Master OFF means absolutely no Life Alarm should remain scheduled.
            cancelAll()
            return
        }
        val location = LocationPrefs(context)

        scheduleSpecialAaradhana()
        schedulePlanetaryTaraAaradhana()

        // Personalized Nakshatra Guidance: the change notification and the
        // 3-hour reminder use the same live Nakshatra/Tara-Bala calculation
        // and the same Do/Avoid text shown on the Guidance card.
        scheduleNakshatraGuidanceAlarms()

        // IMPORTANT: do not call cancelAll() here. scheduleAll() can be
        // triggered by GPS updates, boot and an alarm firing. Cancelling
        // everything first creates unnecessary churn and can momentarily
        // remove a valid alarm. Each stable ID is instead reconciled below.

        // ==========================================
        // MOON ALARMS — IDs 1..3
        // ==========================================
        if (prefs.moonRashi || prefs.moonNakshatra || prefs.moonCharan) {
            val moon = LiveMoonCalculator.getCurrentMoonState()

            reconcile(
                id = 1,
                enabled = prefs.moonRashi,
                at = moon.nextRashiMillis,
                title = "🌙 चंद्र राशी बदल",
                message = moon.nextRashi,
                soundResource = "rashi"
            )
            reconcile(
                id = 2,
                enabled = prefs.moonNakshatra,
                at = moon.nextNakshatraMillis,
                title = "🌙 चंद्र नक्षत्र बदल",
                message = moon.nextNakshatra,
                soundResource = "nakshatra"
            )
            reconcile(
                id = 3,
                enabled = prefs.moonCharan,
                at = moon.nextCharanMillis,
                title = "🌙 चंद्र चरण बदल",
                message = moon.nextCharan,
                soundResource = "charan"
            )
        } else {
            cancel(1); cancel(2); cancel(3)
        }

        // Independent change-triggered Aaradhana alarms. These are deliberately
        // separate from Notification Settings: disabling a notification must not
        // disable its Aaradhana. IDs 131..133 are voice/notification Aaradhana only.
        val aar = com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs(context.applicationContext)
        val moonNow = LiveMoonCalculator.getCurrentMoonState()
        reconcile(131, aar.nakshatraChangeAaradhana, moonNow.nextNakshatraMillis,
            "🕉️ नक्षत्र बदल आराधना", "नक्षत्र बदलानंतर आराधना", "nakshatra")

        // Yoga/Karana require the live panchang calculation; calculate only when
        // either independent Aaradhana is enabled.
        if (aar.yogaChangeAaradhana || aar.karanaChangeAaradhana) {
            val ap = LivePanchangCalculator.getCurrentPanchangState(location.latitude, location.longitude)
            reconcile(132, aar.yogaChangeAaradhana, ap.nextYogaMillis,
                "🕉️ योग बदल आराधना", "योग बदलानंतर आराधना", "yog_badal")
            reconcile(133, aar.karanaChangeAaradhana, ap.nextKaranaMillis,
                "🕉️ करण बदल आराधना", "करण बदलानंतर आराधना", "karan_badal")
        } else {
            cancel(132); cancel(133)
        }

        // ==========================================
        // SUN ALARMS — IDs 11..13
        // ==========================================
        if (prefs.sunRashi || prefs.sunNakshatra || prefs.sunCharan) {
            val sun = LiveSunCalculator.getCurrentSunState()

            reconcile(
                id = 11,
                enabled = prefs.sunRashi,
                at = sun.nextRashiMillis,
                title = "☀️ सूर्य राशी बदल",
                message = sun.nextRashi,
                soundResource = "rashi"
            )
            reconcile(
                id = 12,
                enabled = prefs.sunNakshatra,
                at = sun.nextNakshatraMillis,
                title = "☀️ सूर्य नक्षत्र बदल",
                message = sun.nextNakshatra,
                soundResource = "nakshatra"
            )
            reconcile(
                id = 13,
                enabled = prefs.sunCharan,
                at = sun.nextCharanMillis,
                title = "☀️ सूर्य चरण बदल",
                message = sun.nextCharan,
                soundResource = "charan"
            )
        } else {
            cancel(11); cancel(12); cancel(13)
        }

        // ==========================================
        // PANCHANG ALARMS — IDs 21..24, 26, 27
        // Prahar and Lagna are calculated with the saved live location.
        // ==========================================
        val needsPanchang =
            prefs.tithiAlarm ||
            prefs.yogaAlarm ||
            prefs.karanaAlarm ||
            prefs.pakshaAlarm ||
            prefs.praharAlarm ||
            prefs.lagnaAlarm

        if (needsPanchang) {
            val panchang = LivePanchangCalculator.getCurrentPanchangState(
                latitude = location.latitude,
                longitude = location.longitude
            )

            reconcile(
                id = 21,
                enabled = prefs.tithiAlarm,
                at = panchang.nextTithiMillis,
                title = "🔔 तिथी बदल",
                message = "${panchang.tithi} → ${panchang.nextTithi}",
                soundResource = "tithi_badal"
            )
            reconcile(
                id = 22,
                enabled = prefs.yogaAlarm,
                at = panchang.nextYogaMillis,
                title = "🔔 योग बदल",
                message = "${panchang.yoga} → ${panchang.nextYoga}",
                soundResource = "yog_badal"
            )
            reconcile(
                id = 23,
                enabled = prefs.karanaAlarm,
                at = panchang.nextKaranaMillis,
                title = "🔔 करण बदल",
                message = "${panchang.karana} → ${panchang.nextKarana}",
                soundResource = "karan_badal"
            )
            reconcile(
                id = 24,
                enabled = prefs.pakshaAlarm,
                at = panchang.nextPakshaMillis,
                title = "🔔 पक्ष बदल",
                message = "${panchang.paksha} → ${panchang.nextPaksha}",
                soundResource = "paksh_badal"
            )
            reconcile(
                id = 26,
                enabled = prefs.praharAlarm,
                at = panchang.nextPraharMillis,
                title = "⌛ प्रहर बदल",
                message = "${panchang.prahar} → ${panchang.nextPrahar}",
                soundResource = "prahar_badal"
            )
            reconcile(
                id = 27,
                enabled = prefs.lagnaAlarm,
                at = panchang.nextLagnaMillis,
                title = "⭐ लग्न बदल",
                message = "${panchang.lagna} → ${panchang.nextLagna}",
                soundResource = "lagn_badal"
            )
        } else {
            cancel(21); cancel(22); cancel(23); cancel(24); cancel(26); cancel(27)
        }
    }

    private fun schedulePlanetaryTaraAaradhana() {
        val ap = com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs(context.applicationContext)
        val profile = BirthProfileStore.load(context.applicationContext)
        if (!ap.planetaryTaraAaradhana || profile == null || profile.birthNakshatra.isBlank()) {
            for (id in 341..349) cancel(id)
            return
        }
        val rows = runCatching {
            com.mahaesuvidha.chandrapanchangalarm.model.PlanetaryTaraAaradhanaCalculator.calculate(profile.birthNakshatra)
        }.getOrNull() ?: return
        rows.forEach { row ->
            val id = 341 + row.planet.ordinal
            val enabled = ap.isPlanetaryTaraEnabled(row.planet.name)
            val at = row.nextWarningStartMillis
            if (enabled && at > System.currentTimeMillis()) {
                reconcile(
                    id = id, enabled = true, at = at,
                    title = "⚠️ ${row.planet.marathi} — ${row.tara} तारा आराधना",
                    message = "${row.planet.marathi} ${row.nakshatra} नक्षत्रात प्रवेश करणार आहे.",
                    soundResource = "nakshatra"
                )
            } else {
                cancel(id)
            }
        }
    }

    private fun scheduleSpecialAaradhana() {
        val ap = com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs(context.applicationContext)
        val fixedTimes = parseFixedTimes(ap.specialFixedTimes)
        if (!ap.specialHourly || fixedTimes.isEmpty()) {
            for (id in 301..308) cancel(id)
            scheduledPrefs.edit().remove("special_fixed_signature").apply()
            return
        }

        val signature = fixedTimes.joinToString(",")
        val oldSignature = scheduledPrefs.getString("special_fixed_signature", null)
        if (oldSignature != signature) {
            for (id in 301..308) cancel(id)
            scheduledPrefs.edit().putString("special_fixed_signature", signature).apply()
        }

        val now = System.currentTimeMillis()
        fixedTimes.forEachIndexed { index, hm ->
            val id = 301 + index
            val nextAt = nextDailyTimeMillis(hm.first, hm.second, now)
            reconcile(
                id = id,
                enabled = true,
                at = nextAt,
                title = "🕉️ नक्षत्र आराधना • ${hm.first.toString().padStart(2, '0')}:${hm.second.toString().padStart(2, '0')}",
                message = "सध्याचे नक्षत्र • योग • करण आराधना",
                soundResource = null
            )
        }
        for (index in fixedTimes.size until 8) cancel(301 + index)
    }

    private fun parseFixedTimes(raw: String): List<Pair<Int, Int>> {
        return raw.split(',')
            .mapNotNull { token ->
                val parts = token.trim().split(':')
                if (parts.size != 2) return@mapNotNull null
                val h = parts[0].toIntOrNull() ?: return@mapNotNull null
                val m = parts[1].toIntOrNull() ?: return@mapNotNull null
                if (h !in 0..23 || m !in 0..59) null else h to m
            }
            .distinct()
            .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .take(8)
    }

    private fun nextDailyTimeMillis(hour: Int, minute: Int, now: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now + 1_000L) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /** Restart the special Aaradhana interval immediately after the user saves settings. */
    fun resetSpecialAaradhanaSchedule() {
        synchronized(GLOBAL_SCHEDULE_LOCK) {
            scheduledPrefs.edit().remove("special_next_at").remove("special_interval_hours").apply()
            for (id in 301..308) cancel(id)
        }
    }

    private fun scheduleNakshatraGuidanceAlarms() {
        val profile = BirthProfileStore.load(context.applicationContext)
        if (profile == null || profile.birthNakshatra.isBlank()) {
            cancel(121)
            cancel(122)
            return
        }

        val moon = LiveMoonCalculator.getCurrentMoonState()

        // If the existing Moon-Nakshatra alarm is enabled, turn that same
        // event into the personalized guidance notification in AlarmReceiver.
        // Otherwise use the dedicated guidance-change alarm. This prevents
        // duplicate notifications for the same Nakshatra transition.
        if (AlarmPrefs(context).moonNakshatra) {
            cancel(121)
        } else {
            reconcile(
                id = 121,
                enabled = true,
                at = moon.nextNakshatraMillis,
                title = "🌙 नक्षत्र मार्गदर्शन",
                message = "सध्याचे नक्षत्र व तारा मार्गदर्शन पाहा.",
                soundResource = "nakshatra"
            )
        }

        // Personalized guidance reminder every 3 hours. This reminder has
        // its own saved ON/OFF switch and must not mute any other Life Alarm.
        val guidancePrefs = AlarmPrefs(context)
        if (!guidancePrefs.nakshatraGuidanceEveryThreeHours) {
            cancel(122)
        } else {
            // ID 122 is a repeating 3-hour reminder. The receiver consumes
            // the delivered event before calling scheduleAll(), so this
            // alarm is always advanced to the next 3-hour slot.
            val existing = scheduledPrefs.getString("event_122", null)
            if (existing == null || !isAlarmScheduled(122)) {
                val nextReminder = System.currentTimeMillis() + 3L * 60L * 60L * 1000L
                reconcile(
                    id = 122,
                    enabled = true,
                    at = nextReminder,
                    title = "🌙 चालू नक्षत्र मार्गदर्शन",
                    message = "सध्याचे नक्षत्र, तारा, काय करावे आणि काय टाळावे पाहा.",
                    soundResource = "nakshatra"
                )
            }
        }
    }

    /**
     * Reconcile one real alarm. If the exact same event is already scheduled,
     * leave it alone. If the event time/message changed, replace it.
     */
    private fun reconcile(
        id: Int,
        enabled: Boolean,
        at: Long,
        title: String,
        message: String,
        soundResource: String? = null
    ) {
        if (!enabled || at <= 0L) {
            cancel(id)
            return
        }

        val now = System.currentTimeMillis()
        if (at <= now) {
            // A calculator must never create a past real alarm. The next
            // event will be calculated again after the current cycle.
            cancel(id)
            return
        }

        val signature = "$at|$title|$message"
        val oldSignature = scheduledPrefs.getString("event_$id", null)
        if (oldSignature == signature && isAlarmScheduled(id)) {
            return
        }

        schedule(
            id = id,
            at = at,
            title = title,
            message = message,
            soundResource = soundResource
        )
    }

    // ==========================================
    // GENERAL TEST ALARM
    // ==========================================

    fun scheduleTest(
        type: String
    ) {

        val soundResource =
            when {
                type.contains("राशी") -> "rashi"
                type.contains("नक्षत्र") -> "nakshatra"
                type.contains("चरण") -> "charan"
                else -> "alarm"
            }

        schedule(
            id = 99,
            at = System.currentTimeMillis() + 10_000L,
            title = "🔔 $type बदल Test",
            message = "$type Test Alarm आहे.",
            soundResource = soundResource
        )
    }

    // ==========================================
    // ==========================================
    // IMMEDIATE NOTIFICATION TEST
    // ==========================================

    /** Posts a notification immediately through AlarmReceiver, without waiting for an astronomical event. */
    fun scheduleImmediateNotificationTest() {
        schedule(
            id = 98,
            at = System.currentTimeMillis() + 2_000L,
            title = "🔔 Life Alarm Notification Test",
            message = "ही तात्काळ Notification चाचणी आहे. Notification दिसली आणि आवाज आला तर system ठीक आहे.",
            soundResource = null
        )
    }

    // ==========================================
    // FULL VOICE / NOTIFICATION TEST
    // ==========================================

    /**
     * Schedules every supported announcement event a few seconds apart.
     * AlarmReceiver uses the same LIVE calculation path as real alarms, so
     * this is a practical end-to-end test for notification + Marathi TTS.
     */
    fun scheduleFullVoiceTestSequence() {
        val tests = listOf(
            Triple(201, "🌙 चंद्र राशी Voice Test", "चंद्र राशी"),
            Triple(202, "⭐ चंद्र नक्षत्र Voice Test", "चंद्र नक्षत्र"),
            Triple(203, "🔔 चंद्र चरण Voice Test", "चंद्र चरण"),
            Triple(204, "☀️ सूर्य राशी Voice Test", "सूर्य राशी"),
            Triple(205, "☀️ सूर्य नक्षत्र Voice Test", "सूर्य नक्षत्र"),
            Triple(206, "☀️ सूर्य चरण Voice Test", "सूर्य चरण"),
            Triple(207, "📅 तिथी Voice Test", "तिथी"),
            Triple(208, "✨ योग Voice Test", "योग"),
            Triple(209, "🔔 करण Voice Test", "करण"),
            Triple(210, "🌗 पक्ष Voice Test", "पक्ष"),
            Triple(211, "⏳ प्रहर Voice Test", "प्रहर"),
            Triple(212, "⭐ लग्न Voice Test", "लग्न"),
            Triple(213, "🌙 नक्षत्र मार्गदर्शन Voice Test", "नक्षत्र मार्गदर्शन"),
            Triple(214, "📅 आज वेळ Voice Test", "आज वेळ"),
            Triple(215, "📅 उद्या वेळ Voice Test", "उद्या वेळ"),
            Triple(216, "📅 पुढील तारीख Voice Test", "पुढील तारीख")
        )

        val start = System.currentTimeMillis() + 2_500L
        tests.forEachIndexed { index, item ->
            val fireAt = start + index * 15_000L
            val announcementTime = when (item.first) {
                214 -> fireAt
                215 -> System.currentTimeMillis() + 24L * 60L * 60L * 1000L + 60_000L
                216 -> System.currentTimeMillis() + 3L * 24L * 60L * 60L * 1000L + 60_000L
                else -> fireAt
            }
            schedule(
                id = item.first,
                at = fireAt,
                title = item.second,
                message = "${item.third} चाचणी सूचना आहे.",
                soundResource = null,
                eventAtForAnnouncement = announcementTime
            )
        }
    }

    // Backward-compatible name used by the existing UI.
    fun schedulePanchangTestSequence() = scheduleFullVoiceTestSequence()

    /** Cancels all temporary test alarms without touching real alarms. */
    fun cancelAllTestAlarms() {
        cancel(98)
        cancel(99)
        cancel(101)
        cancel(102)
        cancel(103)
        cancel(104)
        cancel(105)
        cancel(121)
        cancel(122)
        for (id in 301..308) cancel(id)
        for (id in 201..216) cancel(id)
    }

    // ==========================================
    // MAIN SCHEDULE FUNCTION
    // ==========================================

    private fun schedule(
        id: Int,
        at: Long,
        title: String,
        message: String,
        soundResource: String? = null,
        eventAtForAnnouncement: Long? = null
    ) {

        val safeAt = maxOf(
            at,
            System.currentTimeMillis() + 2_000L
        )

        val exactAllowed =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()

        if (!exactAllowed) {
            // Exact alarms are preferred, but a missing exact-alarm grant must
            // never make a saved Life Alarm disappear. Fall back to an
            // allow-while-idle alarm so the event is still delivered while
            // the device is dozing/locked.
            android.util.Log.w(
                "LifeAlarm",
                "Exact alarm permission unavailable for id=$id; using inexact allow-while-idle fallback"
            )
        }

        cancel(id)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", id)
            putExtra("eventAt", eventAtForAnnouncement ?: safeAt)
            if (soundResource != null) {
                putExtra("soundResource", soundResource)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    safeAt,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    safeAt,
                    pendingIntent
                )
            }

            scheduledPrefs.edit()
                .putString("event_$id", "$at|$title|$message")
                .apply()
        } catch (t: Throwable) {
            android.util.Log.e(
                "LifeAlarm",
                "Unable to schedule alarm id=$id",
                t
            )
        }
    }

    /** Mark a one-shot alarm as consumed so scheduleAll() can create its next event. */
    fun markDelivered(id: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            runCatching { alarmManager.cancel(pendingIntent) }
            runCatching { pendingIntent.cancel() }
        }
        scheduledPrefs.edit().remove("event_$id").apply()
    }

    private fun isAlarmScheduled(id: Int): Boolean {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }

    // ==========================================
    // CANCEL ALL ALARMS
    // ==========================================

    fun cancelAll() {

        // All persistent alarms:
        // 1..3   = Moon: Rashi / Nakshatra / Charan
        // 11..13 = Sun:  Rashi / Nakshatra / Charan
        // 21..24 = Panchang: Tithi / Yoga / Karana / Paksha
        // 26     = Prahar
        // 27     = Lagna
        for (id in 1..3) {
            cancel(id)
        }

        for (id in 11..13) {
            cancel(id)
        }

        for (id in 21..27) {
            cancel(id)
        }
        for (id in 131..133) cancel(id)
        for (id in 341..349) cancel(id)

        // Test alarms
        cancel(99)
        cancel(101)
        cancel(102)
        cancel(103)
        cancel(104)
        cancel(105)
        cancel(121)
        cancel(122)
        cancel(301)

        for (id in 201..216) {
            cancel(id)
        }
    }

    // ==========================================
    // CANCEL SINGLE ALARM
    // ==========================================

    private fun cancel(
        id: Int
    ) {

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                id,
                Intent(
                    context,
                    AlarmReceiver::class.java
                ),
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

        if (pendingIntent != null) {
            try {
                alarmManager.cancel(pendingIntent)
            } catch (t: Throwable) {
                android.util.Log.e(
                    "LifeAlarm",
                    "Unable to cancel alarm id=$id",
                    t
                )
            }

            try {
                pendingIntent.cancel()
            } catch (_: Throwable) {
            }
        }

        scheduledPrefs.edit()
            .remove("event_$id")
            .apply()
    }

    companion object {
        private val GLOBAL_SCHEDULE_LOCK = Any()
    }
}
