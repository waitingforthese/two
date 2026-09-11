package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mahaesuvidha.chandrapanchangalarm.model.MantraAudioManager
import com.mahaesuvidha.chandrapanchangalarm.settings.AlarmPrefs
import com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs
import java.io.File
import java.util.Locale

/** One cancellable Aaradhana session. Cached recordings are preferred; TTS is fallback. */
object AaradhanaVoiceSession {
    private var tts: TextToSpeech? = null
    private var player: MediaPlayer? = null
    private var sessionId: Int = -1
    private var pending: android.content.BroadcastReceiver.PendingResult? = null
    private var finishedCallback: (() -> Unit)? = null

    data class AudioStep(val announcement: String? = null, val audioFile: File? = null, val fallbackText: String)

    @Synchronized
    fun stop(id: Int? = null) {
        if (id != null && sessionId != id) return
        val oldTts = tts; val oldPlayer = player; val oldPending = pending; val oldCallback = finishedCallback
        tts = null; player = null; pending = null; finishedCallback = null; sessionId = -1
        runCatching { oldTts?.stop() }; runCatching { oldTts?.shutdown() }; runCatching { oldPlayer?.stop() }; runCatching { oldPlayer?.release() }
        runCatching { oldPending?.finish() }; runCatching { oldCallback?.invoke() }
    }

    fun speakRepeated(context: Context, id: Int, mantra: String, count: Int, result: android.content.BroadcastReceiver.PendingResult, onFinished: (() -> Unit)? = null) =
        startTts(context, id, emptyList(), listOf(mantra), count, result, onFinished)

    fun speakSequence(context: Context, id: Int, mantras: List<String>, eachCount: Int, result: android.content.BroadcastReceiver.PendingResult, onFinished: (() -> Unit)? = null) =
        startTts(context, id, emptyList(), mantras, eachCount, result, onFinished)

    fun speakPreview(context: Context, id: Int, mantras: List<String>, eachCount: Int) =
        startTts(context, id, emptyList(), mantras, eachCount, null, null)

    fun speakAnnouncementAndSequence(context: Context, id: Int, announcements: List<String>, mantras: List<String>, eachCount: Int, result: android.content.BroadcastReceiver.PendingResult?, onFinished: (() -> Unit)? = null) =
        startTts(context, id, announcements, mantras, eachCount, result, onFinished)

    /** Plays cached files in order. Each missing/corrupt file falls back to TTS. */
    fun speakCachedSequence(context: Context, id: Int, steps: List<AudioStep>, eachCount: Int, result: android.content.BroadcastReceiver.PendingResult?, onFinished: (() -> Unit)? = null) {
        stop(); sessionId = id; pending = result; finishedCallback = onFinished
        playSteps(context.applicationContext, id, steps, eachCount.coerceAtLeast(1), 0)
    }

    /** Cached audio sequence first; optional announcements are spoken only after all audio. */
    fun speakCachedSequenceThenAnnouncement(context: Context, id: Int, steps: List<AudioStep>, eachCount: Int, finalAnnouncements: List<String>, result: android.content.BroadcastReceiver.PendingResult?, onFinished: (() -> Unit)? = null) {
        stop(); sessionId = id; pending = result; finishedCallback = onFinished
        playStepsThenAnnouncement(context.applicationContext, id, steps, eachCount.coerceAtLeast(1), 0, finalAnnouncements.filter { it.isNotBlank() })
    }

    private fun playStepsThenAnnouncement(context: Context, id: Int, steps: List<AudioStep>, count: Int, index: Int, announcements: List<String>) {
        if (sessionId != id) return
        if (index >= steps.size) {
            if (announcements.isEmpty()) { stop(id); return }
            speakAnnouncementsOnly(context, id, announcements, 0)
            return
        }
        val step = steps[index]
        playAudioOrFallback(context, id, step.copy(announcement = null), count) {
            playStepsThenAnnouncement(context, id, steps, count, index + 1, announcements)
        }
    }

    private fun speakAnnouncementsOnly(context: Context, id: Int, announcements: List<String>, index: Int) {
        if (sessionId != id) return
        if (index >= announcements.size) { stop(id); return }
        speakOne(context, id, announcements[index]) { speakAnnouncementsOnly(context, id, announcements, index + 1) }
    }

    private fun playSteps(context: Context, id: Int, steps: List<AudioStep>, count: Int, index: Int) {
        if (sessionId != id) return
        if (index >= steps.size) { stop(id); return }
        val step = steps[index]
        val next = { playSteps(context, id, steps, count, index + 1) }
        if (!step.announcement.isNullOrBlank()) {
            speakOne(context, id, step.announcement, { playAudioOrFallback(context, id, step, count, next) })
        } else playAudioOrFallback(context, id, step, count, next)
    }

