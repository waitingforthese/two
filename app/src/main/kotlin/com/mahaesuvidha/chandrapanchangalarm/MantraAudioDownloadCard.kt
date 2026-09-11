package com.mahaesuvidha.chandrapanchangalarm

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahaesuvidha.chandrapanchangalarm.model.MantraAudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MantraAudioDownloadCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var count by remember { mutableIntStateOf(MantraAudioManager.downloadedCount(context)) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var message by remember { mutableStateOf("") }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("🎧 मंत्र रेकॉर्डिंग डाउनलोड", color = Color(0xFFFFC83D), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "अॅपच्या APK मध्ये audio ठेवलेले नाही. एकदा डाउनलोड केल्यावर ते app-private storage मध्ये राहतील आणि पुढील वापरात पुन्हा download करावे लागणार नाही.",
                color = Color.LightGray, fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("डाउनलोड: $count / 9", color = Color.White, fontWeight = FontWeight.Bold)
            if (downloading) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("रेकॉर्डिंग डाउनलोड होत आहेत…", color = Color.LightGray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = !downloading,
                onClick = {
                    downloading = true
                    message = ""
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            MantraAudioManager.downloadAll(context.applicationContext) { done, total, failed ->
                                progress = done.toFloat() / total.toFloat()
                                count = MantraAudioManager.downloadedCount(context)
                                if (failed.isNotEmpty() && done == total) message = "डाउनलोड न झालेली नावे: ${failed.joinToString(", ")}"
                            }
                        }
                        downloading = false
                        count = MantraAudioManager.downloadedCount(context)
                        if (message.isBlank()) message = if (count == 9) "सर्व ९ रेकॉर्डिंग डाउनलोड झाली." else "डाउनलोड अपूर्ण आहे; पुन्हा प्रयत्न करा."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (count == 9) "🔄 पुन्हा तपासा / डाउनलोड" else "⬇️ सर्व ९ रेकॉर्डिंग डाउनलोड करा", fontWeight = FontWeight.Bold) }
            if (message.isNotBlank()) Text(message, color = Color(0xFFFFC83D), fontSize = 12.sp)
            Text(
                "टीप: सध्या डाउनलोड होणारे tracks हे पारंपरिक Navagraha chant pronunciation references आहेत. ते आपल्या निवडलेल्या short-japa मंत्राशी शब्दशः जुळत नसल्यास ते स्वयंचलित जपासाठी वापरले जाणार नाहीत.",
                color = Color.Gray, fontSize = 11.sp
            )
        }
    }
}
