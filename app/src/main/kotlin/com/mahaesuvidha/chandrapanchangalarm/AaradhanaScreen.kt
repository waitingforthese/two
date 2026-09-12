package com.mahaesuvidha.chandrapanchangalarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahaesuvidha.chandrapanchangalarm.model.AaradhanaMaster
import com.mahaesuvidha.chandrapanchangalarm.model.AaradhanaAudioCache
import com.mahaesuvidha.chandrapanchangalarm.alarm.AlarmScheduler
import com.mahaesuvidha.chandrapanchangalarm.alarm.AaradhanaVoiceSession
import com.mahaesuvidha.chandrapanchangalarm.alarm.AaradhanaTestAudioPlayer
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfile
import com.mahaesuvidha.chandrapanchangalarm.model.LiveMoonCalculator
import com.mahaesuvidha.chandrapanchangalarm.model.PanchangState
import com.mahaesuvidha.chandrapanchangalarm.settings.AaradhanaPrefs

@Composable
fun AaradhanaScreen(
    profile: BirthProfile,
    panchang: PanchangState,
    onBack: () -> Unit,
    onOpenPlanetaryTara: (() -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    DisposableEffect(Unit) { onDispose { AaradhanaTestAudioPlayer.stop() } }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { AaradhanaPrefs(context.applicationContext) }
    var special by remember { mutableStateOf(prefs.specialHourly) }
    var japaCountText by remember { mutableStateOf(prefs.specialJapaCount.toString()) }
    var fixedTimesText by remember { mutableStateOf(prefs.specialFixedTimes) }
    var nakChangeAaradhana by remember { mutableStateOf(prefs.nakshatraChangeAaradhana) }
    var yogaChangeAaradhana by remember { mutableStateOf(prefs.yogaChangeAaradhana) }
    var karanaChangeAaradhana by remember { mutableStateOf(prefs.karanaChangeAaradhana) }
    var speechRate by remember { mutableStateOf(prefs.speechRate) }
    var savedPopup by remember { mutableStateOf(false) }
    val moon = remember { LiveMoonCalculator.getCurrentMoonState() }
    val nakInfo = AaradhanaMaster.forNakshatra(moon.nakshatra.marathi)
    val yogaInfo = AaradhanaMaster.forYoga(panchang.yoga)
    val karanaInfo = AaradhanaMaster.forKarana(panchang.karana)
    var audioReady by remember { mutableStateOf(false) }
    var audioBusy by remember { mutableStateOf(false) }
    var audioMessage by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Color(0xFF07111F)).statusBarsPadding().navigationBarsPadding()) {
        Surface(Modifier.fillMaxWidth(), color = Color(0xFF07111F), shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← परत", color = Color.White) }
                Text("🕉️ नक्षत्र आराधना", color = Color(0xFFFFC83D), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KundliReferenceButton(profile, textColor = Color.White)
                    TransitAnalysisButton(profile, textColor = Color.White)
                }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(14.dp)) {
        Spacer(Modifier.height(4.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("👤 ${profile.name}", color = Color.White, fontWeight = FontWeight.Bold)
                Text("जन्म नक्षत्र: ${profile.birthNakshatra.ifBlank { "—" }}", color = Color.LightGray)
                Text("चंद्र नक्षत्र: ${moon.nakshatra.marathi} • चरण ${moon.pada}", color = Color.LightGray)
                Text("गोचर नक्षत्र: ${moon.nakshatra.marathi}", color = Color.LightGray)
                Text("योग: ${panchang.yoga}", color = Color.LightGray)
                Text("करण: ${panchang.karana}", color = Color.LightGray)
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("🔔 बदलाची आराधना", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("नक्षत्र / योग / करण बदलल्यावर संबंधित देवता व मंत्रासह ११ जप आपोआप होतील.", color = Color.LightGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                MantraRow("🌟 नक्षत्र", moon.nakshatra.marathi, nakInfo.deity, nakInfo.mantra)
                MantraRow("🕉️ योग", panchang.yoga, yogaInfo.deity, yogaInfo.mantra)
                MantraRow("🔱 करण", panchang.karana, karanaInfo.deity, karanaInfo.mantra)
                Spacer(Modifier.height(10.dp))
                Text("🎧 नक्षत्र / योग / करण ऑडिओ", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("या दिवसाच्या तीन मंत्रांचे audio APK मध्ये ठेवलेले नाही. एकदा तयार/सेव्ह केल्यावर ते offline वापरता येतील.", color = Color.LightGray, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Button(
                    enabled = !audioBusy,
                    onClick = {
                        audioBusy = true
                        audioMessage = "ऑडिओ तयार होत आहे…"
                        val jobs = listOf(
                            Triple("nakshatra", moon.nakshatra.marathi, nakInfo.mantra),
                            Triple("yoga", panchang.yoga, yogaInfo.mantra),
                            Triple("karana", panchang.karana, karanaInfo.mantra)
                        )
                        var remaining = jobs.size
                        var failed = false
                        jobs.forEach { (type, key, mantra) ->
                            if (AaradhanaAudioCache.exists(context, type, key, mantra)) {
                                remaining--
                                if (remaining == 0) {
                                    audioBusy = false; audioReady = true; audioMessage = if (failed) "काही audio तयार झाले नाहीत." else "नक्षत्र + योग + करण audio तयार झाले."
                                }
                            } else {
                                AaradhanaAudioCache.synthesize(context, type, key, mantra) { ok ->
                                    failed = failed || !ok
                                    remaining--
                                    if (remaining == 0) {
                                        audioBusy = false; audioReady = !failed; audioMessage = if (failed) "काही audio तयार झाले नाहीत; पुन्हा प्रयत्न करा." else "नक्षत्र + योग + करण audio तयार झाले."
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (audioBusy) "⏳ तयार होत आहे…" else "⬇️ नक्षत्र + योग + करण ऑडिओ सेव्ह करा", fontWeight = FontWeight.Bold) }
                if (audioMessage.isNotBlank()) Text(audioMessage, color = Color(0xFFFFC83D), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (audioReady) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
                Column(Modifier.padding(14.dp)) {
                    Text("🎙️ Offline audio", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("हे audio याच device वर सेव्ह झाले आहेत. पुढील आराधनेत local playback जोडता येईल.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("⚠️ ग्रह तारा आराधना", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("सूर्य ते केतू या ९ गोचर ग्रहांसाठी जन्म नक्षत्रापासून विपत / प्रत्यारी / वध तारा-नक्षत्र शोधा. नक्षत्राची सुरुवात व समाप्त वेळ आणि स्वतंत्र Alarm सेट करता येईल.", color = Color.LightGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                if (onOpenPlanetaryTara != null) {
                    Button(onClick = onOpenPlanetaryTara, modifier = Modifier.fillMaxWidth()) {
                        Text("⚠️ ग्रह तारा आराधना उघडा", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
            Column(Modifier.padding(14.dp)) {
                Text("🕉️ विशेष आराधना", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("स्वयंचलित आराधना", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("ON असल्यास रोज ठरलेल्या घड्याळाच्या वेळेला आराधना होईल.", color = Color.LightGray, fontSize = 12.sp)
                    }
                    Switch(checked = special, onCheckedChange = {
                        special = it
                        prefs.specialHourly = it
                        val scheduler = AlarmScheduler(context.applicationContext)
                        if (it) scheduler.resetSpecialAaradhanaSchedule()
                        scheduler.scheduleAll()
                    })
                }

                Spacer(Modifier.height(12.dp))
                Text("⚙️ जपाची सेटिंग", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = fixedTimesText,
                    onValueChange = { fixedTimesText = it.filter { ch -> ch.isDigit() || ch == ':' || ch == ',' }.take(80) },
                    label = { Text("दररोज आराधना वेळा (HH:mm)") },
                    supportingText = { Text("उदा. 05:00,08:00,11:00,14:00,17:00,20:00,23:00 • कमाल 8 वेळा") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFFC83D), unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFFFFC83D), unfocusedLabelColor = Color.LightGray
                    )
                )

                Spacer(Modifier.height(10.dp))
                Text("🔔 बदलाची आराधना — Notification Settings पासून स्वतंत्र", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("खालील आराधना ON असेल तर संबंधित Alarm Settings OFF असली तरी आराधना होईल.", color = Color.LightGray, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🌙 नक्षत्र बदल आराधना", color = Color.White); Switch(nakChangeAaradhana, { nakChangeAaradhana = it; prefs.nakshatraChangeAaradhana = it }) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("✨ योग बदल आराधना", color = Color.White); Switch(yogaChangeAaradhana, { yogaChangeAaradhana = it; prefs.yogaChangeAaradhana = it }) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("🔔 करण बदल आराधना", color = Color.White); Switch(karanaChangeAaradhana, { karanaChangeAaradhana = it; prefs.karanaChangeAaradhana = it }) }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = japaCountText,
                        onValueChange = { japaCountText = it.filter(Char::isDigit).take(3) },
                        label = { Text("जप संख्या") },
                        supportingText = { Text("1 ते 108") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFFC83D),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFFC83D),
                            unfocusedLabelColor = Color.LightGray
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("🐢 जपाचा आवाज किती संथ ठेवायचा", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("सध्याचा वेग: ${String.format(java.util.Locale.US, "%.2f", speechRate)}  • कमी = अधिक हळू", color = Color.LightGray, fontSize = 12.sp)
                Slider(
                    value = speechRate,
                    onValueChange = { speechRate = it },
                    valueRange = 0.35f..0.90f,
                    steps = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("अधिक संथ", color = Color.LightGray, fontSize = 11.sp)
                    Text("जलद", color = Color.LightGray, fontSize = 11.sp)
                }

                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val count = japaCountText.toIntOrNull()?.coerceIn(1, 108) ?: 11
                    japaCountText = count.toString()
                    val cleanTimes = fixedTimesText.split(',').mapNotNull { token ->
                        val parts = token.trim().split(':')
                        if (parts.size != 2) return@mapNotNull null
                        val h = parts[0].toIntOrNull() ?: return@mapNotNull null
                        val m = parts[1].toIntOrNull() ?: return@mapNotNull null
                        if (h !in 0..23 || m !in 0..59) null else String.format(java.util.Locale.US, "%02d:%02d", h, m)
                    }.distinct().take(8).joinToString(",")
                    if (cleanTimes.isNotBlank()) fixedTimesText = cleanTimes
                    prefs.specialJapaCount = count
                    prefs.specialFixedTimes = cleanTimes.ifBlank { "05:00,08:00,11:00,14:00,17:00,20:00,23:00" }
                    prefs.speechRate = speechRate

                    // Saving fixed clock times intentionally rebuilds the daily slots
                    // from this save action; unrelated Alarm Settings changes preserve them.
                    val scheduler = AlarmScheduler(context.applicationContext)
                    scheduler.resetSpecialAaradhanaSchedule()
                    scheduler.scheduleAll()

                    // Immediately play the currently active Aaradhana so the user can
                    // verify count, pronunciation and speech speed without waiting for an alarm.
                    val previewMantras = mutableListOf(
                        nakInfo.mantra,
                        yogaInfo.mantra,
                        karanaInfo.mantra
                    ).filter { it.isNotBlank() }.toMutableList()
                    val previewAnnouncements = mutableListOf<String>()
                    val previewProfile = com.mahaesuvidha.chandrapanchangalarm.model.BirthProfileStore.load(context.applicationContext)
                    if (prefs.planetaryTaraAaradhana && previewProfile?.birthNakshatra?.isNotBlank() == true) {
                        runCatching {
                            com.mahaesuvidha.chandrapanchangalarm.model.PlanetaryTaraAaradhanaCalculator
                                .calculate(previewProfile.birthNakshatra)
                                .filter { row -> row.isWarning && prefs.isPlanetaryTaraEnabled(row.planet.name) }
                                .forEach { row ->
                                    previewAnnouncements.add("${row.planet.marathi} — ${row.tara} तारा")
                                    previewMantras.add(AaradhanaMaster.forPlanet(row.planet).mantra)
                                }
                        }
                    }
                    val cachedSteps = listOf(
                        Triple("nakshatra", moon.nakshatra.marathi, nakInfo.mantra),
                        Triple("yoga", panchang.yoga, yogaInfo.mantra),
                        Triple("karana", panchang.karana, karanaInfo.mantra)
                    ).map { (type, key, mantra) ->
                        AaradhanaVoiceSession.AudioStep(
                            audioFile = AaradhanaAudioCache.file(context, type, key).takeIf { it.exists() && it.length() > 2048 },
                            fallbackText = mantra
                        )
                    }
                    // Test/preview: local Nakshatra -> Yoga -> Karana audio first; all announcements last.
                    AaradhanaVoiceSession.speakCachedSequenceThenAnnouncement(
                        context.applicationContext, 399, cachedSteps, count, previewAnnouncements, null
                    )
                    savedPopup = true
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("💾 जतन करा + आत्ताच आराधना ऐका", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                Text("🎧 Local Audio TEST", color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("प्रत्येक downloaded/local audio स्वतंत्रपणे तपासा. शेवटी तारा घोषणा TTS ने ऐकवली जाईल.", color = Color.LightGray, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        val f = AaradhanaAudioCache.file(context, "nakshatra", moon.nakshatra.marathi)
                        if (f.exists() && f.length() > 2048) {
                            AaradhanaVoiceSession.stop()
                            AaradhanaTestAudioPlayer.play(context, f, onError = { AaradhanaVoiceSession.speakPreview(context, 501, listOf(nakInfo.mantra), 1) })
                        } else AaradhanaVoiceSession.speakPreview(context, 501, listOf(nakInfo.mantra), 1)
                    }, modifier = Modifier.weight(1f)) { Text("⭐ नक्षत्र", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = {
                        val f = AaradhanaAudioCache.file(context, "yoga", panchang.yoga)
                        if (f.exists() && f.length() > 2048) {
                            AaradhanaVoiceSession.stop()
                            AaradhanaTestAudioPlayer.play(context, f, onError = { AaradhanaVoiceSession.speakPreview(context, 502, listOf(yogaInfo.mantra), 1) })
                        } else AaradhanaVoiceSession.speakPreview(context, 502, listOf(yogaInfo.mantra), 1)
                    }, modifier = Modifier.weight(1f)) { Text("✨ योग", fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = {
                        val f = AaradhanaAudioCache.file(context, "karana", panchang.karana)
                        if (f.exists() && f.length() > 2048) {
                            AaradhanaVoiceSession.stop()
                            AaradhanaTestAudioPlayer.play(context, f, onError = { AaradhanaVoiceSession.speakPreview(context, 503, listOf(karanaInfo.mantra), 1) })
                        } else AaradhanaVoiceSession.speakPreview(context, 503, listOf(karanaInfo.mantra), 1)
                    }, modifier = Modifier.weight(1f)) { Text("🔱 करण", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(6.dp))
                Text("ON असल्यास: नक्षत्र मंत्र → योग मंत्र → करण मंत्र • प्रत्येक मंत्रासाठी निवडलेली जप संख्या. आराधना रोजच्या fixed clock वेळेला होईल.", color = Color.LightGray, fontSize = 12.sp)
                Text("ग्रह तारा आराधना सक्रिय असल्यास: ग्रह — विपत / प्रत्यारी / वध तारा घोषणा → त्या ग्रहाचा मंत्र → जप.", color = Color.LightGray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("📖 मार्गदर्शन स्वतंत्र Part मध्ये राहील.", color = Color.LightGray, fontSize = 13.sp)
    }

        }

    if (savedPopup) {
        AlertDialog(
            onDismissRequest = { savedPopup = false },
            title = { Text("✅ आराधना सेटिंग जतन झाली") },
            text = {
                Column {
                    Text("जप संख्या: $japaCountText")
                    Text("रोजच्या वेळा: $fixedTimesText")
                    Text("आवाजाचा वेग: ${String.format(java.util.Locale.US, "%.2f", speechRate)}")
                    Spacer(Modifier.height(8.dp))
                    Text("🕉️ सध्याची नक्षत्र + योग + करण आराधना आत्ताच सुरू केली आहे.")
                    Text("आवाज/स्पीड तपासण्यासाठी ऐका. पुढील alarm सेव्ह केलेल्या setting प्रमाणे schedule झाला आहे.")
                }
            },
            confirmButton = {
                TextButton(onClick = { savedPopup = false }) { Text("ठीक आहे") }
            }
        )
    }
}

@Composable
private fun MantraRow(label: String, value: String, deity: String, mantra: String) {
    Column(Modifier.padding(vertical = 5.dp)) {
        Text("$label: $value", color = Color.White, fontWeight = FontWeight.Bold)
        Text("🙏 अधिदेवता: $deity", color = Color(0xFFFFC83D), fontSize = 13.sp)
        Text("📿 $mantra", color = Color.White, fontSize = 13.sp)
    }
}
