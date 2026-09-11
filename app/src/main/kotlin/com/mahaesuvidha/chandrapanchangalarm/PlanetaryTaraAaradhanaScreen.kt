package com.mahaesuvidha.chandrapanchangalarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.media.MediaPlayer
import com.mahaesuvidha.chandrapanchangalarm.model.AaradhanaMaster
import com.mahaesuvidha.chandrapanchangalarm.model.MantraAudioManager
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfile
import com.mahaesuvidha.chandrapanchangalarm.model.Graha
import com.mahaesuvidha.chandrapanchangalarm.model.PlanetaryTaraAaradhanaCalculator
import com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs
import com.mahaesuvidha.chandrapanchangalarm.alarm.AlarmScheduler

@Composable
fun PlanetaryTaraAaradhanaScreen(profile: BirthProfile, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = androidx.compose.ui.platform.LocalContext.current
    var playingPlanet by remember { mutableStateOf<com.mahaesuvidha.chandrapanchangalarm.model.Graha?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { player?.release() } }
    val prefs = remember { AaradhanaPrefs(context.applicationContext) }
    var enabled by remember { mutableStateOf(prefs.planetaryTaraAaradhana) }
    val scope = rememberCoroutineScope()
    val planetSwitches = remember { mutableStateMapOf<String, Boolean>() }
    LaunchedEffect(profile.birthNakshatra) {
        Graha.entries.forEach { g -> planetSwitches[g.name] = prefs.isPlanetaryTaraEnabled(g.name) }
    }
    val rows = remember(profile.birthNakshatra) {
        PlanetaryTaraAaradhanaCalculator.calculate(profile.birthNakshatra)
    }
    val warningRows = rows.filter { it.isWarning }

    Column(Modifier.fillMaxSize().background(Color(0xFF07111F)).statusBarsPadding().navigationBarsPadding()) {
        Surface(Modifier.fillMaxWidth(), color = Color(0xFF07111F), shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← परत", color = Color.White) }
                Text("⚠️ ग्रह तारा आराधना", color = Color(0xFFFFC83D), fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                KundliReferenceButton(profile, textColor = Color.White)
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
                Column(Modifier.padding(14.dp)) {
                    Text("जन्म नक्षत्र: ${profile.birthNakshatra.ifBlank { "—" }}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("विपत / प्रत्यारी / वध या तीन तारा स्थिती येथे शोधल्या जातात.", color = Color.LightGray, fontSize = 13.sp)
                    Text("गोचर ग्रह एखाद्या अशुभ तारा-नक्षत्रात प्रवेश करतो तेव्हा त्या ग्रहाची आराधना सुरू करण्यासाठी स्वतंत्र Alarm ठेवता येतो.", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ग्रह तारा आराधना", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("प्रत्येक ग्रह स्वतंत्रपणे ON/OFF करता येईल.", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Switch(checked = enabled, onCheckedChange = {
                            enabled = it
                            prefs.planetaryTaraAaradhana = it
                            scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                                AlarmScheduler(context.applicationContext).scheduleAll()
                            }
                        })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            rows.forEach { row ->
                val planetEnabled = planetSwitches[row.planet.name] ?: prefs.isPlanetaryTaraEnabled(row.planet.name)
                val info = AaradhanaMaster.forPlanet(row.planet)
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = if (row.isWarning) Color(0xFF2A1E12) else Color(0xFF10253A))) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${row.planet.marathi}  •  ${String.format(java.util.Locale.US, "%.2f°", row.degreeInRashi)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Switch(checked = planetEnabled, enabled = enabled, onCheckedChange = { checked ->
                                // Keep this screen mounted: update Compose state first,
                                // persist the value, then reconcile alarms asynchronously.
                                planetSwitches[row.planet.name] = checked
                                prefs.setPlanetaryTaraEnabled(row.planet.name, checked)
                                scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                                    AlarmScheduler(context.applicationContext).scheduleAll()
                                }
                            })
                        }
                        Text("गोचर राशी: ${row.rashi}", color = Color.LightGray)
                        Text("गोचर नक्षत्र: ${row.nakshatra} • चरण ${row.pada}", color = Color.LightGray)
                        Text("तारा: ${row.tara}${if (row.isWarning) "  ⚠️ आराधना सक्रिय" else ""}", color = if (row.isWarning) Color(0xFFFFB74D) else Color(0xFFB9C4D0), fontWeight = FontWeight.Bold)
                        Text("नक्षत्र सुरू: ${PlanetaryTaraAaradhanaCalculator.format(row.startMillis)}", color = Color.White, fontSize = 13.sp)
                        Text("नक्षत्र समाप्त: ${PlanetaryTaraAaradhanaCalculator.format(row.endMillis)}", color = Color.White, fontSize = 13.sp)
                        if (row.nextWarningStartMillis > 0L) Text("पुढील ${"विपत / प्रत्यारी / वध"} सुरुवात: ${PlanetaryTaraAaradhanaCalculator.format(row.nextWarningStartMillis)}", color = Color(0xFFFFC83D), fontSize = 13.sp)
                        Text("🙏 ${info.deity}  •  📿 ${info.mantra}", color = Color(0xFFFFC83D), fontSize = 13.sp)
                        Text("संदर्भ: ${info.source}", color = Color(0xFFB9C4D0), fontSize = 11.sp)
                        if (info.audioReferenceUrl.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.audioReferenceUrl)))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎧 उच्चार / मंत्र recording reference उघडा", fontWeight = FontWeight.Bold)
                            }
                            Text(info.audioReferenceLabel, color = Color(0xFFB9C4D0), fontSize = 11.sp)
                            val localAudio = MantraAudioManager.file(context, row.planet)
                            if (localAudio?.exists() == true) {
                                OutlinedButton(
                                    onClick = {
                                        runCatching {
                                            player?.release()
                                            val mp = MediaPlayer().apply {
                                                setDataSource(localAudio.absolutePath)
                                                setOnCompletionListener { playingPlanet = null; it.release() }
                                                prepare()
                                                start()
                                            }
                                            player = mp
                                            playingPlanet = row.planet
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (playingPlanet == row.planet) "⏹️ रेकॉर्डिंग चालू आहे" else "🎙️ डाउनलोड केलेले उच्चार वाजवा", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            if (warningRows.isEmpty()) {
                Text("सध्या कोणताही गोचर ग्रह विपत / प्रत्यारी / वध तारा-नक्षत्रात नाही.", color = Color.LightGray, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text("टीप: ही पारंपरिक तारा-बळावर आधारित आराधना/निरीक्षण सुविधा आहे. ती निश्चित घटना किंवा अपघाताची भविष्यवाणी समजू नये.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
