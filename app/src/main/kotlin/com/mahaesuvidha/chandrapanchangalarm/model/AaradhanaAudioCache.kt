package com.mahaesuvidha.chandrapanchangalarm.model

import android.content.Context
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

/**
 * Creates persistent offline audio for the currently selected Nakshatra/Yoga/Karana mantra.
 * The APK contains no audio. Android's installed Sanskrit/Hindi TTS voice is synthesized
 * once into app-private files and the resulting file is reused for later Aaradhana playback.
 */
object AaradhanaAudioCache {
    private const val DIR = "aaradhana_audio"
    private fun dir(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(context: Context, type: String, key: String): File {
        val safe = key.replace(Regex("[^\\p{L}\\p{N}_-]"), "_")
        return File(dir(context), "${type}_${safe}.wav")
    }

    fun exists(context: Context, type: String, key: String, mantra: String): Boolean =
        file(context, type, key).let { it.exists() && it.length() > 2048 }

    fun synthesize(context: Context, type: String, key: String, mantra: String, onDone: (Boolean) -> Unit) {
        val target = file(context, type, key)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) { onDone(false); return@TextToSpeech }
            val selected = listOf(Locale("hi", "IN"), Locale("sa", "IN"), Locale("mr", "IN"))
                .firstOrNull { locale -> ttsAvailable(tts, locale) }
            if (selected != null) tts.language = selected
            tts.setSpeechRate(0.72f)
            val result = tts.synthesizeToFile(mantra, android.os.Bundle(), target, "aaradhana_${type}_${key}")
            if (result != TextToSpeech.SUCCESS) {
                tts.shutdown(); onDone(false)
            } else {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    tts.shutdown(); onDone(target.exists() && target.length() > 2048)
                }, 700)
            }
        }
    }

    private fun ttsAvailable(tts: TextToSpeech, locale: Locale): Boolean =
        tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
}
