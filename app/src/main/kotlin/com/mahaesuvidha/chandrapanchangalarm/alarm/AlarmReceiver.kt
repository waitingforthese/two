package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfileStore
import com.mahaesuvidha.chandrapanchangalarm.model.LiveMoonCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LivePanchangCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.LiveSunCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.NakshatraGuidanceCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.AaradhanaMaster
import com.mahaesuvidha.chandrapanchangalarm.model.PlanetaryTaraAaradhanaCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.AaradhanaAudioCache
import com.mahaesuvidha.chandrapanchangalarm.model.MantraAudioManager
import com.mahaesuvidha.chandrapanchangalarm.model.Graha
import com.mahaesuvidha.chandrapanchangalarm.settings.AlarmPrefs
import com.mahaesuvidha.chandrapanchangalarm.settings.LocationPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "चंद्र सूर्य अलार्म"
        val message = intent.getStringExtra("message") ?: "पंचांग बदल झाला आहे."
        val id = intent.getIntExtra("id", 1)
        val eventAt = intent.getLongExtra("eventAt", 0L)

        // RTC_WAKEUP wakes the CPU for delivery, but a short partial wakelock
        // keeps the receiver/TTS alive reliably on locked/dozing devices.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LifeAlarm:AlarmReceiver"
        )
        runCatching { wakeLock.acquire(3L * 60L * 1000L) }

        // Master OFF is the final safety gate. Do not show notification, speak,
        // or reschedule anything even if an already-delivered PendingIntent fires.
        val prefs = AlarmPrefs(context)
        if (!prefs.masterAlarm) {
            runCatching { AaradhanaVoiceSession.stop() }
            runCatching {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .cancel(id)
            }
            runCatching { if (wakeLock.isHeld) wakeLock.release() }
            return
        }

        val firedPrefs = context.getSharedPreferences(
            "life_alarm_fired_events", Context.MODE_PRIVATE
        )
        val firedKey = "fired_$id"
        if (eventAt > 0L && firedPrefs.getLong(firedKey, -1L) == eventAt) {
            runCatching { if (wakeLock.isHeld) wakeLock.release() }
            return
        }
        if (eventAt > 0L) firedPrefs.edit().putLong(firedKey, eventAt).apply()

        // Consume only after the duplicate guard. This prevents a late duplicate
        // broadcast from cancelling the newly scheduled next event.
        AlarmScheduler(context.applicationContext).markDelivered(id)

        val isGuidanceNotification = id == 2 || id == 121 || id == 122 || id == 213

        if (isGuidanceNotification) {
            showNakshatraGuidanceNotification(context, id, eventAt)
        } else {
            showNotification(context, title, message, id, eventAt)
        }

        if (id in 341..349) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            Thread {
                try {
                    val profile = BirthProfileStore.load(appContext)
                    val planet = Graha.entries[(id - 341).coerceIn(0, Graha.entries.lastIndex)]
                    val mantra = AaradhanaMaster.forPlanet(planet).mantra
                    AaradhanaVoiceSession.speakRepeated(
                        appContext, id, mantra,
                        com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs(appContext).specialJapaCount,
                        pendingResult
                    ) {
                        AlarmScheduler(appContext).scheduleAll()
                        runCatching { if (wakeLock.isHeld) wakeLock.release() }
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Planetary Tara Aaradhana failed", t)
                    pendingResult.finish()
                    runCatching { if (wakeLock.isHeld) wakeLock.release() }
                }
            }.start()
            return
        }

        val aaradhanaChange = id in 131..133
        if (aaradhanaChange) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            Thread {
                try {
                    val p = LivePanchangCalculator.getCurrentPanchangState(LocationPrefs(appContext).latitude, LocationPrefs(appContext).longitude)
                    val moon = LiveMoonCalculator.getCurrentMoonState()
                    val (type, key, mantra) = when (id) {
                        2, 131 -> Triple("nakshatra", moon.nakshatra.marathi, AaradhanaMaster.forNakshatra(moon.nakshatra.marathi).mantra)
                        22, 132 -> Triple("yoga", p.yoga, AaradhanaMaster.forYoga(p.yoga).mantra)
                        else -> Triple("karana", p.karana, AaradhanaMaster.forKarana(p.karana).mantra)
                    }
                    val ap = com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs(appContext)
                    val cached = AaradhanaAudioCache.file(appContext, type, key)
                    AaradhanaVoiceSession.speakCachedSequence(appContext, id, listOf(AaradhanaVoiceSession.AudioStep(audioFile = cached, fallbackText = mantra)), ap.specialJapaCount, pendingResult) {
                        runCatching { if (wakeLock.isHeld) wakeLock.release() }
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Aaradhana voice failed", t)
                    pendingResult.finish()
                    runCatching { if (wakeLock.isHeld) wakeLock.release() }
                }
            }.start()
        } else if (id in 301..308) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            Thread {
                try {
                    val p = LivePanchangCalculator.getCurrentPanchangState(LocationPrefs(appContext).latitude, LocationPrefs(appContext).longitude)
                    val moon = LiveMoonCalculator.getCurrentMoonState()
                    val aarPrefs = com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs(appContext)
                    val nakMantra = AaradhanaMaster.forNakshatra(moon.nakshatra.marathi).mantra
                    val yogaMantra = AaradhanaMaster.forYoga(p.yoga).mantra
                    val karanaMantra = AaradhanaMaster.forKarana(p.karana).mantra
                    val mantraSteps = mutableListOf(
                        AaradhanaVoiceSession.AudioStep(audioFile = AaradhanaAudioCache.file(appContext, "nakshatra", moon.nakshatra.marathi), fallbackText = nakMantra),
                        AaradhanaVoiceSession.AudioStep(audioFile = AaradhanaAudioCache.file(appContext, "yoga", p.yoga), fallbackText = yogaMantra),
                        AaradhanaVoiceSession.AudioStep(audioFile = AaradhanaAudioCache.file(appContext, "karana", p.karana), fallbackText = karanaMantra)
                    )

                    // While a transit planet remains in Vipat / Pratyari / Vadha,
                    // include that planet's Aaradhana in every scheduled Nakshatra
                    // Aaradhana session. This is intentionally independent of the
                    // normal notification switches; only the planetary Tara master
                    // and that planet's own switch control inclusion.
                    val profile = BirthProfileStore.load(appContext)
                    if (aarPrefs.planetaryTaraAaradhana && profile?.birthNakshatra?.isNotBlank() == true) {
                        runCatching {
                            PlanetaryTaraAaradhanaCalculator.calculate(profile.birthNakshatra)
                                .filter { row ->
                                    row.isWarning && aarPrefs.isPlanetaryTaraEnabled(row.planet.name)
                                }
                                .forEach { row ->
                                    // Announce the exact planet + Tara relationship once before
                                    // that planet's mantra. The announcement itself is not repeated.
                                    val planetInfo = AaradhanaMaster.forPlanet(row.planet)
                                    mantraSteps.add(
                                        AaradhanaVoiceSession.AudioStep(
                                            audioFile = MantraAudioManager.file(appContext, row.planet),
                                            fallbackText = planetInfo.mantra
                                        )
                                    )
                                }
                        }.onFailure {
                            android.util.Log.e("LifeAlarm", "Planetary Tara Aaradhana merge failed", it)
                        }
                    }

                    val finalTaraAnnouncements = if (aarPrefs.planetaryTaraAaradhana && profile?.birthNakshatra?.isNotBlank() == true) {
                        runCatching {
                            PlanetaryTaraAaradhanaCalculator.calculate(profile.birthNakshatra)
                                .filter { row -> row.isWarning && aarPrefs.isPlanetaryTaraEnabled(row.planet.name) }
                                .map { row -> "${row.planet.marathi} — ${row.tara} तारा" }
                        }.getOrDefault(emptyList())
                    } else emptyList()

                    AaradhanaVoiceSession.speakCachedSequenceThenAnnouncement(
                        appContext, id, mantraSteps,
                        aarPrefs.specialJapaCount, finalTaraAnnouncements, pendingResult
                    ) {
                        // The current 301 event has already been consumed. Reconcile
                        // immediately so the next interval is anchored to the saved
                        // schedule rather than to every incidental scheduleAll() call.
                        AlarmScheduler(appContext).scheduleAll()
                        runCatching { if (wakeLock.isHeld) wakeLock.release() }
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Special Aaradhana failed", t)
                    pendingResult.finish()
                    runCatching { if (wakeLock.isHeld) wakeLock.release() }
                }
            }.start()
        } else if (prefs.voiceAnnouncement) {
            val pendingResult = goAsync()
            val appContext = context.applicationContext
            Thread {
                try {
                    VoiceAnnouncement.speakEvent(appContext, id, message, eventAt, pendingResult) {
                        runCatching { if (wakeLock.isHeld) wakeLock.release() }
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Voice announcement failed", t)
                    pendingResult.finish()
                    runCatching { if (wakeLock.isHeld) wakeLock.release() }
                }
            }.start()
        } else {
            // No TTS work is pending.
            runCatching { if (wakeLock.isHeld) wakeLock.release() }
        }

        if (id in 1..3 || id in 11..13 || id in 21..27 || id == 121 || id == 122 || id in 131..133 || id in 301..308) {
            Thread {
                try {
                    AlarmScheduler(context.applicationContext).scheduleAll()
                } catch (t: Throwable) {
                    android.util.Log.e("LifeAlarm", "Failed to schedule next alarm after id=$id", t)
                }
            }.start()
        }
    }

    private fun showNakshatraGuidanceNotification(context: Context, id: Int, eventAt: Long) {
        val profile = BirthProfileStore.load(context.applicationContext) ?: return
        if (profile.birthNakshatra.isBlank()) return

        val guidance = runCatching {
            NakshatraGuidanceCalculator.currentGuidance(profile.birthNakshatra)
        }.getOrNull() ?: return

        val title = "🌙 नक्षत्र मार्गदर्शन — ${guidance.nakshatra}"
        val taraLine = "तारा: ${guidance.tara.marathi}"
        val timing = if (eventAt > 0L) "\n\nबदलाची वेळ: ${VoiceAnnouncement.formatEventTiming(eventAt)}" else ""
        val text = "$taraLine$timing\n\nकाय करावे: ${guidance.doText}\n\nकाय टाळावे: ${guidance.avoidText}"
        val bigText = NotificationCompat.BigTextStyle()
            .bigText(text)
            .setBigContentTitle(title)
            .setSummaryText("जन्म नक्षत्र: ${profile.birthNakshatra}")

        val channelId = "life_alarm_nakshatra_guidance_voice_v3"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "नक्षत्र मार्गदर्शन", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "चालू नक्षत्र, तारा, काय करावे आणि काय टाळावे"
            channel.enableVibration(true)
            channel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(
            context, com.mahaesuvidha.chandrapanchangalarm.MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 7000 + id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, AaradhanaStopReceiver::class.java).apply {
            action = AaradhanaStopReceiver.ACTION_STOP
            putExtra(AaradhanaStopReceiver.EXTRA_ID, id)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, 9000 + id, deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("$taraLine • जन्म नक्षत्र: ${profile.birthNakshatra}")
            .setStyle(bigText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(if (eventAt > 0L) eventAt else System.currentTimeMillis())
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .build()

        notificationManager.notify(7000 + id, notification)
    }

    private fun showNotification(context: Context, title: String, message: String, id: Int, eventAt: Long) {
        val channelId = "life_alarm_voice_v3"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Life Alarm Voice", NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "राशी, नक्षत्र, चरण आणि पंचांग बदलांसाठी Voice Announcement"
            channel.enableVibration(true)
            channel.setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            notificationManager.createNotificationChannel(channel)
        }

        val aaradhana = when (id) {
            2 -> { val s = LiveMoonCalculator.getCurrentMoonState(); val a = AaradhanaMaster.forNakshatra(s.nakshatra.marathi); "\n🙏 अधिदेवता: ${a.deity}\n📿 मंत्र: ${a.mantra}" }
            22 -> { val p = LivePanchangCalculator.getCurrentPanchangState(LocationPrefs(context).latitude, LocationPrefs(context).longitude); val a = AaradhanaMaster.forYoga(p.yoga); "\n🙏 अधिदेवता: ${a.deity}\n📿 मंत्र: ${a.mantra}" }
            23 -> { val p = LivePanchangCalculator.getCurrentPanchangState(LocationPrefs(context).latitude, LocationPrefs(context).longitude); val a = AaradhanaMaster.forKarana(p.karana); "\n🙏 अधिदेवता: ${a.deity}\n📿 मंत्र: ${a.mantra}" }
            in 341..349 -> { val planet = Graha.entries[(id - 341).coerceIn(0, Graha.entries.lastIndex)]; val a = AaradhanaMaster.forPlanet(planet); "\n⚠️ तारा आराधना: विपत / प्रत्यारी / वध\n🙏 देवता: ${a.deity}\n📿 मंत्र: ${a.mantra}" }
            else -> ""
        }
        val deleteIntent = Intent(context, AaradhanaStopReceiver::class.java).apply {
            action = AaradhanaStopReceiver.ACTION_STOP
            putExtra(AaradhanaStopReceiver.EXTRA_ID, id)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(context, 9000 + id, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val fullText = "$message\nबदलाची वेळ: ${VoiceAnnouncement.formatEventTiming(eventAt)}$aaradhana"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(fullText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(if (eventAt > 0L) eventAt else System.currentTimeMillis())
            .setAutoCancel(true)
            .setDeleteIntent(deletePendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}

private object VoiceAnnouncement {
    private const val PREFS = "life_alarm_voice_state"

    fun speakEvent(
        context: Context,
        id: Int,
        fallbackMessage: String,
        eventAt: Long,
        pendingResult: BroadcastReceiver.PendingResult,
        onFinished: (() -> Unit)? = null
    ) {
        val prefs = AlarmPrefs(context)
        if (!prefs.voiceAnnouncement) {
            pendingResult.finish()
            onFinished?.invoke()
            return
        }

        val text = buildAnnouncement(context, id, fallbackMessage, eventAt)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                pendingResult.finish()
                onFinished?.invoke()
                return@TextToSpeech
            }

            val mr = Locale("mr", "IN")
            val result = tts.setLanguage(mr)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val hi = Locale("hi", "IN")
                tts.setLanguage(hi)
                selectPreferredFemaleVoice(tts, hi, prefs)
            } else {
                // Prefer a female Marathi voice when the installed TTS engine
                // exposes one. Android does not provide a standard gender API,
                // so selection is based on the engine's advertised voice name.
                selectPreferredFemaleVoice(tts, mr, prefs)
            }

            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts.setSpeechRate(0.92f)
            tts.setPitch(1.10f)

            val music = if (prefs.backgroundMusic) {
                runCatching {
                    MediaPlayer.create(context, com.mahaesuvidha.chandrapanchangalarm.R.raw.voice_background)
                }.getOrNull()
            } else null

            music?.setVolume(0.16f, 0.16f)
            music?.isLooping = true
            music?.start()

            tts.setOnUtteranceProgressListener(object :
                android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    music?.release()
                    tts.shutdown()
                    pendingResult.finish()
                    onFinished?.invoke()
                }
                override fun onDone(utteranceId: String?) {
                    music?.release()
                    tts.shutdown()
                    pendingResult.finish()
                    onFinished?.invoke()
                }
            })

            val utteranceId = "life_alarm_${System.currentTimeMillis()}"
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun selectPreferredFemaleVoice(tts: TextToSpeech, locale: Locale, prefs: AlarmPrefs) {
        val voices = runCatching { tts.voices ?: emptyList() }.getOrDefault(emptyList())
        val languageVoices = voices.filter { voice ->
            voice.locale.language.equals(locale.language, ignoreCase = true)
        }
        val marathiVoices = languageVoices.sortedWith(
            compareByDescending<android.speech.tts.Voice> {
                it.locale.country.equals(locale.country, ignoreCase = true)
            }
        )
        if (marathiVoices.isEmpty()) return

        val femaleKeywords = listOf("female", "woman", "girl", "fem", "स्त्री", "महिला")
        val female = marathiVoices.firstOrNull { voice ->
            femaleKeywords.any { key -> voice.name.contains(key, ignoreCase = true) }
        }
        val saved = marathiVoices.firstOrNull { voice ->
            voice.name == prefs.preferredVoiceName &&
                femaleKeywords.any { key -> voice.name.contains(key, ignoreCase = true) }
        }
        // Never reuse a previously saved voice if it is not advertised as female.
        // Prefer an explicitly female Marathi voice; only fall back when the device
        // exposes no identifiable female Marathi/Indian voice at all.
        val selected = saved ?: female ?: marathiVoices.firstOrNull {
            it.locale.country.equals("IN", ignoreCase = true)
        } ?: marathiVoices.first()
        runCatching {
            tts.voice = selected
            prefs.preferredVoiceName = selected.name
            if (female == null) {
                android.util.Log.w("LifeAlarm", "No explicitly female Marathi TTS voice was exposed by the installed engine; using the best Marathi fallback.")
            }
        }
    }

    fun formatEventTiming(eventAt: Long): String {
        if (eventAt <= 0L) return "वेळ उपलब्ध नाही"
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val nowCal = Calendar.getInstance(tz)
        val eventCal = Calendar.getInstance(tz).apply { timeInMillis = eventAt }

        fun dayStart(c: Calendar): Calendar = Calendar.getInstance(tz).apply {
            set(Calendar.YEAR, c.get(Calendar.YEAR))
            set(Calendar.DAY_OF_YEAR, c.get(Calendar.DAY_OF_YEAR))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = dayStart(nowCal)
        val tomorrow = Calendar.getInstance(tz).apply {
            timeInMillis = today.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val eventDay = dayStart(eventCal)

        val hour12 = eventCal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = eventCal.get(Calendar.MINUTE)
        val am = eventCal.get(Calendar.AM_PM) == Calendar.AM
        val part = when {
            am && hour12 < 5 -> "पहाटे"
            am -> "सकाळी"
            !am && hour12 < 5 -> "दुपारी"
            !am && hour12 < 8 -> "संध्याकाळी"
            else -> "रात्री"
        }
        val time = if (minute == 0) {
            "$part $hour12 वाजता"
        } else {
            "$part $hour12 वाजून $minute मिनिटांनी"
        }

        return when {
            eventDay.timeInMillis == today.timeInMillis -> "आज $time"
            eventDay.timeInMillis == tomorrow.timeInMillis -> "उद्या $time"
            else -> {
                val months = arrayOf(
                    "जानेवारी", "फेब्रुवारी", "मार्च", "एप्रिल", "मे", "जून",
                    "जुलै", "ऑगस्ट", "सप्टेंबर", "ऑक्टोबर", "नोव्हेंबर", "डिसेंबर"
                )
                "${eventCal.get(Calendar.DAY_OF_MONTH)} ${months[eventCal.get(Calendar.MONTH)]} ${eventCal.get(Calendar.YEAR)} रोजी $time"
            }
        }
    }

    private fun buildAnnouncement(context: Context, id: Int, fallback: String, eventAt: Long): String {
        val now = System.currentTimeMillis()
        val timeZone = TimeZone.getTimeZone("Asia/Kolkata")

        fun timeOnly(millis: Long): String {
            val f = SimpleDateFormat("h:mm a", Locale.ENGLISH)
            f.timeZone = timeZone
            val raw = f.format(Date(millis))
            return raw.replace("AM", "am").replace("PM", "pm")
        }

        fun until(millis: Long): String =
            if (millis > now) "हे ${formatEventTiming(millis)} पर्यंत राहील." else ""

        return runCatching {
            when (id) {
                1, 201 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    "नमस्कार! चंद्र राशीमध्ये बदल झाला आहे. आता ${s.rashi.marathi} राशी सुरू झाली आहे. ${until(s.nextRashiMillis)}"
                }
                2, 121, 202 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    "नमस्कार! नक्षत्रामध्ये बदल झाला आहे. आता ${s.nakshatra.marathi} नक्षत्र सुरू झाले आहे. ${until(s.nextNakshatraMillis)}"
                }
                122 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    val base = "नमस्कार! नक्षत्र मार्गदर्शनाची सूचना आहे. सध्या ${s.nakshatra.marathi} नक्षत्र सुरू आहे. ${until(s.nextNakshatraMillis)}"
                    val profile = BirthProfileStore.load(context)
                    if (profile != null && profile.birthNakshatra.isNotBlank()) {
                        val g = NakshatraGuidanceCalculator.currentGuidance(profile.birthNakshatra, now)
                        "$base ${g.tara.marathi} तारा. काय करावे: ${g.doText} काय टाळावे: ${g.avoidText}"
                    } else base
                }
                213 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    val base = "नमस्कार! नक्षत्र मार्गदर्शनाची चाचणी सूचना आहे. सध्या ${s.nakshatra.marathi} नक्षत्र सुरू आहे. ${until(s.nextNakshatraMillis)}"
                    val profile = BirthProfileStore.load(context)
                    if (profile != null && profile.birthNakshatra.isNotBlank()) {
                        val g = NakshatraGuidanceCalculator.currentGuidance(profile.birthNakshatra, now)
                        "$base ${g.tara.marathi} तारा. काय करावे: ${g.doText} काय टाळावे: ${g.avoidText}"
                    } else base
                }
                3, 203 -> {
                    val s = LiveMoonCalculator.getCurrentMoonState()
                    "नमस्कार! नक्षत्र चरणामध्ये बदल झाला आहे. आता ${s.pada} वा चरण सुरू झाला आहे. ${until(s.nextCharanMillis)}"
                }
                11, 204 -> {
                    val s = LiveSunCalculator.getCurrentSunState()
                    "नमस्कार! सूर्य राशीमध्ये बदल झाला आहे. आता ${s.rashi.marathi} राशी सुरू झाली आहे. ${until(s.nextRashiMillis)}"
                }
                12, 205 -> {
                    val s = LiveSunCalculator.getCurrentSunState()
                    "नमस्कार! सूर्य नक्षत्रामध्ये बदल झाला आहे. आता ${s.nakshatra.marathi} नक्षत्र सुरू झाले आहे. ${until(s.nextNakshatraMillis)}"
                }
                13, 206 -> {
                    val s = LiveSunCalculator.getCurrentSunState()
                    "नमस्कार! सूर्य नक्षत्र चरणामध्ये बदल झाला आहे. आता ${s.pada} वा चरण सुरू झाला आहे. ${until(s.nextCharanMillis)}"
                }
                21, 22, 23, 24, 26, 27, 207, 208, 209, 210, 211, 212 -> {
                    val p = LivePanchangCalculator.getCurrentPanchangState(
                        LocationPrefs(context).latitude, LocationPrefs(context).longitude
                    )
                    when (id) {
                        21, 207 -> "नमस्कार! तिथीमध्ये बदल झाला आहे. आता ${p.tithi} तिथी सुरू झाली आहे. ${until(p.nextTithiMillis)}"
                        22, 208 -> "नमस्कार! योगामध्ये बदल झाला आहे. आता ${p.yoga} योग सुरू झाला आहे. ${until(p.nextYogaMillis)}"
                        23, 209 -> "नमस्कार! करणामध्ये बदल झाला आहे. आता ${p.karana} करण सुरू झाले आहे. ${until(p.nextKaranaMillis)}"
                        24, 210 -> "नमस्कार! पक्षामध्ये बदल झाला आहे. आता ${p.paksha} पक्ष सुरू झाला आहे. ${until(p.nextPakshaMillis)}"
                        26, 211 -> "नमस्कार! प्रहरामध्ये बदल झाला आहे. आता ${p.prahar} प्रहर सुरू झाला आहे. ${until(p.nextPraharMillis)}"
                        27, 212 -> "नमस्कार! लग्नामध्ये बदल झाला आहे. आता ${p.lagna} लग्न सुरू झाले आहे. ${until(p.nextLagnaMillis)}"
                        else -> "नमस्कार! $fallback"
                    }
                }
                in 341..349 -> {
                    val profile = BirthProfileStore.load(context)
                    val planet = Graha.entries[(id - 341).coerceIn(0, Graha.entries.lastIndex)]
                    val row = profile?.birthNakshatra?.takeIf { it.isNotBlank() }?.let { birth ->
                        PlanetaryTaraAaradhanaCalculator.calculate(birth).firstOrNull { it.planet == planet }
                    }
                    if (row != null) {
                        "नमस्कार! ${planet.marathi} गोचर ग्रह ${row.nakshatra} नक्षत्रात आला आहे. सध्या ${row.tara} तारा आहे. नक्षत्र ${PlanetaryTaraAaradhanaCalculator.format(row.startMillis)} ते ${PlanetaryTaraAaradhanaCalculator.format(row.endMillis)} आहे."
                    } else "नमस्कार! ${planet.marathi} ग्रह तारा आराधना सुरू आहे."
                }
                214 -> "नमस्कार! आजच्या वेळेची चाचणी आहे. हा संदेश ${formatEventTiming(eventAt)} या वेळेसाठी आहे."
                215 -> "नमस्कार! उद्याच्या वेळेची चाचणी आहे. हा संदेश ${formatEventTiming(eventAt)} या वेळेसाठी आहे."
                216 -> "नमस्कार! पुढील तारखेची चाचणी आहे. हा संदेश ${formatEventTiming(eventAt)} या वेळेसाठी आहे."
                else -> "नमस्कार! $fallback"
            }
        }.getOrElse {
            "नमस्कार! $fallback"
        }
    }
}