    private fun playAudioOrFallback(context: Context, id: Int, step: AudioStep, count: Int, next: () -> Unit) {
        if (sessionId != id) return
        val file = step.audioFile
        if (file != null && file.exists() && file.length() > 2048) {
            playFileRepeated(context, id, file, step.fallbackText, count, next)
        } else {
            speakOne(context, id, step.fallbackText, next)
        }
    }

    private fun playFileRepeated(context: Context, id: Int, file: File, fallbackText: String, count: Int, next: () -> Unit) {
        var n = 0
        fun playOnce() {
            if (sessionId != id) return
            if (n >= count) { next(); return }
            n++
            val mp = MediaPlayer()
            player = mp
            runCatching {
                mp.setDataSource(file.absolutePath)
                mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                mp.setOnCompletionListener { runCatching { it.release() }; if (player === it) player = null; playOnce() }
                mp.setOnErrorListener { p, _, _ -> runCatching { p.release() }; if (player === p) player = null; speakOne(context, id, fallbackText, next); true }
                mp.prepare(); mp.start()
            }.onFailure { runCatching { mp.release() }; if (player === mp) player = null; speakOne(context, id, fallbackText, next) }
        }
        playOnce()
    }


    private fun speakOne(context: Context, id: Int, text: String, done: () -> Unit) {
        if (sessionId != id) return
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS || sessionId != id) { runCatching { engine.shutdown() }; if (sessionId == id) done(); return@TextToSpeech }
            val prefs = AlarmPrefs(context)
            val selected = configureVoice(engine, prefs)
            engine.language = selected
            engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            engine.setSpeechRate(AaradhanaPrefs(context).speechRate); engine.setPitch(1f)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) { runCatching { engine.shutdown() }; if (sessionId == id) done() }
                override fun onDone(utteranceId: String?) { runCatching { engine.shutdown() }; if (sessionId == id) done() }
            })
            engine.speak(pronounce(text), TextToSpeech.QUEUE_FLUSH, null, "cached_${id}_${System.nanoTime()}")
            tts = engine
        }
    }

    private fun startTts(context: Context, id: Int, announcements: List<String>, mantras: List<String>, eachCount: Int, result: android.content.BroadcastReceiver.PendingResult?, onFinished: (() -> Unit)?) {
        stop(); sessionId = id; pending = result; finishedCallback = onFinished
        val app = context.applicationContext; val prefs = AlarmPrefs(app)
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(app) { status ->
            if (status != TextToSpeech.SUCCESS || sessionId != id) { runCatching { engine.shutdown() }; if (sessionId == id) stop(id); return@TextToSpeech }
            val selectedLocale = configureVoice(engine, prefs); engine.language = selectedLocale
            engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            engine.setSpeechRate(AaradhanaPrefs(app).speechRate); engine.setPitch(1f)
            val utterances = announcements.filter { it.isNotBlank() }.map(::pronounce) + mantras.filter { it.isNotBlank() }.flatMap { m -> List(eachCount.coerceAtLeast(1)) { pronounce(m) } }
            if (utterances.isEmpty()) { stop(id); return@TextToSpeech }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) { if (sessionId == id) stop(id) }
                override fun onDone(utteranceId: String?) { if (sessionId == id && utteranceId == "aaradhana_${id}_last") stop(id) }
            })
            utterances.forEachIndexed { index, text ->
                val uid = if (index == utterances.lastIndex) "aaradhana_${id}_last" else "aaradhana_${id}_$index"
                engine.speak(text, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, uid)
            }
            tts = engine
        }
    }

    private fun configureVoice(tts: TextToSpeech, prefs: AlarmPrefs): Locale {
        val voices = runCatching { tts.voices ?: emptySet() }.getOrDefault(emptySet())
        val mr = voices.filter { it.locale.language.equals("mr", true) }; val hi = voices.filter { it.locale.language.equals("hi", true) }
        val femaleKeys = listOf("female", "woman", "girl", "fem", "महिला", "स्त्री")
        val female = { v: android.speech.tts.Voice -> femaleKeys.any { v.name.contains(it, true) } }
        val selected = mr.firstOrNull { it.name == prefs.preferredVoiceName } ?: mr.firstOrNull(female) ?: mr.firstOrNull { it.locale.country.equals("IN", true) } ?: hi.firstOrNull(female) ?: hi.firstOrNull { it.locale.country.equals("IN", true) } ?: voices.firstOrNull()
        if (selected != null) { runCatching { tts.voice = selected }; prefs.preferredVoiceName = selected.name; return selected.locale }
        return Locale("mr", "IN")
    }

    private fun pronounce(text: String) = text.replace("ॐ", "ओम् ").replace("  ", " ").trim()
}
