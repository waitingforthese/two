package com.mahaesuvidha.chandrapanchangalarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahaesuvidha.chandrapanchangalarm.model.*

@Composable
fun UserManagerScreen(profile: BirthProfile, onBack: () -> Unit, onEdited: (BirthProfile, BirthProfile) -> Unit, onSelect: (BirthProfile) -> Unit) {
    val context = LocalContext.current
    var users by remember { mutableStateOf(BirthProfileStore.savedProfiles(context.applicationContext)) }
    var editing by remember { mutableStateOf<BirthProfile?>(null) }
    var deleting by remember { mutableStateOf<BirthProfile?>(null) }
    BackHandler(onBack = onBack)

    if (editing != null) {
        UserEditDialog(
            initial = editing!!,
            onDismiss = { editing = null },
            onSave = { updated ->
                val old = editing!!
                BirthProfileStore.update(context.applicationContext, old, updated)
                users = BirthProfileStore.savedProfiles(context.applicationContext)
                onEdited(old, updated)
                editing = null
            }
        )
    }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("User delete करायचा?") },
            text = { Text("“${deleting!!.name}” हा user कायमचा delete करायचा आहे का?") },
            confirmButton = { TextButton(onClick = { BirthProfileStore.remove(context.applicationContext, deleting!!); users = BirthProfileStore.savedProfiles(context.applicationContext); deleting = null }) { Text("Delete", color = Color(0xFFD32F2F)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF07111F)).statusBarsPadding().navigationBarsPadding()) {
        Surface(Modifier.fillMaxWidth(), color = Color(0xFF07111F), shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← मागे", color = Color.White) }
                Text("👥 User व्यवस्थापन", color = Color(0xFFFFC83D), fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KundliReferenceButton(profile, textColor = Color.White)
                    TransitAnalysisButton(profile, textColor = Color.White)
                }
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
        Spacer(Modifier.height(4.dp))
        if (users.isEmpty()) Text("एकही saved user नाही.", color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        users.forEach { user ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF10253A))) {
                Column(Modifier.padding(12.dp)) {
                    Text(user.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("जन्म: ${user.birthDate} • ${user.birthTime}", color = Color.LightGray, fontSize = 11.sp)
                    Text("ठिकाण: ${user.birthPlace}", color = Color.LightGray, fontSize = 11.sp)
                    Text("चंद्र राशी: ${user.birthMoonRashi} • नक्षत्र: ${user.birthNakshatra}", color = Color(0xFFFFC83D), fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onSelect(user) }, modifier = Modifier.weight(1f)) { Text("वापरा") }
                        OutlinedButton(onClick = { editing = user }, modifier = Modifier.weight(1f)) { Text("✏️ Edit") }
                        OutlinedButton(onClick = { deleting = user }, modifier = Modifier.weight(1f)) { Text("🗑️ Delete") }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun UserEditDialog(initial: BirthProfile, onDismiss: () -> Unit, onSave: (BirthProfile) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var date by remember(initial) { mutableStateOf(initial.birthDate) }
    var time by remember(initial) { mutableStateOf(initial.birthTime) }
    var place by remember(initial) { mutableStateOf(initial.birthPlace) }
    var gender by remember(initial) { mutableStateOf(initial.gender) }
    var error by remember { mutableStateOf<String?>(null) }
    val colors = OutlinedTextFieldDefaults.colors()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ User Edit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("नाव") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors)
                OutlinedTextField(date, { date = formatEditDate(it) }, label = { Text("जन्मतारीख DD/MM/YYYY") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = colors)
                OutlinedTextField(time, { time = formatEditTime(it) }, label = { Text("जन्मवेळ HH:MM") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), colors = colors)
                OutlinedTextField(place, { place = it }, label = { Text("जन्मठिकाण") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = colors)
                Spacer(Modifier.height(4.dp))
                Text("लिंग: $gender", color = Color.DarkGray, fontSize = 12.sp)
                if (error != null) Text(error!!, color = Color(0xFFD32F2F), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    require(name.trim().isNotEmpty()) { "नाव भरा" }
                    require(date.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) { "जन्मतारीख योग्य भरा" }
                    require(time.matches(Regex("\\d{2}:\\d{2}"))) { "जन्मवेळ योग्य भरा" }
                    require(place.trim().isNotEmpty()) { "जन्मठिकाण भरा" }
                    require(validateEditDate(date) == null) { validateEditDate(date)!! }
                    require(validateEditTime(time) == null) { validateEditTime(time)!! }
                    val rashi = GhatChakraCalculator.calculateBirthMoonRashi(date, time)
                    val nak = NakshatraGuidanceCalculator.calculateBirthNakshatra(date, time)
                    onSave(BirthProfile(name.trim(), date, gender, time, place.trim(), rashi, nak))
                } catch (t: Throwable) { error = t.message ?: "माहिती तपासा" }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatEditDate(raw: String): String = raw.filter(Char::isDigit).take(8).let { d -> buildString { d.forEachIndexed { i,c -> if(i==2 || i==4) append('/'); append(c) } } }
private fun formatEditTime(raw: String): String = raw.filter(Char::isDigit).take(4).let { d -> buildString { d.forEachIndexed { i,c -> if(i==2) append(':'); append(c) } } }
private fun validateEditDate(v: String): String? = runCatching { java.time.LocalDate.parse(v, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")); null }.getOrElse { "जन्मतारीख चुकीची आहे" }
private fun validateEditTime(v: String): String? = runCatching { val p=v.split(":"); require(p.size==2); require(p[0].toInt() in 0..23 && p[1].toInt() in 0..59); null }.getOrElse { "जन्मवेळ चुकीची आहे" }
