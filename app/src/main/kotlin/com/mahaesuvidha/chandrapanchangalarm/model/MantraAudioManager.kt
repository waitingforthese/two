package com.mahaesuvidha.chandrapanchangalarm.model

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * First-launch/on-demand downloader for traditional Navagraha pronunciation references.
 * Files are kept in app-private persistent storage so APK size is not increased.
 * These recordings are pronunciation/reference tracks; they are NOT silently substituted
 * for the selected short-japa mantra unless the recording text is an exact match.
 */
object MantraAudioManager {
    data class Track(val planet: Graha, val label: String, val url: String, val fileName: String)

    private const val DIR = "mantra_audio"
    private val tracks = listOf(
        Track(Graha.SURYA, "सूर्य — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Ravi-Sun-Level-3.mp3", "surya_reference.mp3"),
        Track(Graha.CHANDRA, "चंद्र — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Chandra-Moon-Level-3.mp3", "chandra_reference.mp3"),
        Track(Graha.MANGAL, "मंगळ — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Kuja-Mars-Level-3.mp3", "mangal_reference.mp3"),
        Track(Graha.BUDH, "बुध — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Budha-Mercury-Level-3.mp3", "budh_reference.mp3"),
        Track(Graha.GURU, "गुरु — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Guru-Jupiter-Level-3.mp3", "guru_reference.mp3"),
        Track(Graha.SHUKRA, "शुक्र — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Shukra-Venus-Level-3.mp3", "shukra_reference.mp3"),
        Track(Graha.SHANI, "शनि — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Shani-Saturn-Level-3.mp3", "shani_reference.mp3"),
        Track(Graha.RAHU, "राहू — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Raahu-Level-3-sun-moons-northern-point-of-intersection.mp3", "rahu_reference.mp3"),
        Track(Graha.KETU, "केतू — पारंपरिक Navagraha chant", "https://sadvidyafoundation.org/wp-content/uploads/2017/12/Ketu-Level-3-sun-moons-southern-point-of-intersection.mp3", "ketu_reference.mp3")
    )

    fun allTracks(): List<Track> = tracks
    fun directory(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }
    fun file(context: Context, planet: Graha): File? = tracks.firstOrNull { it.planet == planet }?.let { File(directory(context), it.fileName) }
    fun isDownloaded(context: Context, planet: Graha): Boolean = file(context, planet)?.let { it.exists() && it.length() > 4096 } == true
    fun downloadedCount(context: Context): Int = tracks.count { isDownloaded(context, it.planet) }

    fun downloadAll(context: Context, onProgress: (done: Int, total: Int, failed: List<String>) -> Unit) {
        val dir = directory(context)
        val failed = mutableListOf<String>()
        tracks.forEachIndexed { index, track ->
            if (!isDownloaded(context, track.planet)) {
                try {
                    val tmp = File(dir, track.fileName + ".part")
                    val conn = (URL(track.url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                    }
                    conn.connect()
                    if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                    conn.inputStream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
                    if (tmp.length() <= 4096) error("empty audio")
                    val target = File(dir, track.fileName)
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                } catch (e: Exception) {
                    File(dir, track.fileName + ".part").delete()
                    failed += track.planet.marathi
                }
            }
            onProgress(index + 1, tracks.size, failed.toList())
        }
    }
}
