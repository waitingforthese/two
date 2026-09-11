package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mahaesuvidha.chandrapanchangalarm.settings.AlarmPrefs
import com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs
import java.util.Locale

/** One cancellable TTS session for Aaradhana. */
object AaradhanaVoiceSession {
    private var tts: TextToSpeech? = null
    private var sessionId: Int = -1
    private var pending: android.content.BroadcastReceiver.PendingResult? = null
    private var finishedCallback: (() -> Unit)? = null

    @Synchronized
    fun stop(id: Int? = null) {
        if (id != null && sessionId != id) return
        val oldTts = tts
        val oldPending = pending
        val oldCallback = finishedCallback
        tts = null
        pending = null
        finishedCallback = null
        sessionId = -1
        runCatching { oldTts?.stop() }
        runCatching { oldTts?.shutdown() }
        runCatching { oldPending?.finish() }
        runCatching { oldCallback?.invoke() }
    }

    fun speakRepeated(
        context: Context,
        id: Int,
        mantra: String,
        count: Int,
        result: android.content.BroadcastReceiver.PendingResult,
        onFinished: (() -> Unit)? = null
    ) = start(context, id, listOf(mantra), count, result, onFinished)

    fun speakSequence(
        context: Context,
        id: Int,
        mantras: List<String>,
        eachCount: Int,
        result: android.content.BroadcastReceiver.PendingResult,
        onFinished: (() -> Unit)? = null
    ) = start(context, id, mantras, eachCount, result, onFinished)

    /** Immediate user-requested preview. It is not an alarm and therefore is allowed even when Master Alarm is OFF. */
    fun speakPreview(
        context: Context,
        id: Int,
        mantras: List<String>,
        eachCount: Int
    ) = start(context, id, mantras, eachCount, null, null)

    /** Announces special context once, then performs the supplied mantra sequence. */
    fun speakAnnouncementAndSequence(
        context: Context,
        id: Int,
        announcements: List<String>,
        mantras: List<String>,
        eachCount: Int,
        result: android.content.BroadcastReceiver.PendingResult?,
        onFinished: (() -> Unit)? = null
    ) = startWithAnnouncements(context, id, announcements, mantras, eachCount, result, onFinished)

    private fun start(
        context: Context,
        id: Int,
        mantras: List<String>,
        eachCount: Int,
        result: android.content.BroadcastReceiver.PendingResult?,
        onFinished: (() -> Unit)?
    ) = startWithAnnouncements(context, id, emptyList(), mantras, eachCount, result, onFinished)

    private fun startWithAnnouncements(
        context: Context,
        id: Int,
        announcements: List<String>,
        mantras: List<String>,
        eachCount: Int,
        result: android.content.BroadcastReceiver.PendingResult?,
        onFinished: (() -> Unit)?
    ) {
        stop()
        sessionId = id
        pending = result
        finishedCallback = onFinished
        val app = context.applicationContext
        val prefs = AlarmPrefs(app)
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(app) { status ->
            if (status != TextToSpeech.SUCCESS || sessionId != id) {
                runCatching { engine.shutdown() }
                if (sessionId == id) stop(id)
                return@TextToSpeech
            }

            val selectedLocale = configureVoice(engine, prefs)
            engine.setLanguage(selectedLocale)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // Slower speech gives Devanagari/Sanskrit words noticeably clearer boundaries.
            engine.setSpeechRate(AaradhanaPrefs(app).speechRate)
            engine.setPitch(1.00f)

            val announcementUtterances = announcements
                .filter { it.isNotBlank() }
                .map(::pronounce)
            val mantraUtterances = mantras
                .filter { it.isNotBlank() }
                .flatMap { mantraText -> List(eachCount.coerceAtLeast(1)) { pronounce(mantraText) } }
            val utterances = announcementUtterances + mantraUtterances

            if (utterances.isEmpty()) {
                stop(id)
                return@TextToSpeech
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    if (sessionId == id) stop(id)
                }
                override fun onDone(utteranceId: String?) {
                    if (sessionId == id && utteranceId == "aaradhana_${id}_last") stop(id)
                }
            })

            // Queue the complete session. This makes tts.stop() reliably clear every
            // remaining repetition when the notification is swiped away.
            utterances.forEachIndexed { index, text ->
                val utteranceId = if (index == utterances.lastIndex) {
                    "aaradhana_${id}_last"
                } else {
                    "aaradhana_${id}_$index"
                }
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                if (sessionId != id) return@forEachIndexed
                engine.speak(text, queueMode, null, utteranceId)
            }
        }
        tts = engine
    }

    private fun configureVoice(tts: TextToSpeech, prefs: AlarmPrefs): Locale {
        val voices = runCatching { tts.voices ?: emptySet() }.getOrDefault(emptySet())
        val mrVoices = voices.filter { it.locale.language.equals("mr", true) }
        val hiVoices = voices.filter { it.locale.language.equals("hi", true) }
        val femaleKeys = listOf("female", "woman", "girl", "fem", "महिला", "स्त्री")
        val isFemale = { v: android.speech.tts.Voice -> femaleKeys.any { v.name.contains(it, true) } }

        val savedMr = mrVoices.firstOrNull { it.name == prefs.preferredVoiceName }
        val femaleMr = mrVoices.firstOrNull(isFemale)
        val indianMr = mrVoices.firstOrNull { it.locale.country.equals("IN", true) }
        val femaleHi = hiVoices.firstOrNull(isFemale)
        val indianHi = hiVoices.firstOrNull { it.locale.country.equals("IN", true) }

        val selected = savedMr ?: femaleMr ?: indianMr ?: femaleHi ?: indianHi ?: voices.firstOrNull()
        if (selected != null) {
            runCatching { tts.voice = selected }
            prefs.preferredVoiceName = selected.name
            return selected.locale
        }
        return Locale("mr", "IN")
    }

    /** Speech text is separate from display text so Sanskrit/Marathi TTS has clearer pronunciation. */
    private fun pronounce(text: String): String {
        return text
            .replace("ॐ", "ओम् ")
            .replace("  ", " ")
            .trim()
    }
}
