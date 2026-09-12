package com.mahaesuvidha.chandrapanchangalarm.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/** Crash-safe foreground test player for cached Aaradhana audio. */
object AaradhanaTestAudioPlayer {
    private var player: MediaPlayer? = null

    @Synchronized
    fun stop() {
        val p = player
        player = null
        runCatching { p?.stop() }
        runCatching { p?.reset() }
        runCatching { p?.release() }
    }

    fun play(context: Context, file: File, onDone: (() -> Unit)? = null, onError: (() -> Unit)? = null) {
        stop()
        if (!file.exists() || file.length() <= 2048) { onError?.invoke(); return }
        val app = context.applicationContext
        val mp = MediaPlayer()
        player = mp
        fun fail() {
            synchronized(this) { if (player === mp) player = null }
            runCatching { mp.reset() }; runCatching { mp.release() }
            onError?.invoke()
        }
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener { prepared ->
                if (player !== prepared) { runCatching { prepared.release() }; return@setOnPreparedListener }
                runCatching { prepared.start() }.onFailure { fail() }
            }
            mp.setOnCompletionListener { completed ->
                synchronized(this) { if (player === completed) player = null }
                runCatching { completed.release() }
                onDone?.invoke()
            }
            mp.setOnErrorListener { _, _, _ -> fail(); true }
            mp.prepareAsync()
        }.onFailure { fail() }
    }
}
