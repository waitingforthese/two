package com.mahaesuvidha.chandrapanchangalarm

import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mahaesuvidha.chandrapanchangalarm.model.BirthProfile
import com.mahaesuvidha.chandrapanchangalarm.model.MoonState
import com.mahaesuvidha.chandrapanchangalarm.model.TodayPrediction
import com.mahaesuvidha.chandrapanchangalarm.model.TodayPredictionCalculator

@Composable
fun TodayPredictionScreen(
    profile: BirthProfile,
    moonState: MoonState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var birthCoordinates by remember(profile.birthPlace) { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(profile.birthPlace) {
        birthCoordinates = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) return@runCatching null
                val geocoder = Geocoder(context, java.util.Locale.getDefault())
                val address = geocoder.getFromLocationName(profile.birthPlace, 1)?.firstOrNull()
                address?.let { it.latitude to it.longitude }
            }.getOrNull()
        }
    }

    val prediction = remember(profile, moonState, birthCoordinates) {
        TodayPredictionCalculator.calculate(
            birthMoonRashi = profile.birthMoonRashi,
            birthNakshatra = profile.birthNakshatra,
            birthDate = profile.birthDate,
            birthTime = profile.birthTime,
            currentMoonRashi = moonState.rashi.marathi,
            currentNakshatra = moonState.nakshatra,
            birthCoordinates = birthCoordinates
        )
    }

    val bg = Color(0xFF07111F)
    val card = Color(0xFF10253A)
    val gold = Color(0xFFFFC83D)
    val white = Color(0xFFF5F7FA)
    val red = Color(0xFFE53935)
    val green = Color(0xFF66BB6A)

    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(bg).statusBarsPadding().navigationBarsPadding()) {
        Surface(Modifier.fillMaxWidth(), color = bg, shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← मागे", color = white) }
                Text("🔮 आजचे भाकीत", color = gold, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KundliReferenceButton(profile, textColor = white)
                    TransitAnalysisButton(profile, textColor = white)
                }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
        Spacer(Modifier.height(2.dp))

        PredictionCard(card, white) {
            Text("जन्म चंद्र राशी: ${prediction.birthMoonRashi}", color = Color(0xFF4DA3FF), fontWeight = FontWeight.Bold)
            Text("आजची चंद्र राशी: ${prediction.currentMoonRashi}", color = white)
            Text("आजचे नक्षत्र: ${prediction.currentNakshatra}", color = gold)
            Text(
                "ताराबल: ${prediction.taraName}  •  Score ${signed(prediction.taraScore)}",
                color = if (prediction.taraScore < 0) red else white,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (prediction.totalScore < 0) Color(0xFF32151A) else Color(0xFF132B1A)
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "${gradeEmoji(prediction.totalScore)} ${prediction.grade}",
                    color = if (prediction.totalScore < 0) red else green,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text("एकत्रित Score: ${signed(prediction.totalScore)}", color = white, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(prediction.headline, color = white, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        PredictionCard(card, white) {
            Text("📊 ग्रहांचे चंद्रापासून गोचर", color = gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("जन्मभाव = जन्मलग्नापासून  •  गोचर भाव = जन्म चंद्रराशीपासून", color = Color.LightGray, fontSize = 11.sp)
            if (birthCoordinates == null) {
                Text("जन्मठिकाण शोधत आहे...", color = Color.Gray, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))

            // Horizontal scroll keeps all columns readable on smaller phones.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ग्रह", color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
                Text("जन्मकुंडली\n(लग्नापासून)", color = gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(132.dp), textAlign = TextAlign.Center)
                Text("चंद्रापासून गोचर", color = gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(132.dp), textAlign = TextAlign.Center)
                Text("Score", color = gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
            }

            prediction.rows.forEach { row ->
                val scoreColor = when {
                    row.score <= -2 -> red
                    row.score >= 2 -> green
                    else -> white
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(88.dp)) {
                            Text("${planetEmoji(row.graha)} ${row.graha}", color = white, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Column(
                            modifier = Modifier.width(132.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                row.birthHouse?.let { "${it}वा भाव" } ?: "—",
                                color = white,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                row.birthRashi ?: "—",
                                color = gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                        Column(
                            modifier = Modifier.width(132.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${row.house}वा भाव",
                                color = white,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                row.rashi,
                                color = gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            signed(row.score),
                            color = scoreColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(48.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    Text(
                        row.effect,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
                HorizontalDivider(color = Color(0x334F6475))
            }
        }

        Spacer(Modifier.height(10.dp))

        PredictionCard(card, white) {
            Text("📝 आजचा निष्कर्ष", color = gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(prediction.summary, color = white, fontSize = 14.sp)
        }

        Spacer(Modifier.height(10.dp))

        PredictionCard(card, white) {
            Text("✅ आज काय करावे", color = green, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(prediction.doText, color = white, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(10.dp))
            Text("⚠️ आज काय टाळावे", color = red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(prediction.avoidText, color = white, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "हे पारंपरिक वैदिक ज्योतिषाच्या गोचर-नियमांवर आधारित मार्गदर्शन आहे.",
            color = Color.Gray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        }
    }
}

@Composable
private fun PredictionCard(
    color: Color,
    textColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

private fun gradeEmoji(score: Int): String = when {
    score >= 9 -> "🟢"
    score >= 4 -> "🟢"
    score >= -3 -> "🟡"
    else -> "🔴"
}

private fun planetEmoji(name: String): String = when (name) {
    "सूर्य" -> "☀️"
    "चंद्र" -> "🌙"
    "मंगळ" -> "♂️"
    "बुध" -> "☿️"
    "गुरु" -> "♃"
    "शुक्र" -> "♀️"
    "शनि" -> "♄"
    "राहू" -> "☊"
    "केतू" -> "☋"
    else -> "•"
}
