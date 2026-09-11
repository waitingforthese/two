package com.mahaesuvidha.chandrapanchangalarm

import android.location.Geocoder
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahaesuvidha.chandrapanchangalarm.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.floor

private val FrameworkBg = Color(0xFFF4F7FA)
private val FrameworkCard = Color(0xFFFFFFFF)
private val FrameworkText = Color(0xFF18212B)
private val FrameworkSecondary = Color(0xFF52606D)
private val FrameworkAccent = Color(0xFF9A6700)
private val FrameworkBorder = Color(0xFFD9E1E8)

private enum class FrameworkKind(val title: String, val icon: String, val subtitle: String) {
    MEDICAL("Medical Astrology", "🩺", "आरोग्याशी संबंधित पारंपरिक ज्योतिषीय संकेतांचा अभ्यास"),
    BUSINESS("Business Astrology", "💼", "व्यवसाय, पैसा, भागीदारी व लाभाचा अभ्यास"),
    EDUCATION("Educational Astrology", "📖", "शिक्षण, बुद्धी, एकाग्रता व उच्च शिक्षणाचा अभ्यास"),
    VASTU("Vastushastra", "🏠", "दिशा, वास्तु घटक व कुंडलीशी तुलनात्मक अभ्यास")
}

private data class StudySection(val title: String, val points: List<Pair<String, String>>)

private data class FrameworkDay(
    val date: LocalDate,
    val rashi: String,
    val house: Int,
    val nakshatra: String,
    val pada: Int,
    val nakshatraLord: String,
    val navamshaRashi: String,
    val navamshaLord: String,
    val rashiLord: String,
    val degrees: String,
    val aspects: String,
    val aspectHouses: List<Int>,
    val aspectPlanets: String,
    val topic: String,
    val change: String
)

private data class FrameworkPlanet(
    val graha: Graha,
    val birthHouse: Int,
    val birthRashi: String,
    val birthNakshatra: String,
    val birthPada: Int,
    val birthNakshatraLord: String,
    val birthNavamshaRashi: String,
    val birthNavamshaLord: String,
    val birthRashiLord: String,
    val birthDegrees: String,
    val transit: FrameworkDay,
    val subject: String,
    val sections: List<StudySection>,
    val reasoning: String,
    val prediction: String,
    val comparison: List<FrameworkDay>
)



@Composable
fun TransitAnalysisDialogHost(
    profile: BirthProfile,
    show: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var coords by remember(profile.birthPlace) { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(profile.birthPlace) {
        coords = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) null else Geocoder(context, java.util.Locale.getDefault())
                    .getFromLocationName(profile.birthPlace, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
            }.getOrNull()
        }
    }
    val data = remember(profile, coords) {
        if (coords == null) emptyList()
        else FrameworkCalculator.calculate(profile, coords!!.first, coords!!.second, FrameworkKind.MEDICAL)
    }
    if (show && data.isNotEmpty()) {
        TransitAnalysisPopup(data, onDismiss)
    }
}

@Composable
fun TransitAnalysisButton(
    profile: BirthProfile,
    textColor: Color = Color.White
) {
    val context = LocalContext.current
    var show by remember(profile) { mutableStateOf(false) }
    var coords by remember(profile.birthPlace) { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(profile.birthPlace) {
        coords = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) null else Geocoder(context, java.util.Locale.getDefault())
                    .getFromLocationName(profile.birthPlace, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
            }.getOrNull()
        }
    }
    val data = remember(profile, coords) {
        if (coords == null) emptyList()
        else FrameworkCalculator.calculate(profile, coords!!.first, coords!!.second, FrameworkKind.MEDICAL)
    }
    TextButton(
        onClick = { if (data.isNotEmpty()) show = true },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("📊 गोचर", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    if (show && data.isNotEmpty()) {
        TransitAnalysisPopup(data) { show = false }
    }
}

@Composable
private fun TransitAnalysisPopup(
    planets: List<FrameworkPlanet>,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<Graha?>(null) }
    val chosen = selected?.let { g -> planets.firstOrNull { it.graha == g } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F9FC),
        titleContentColor = FrameworkText,
        textContentColor = FrameworkText,
        title = {
            Text(
                if (chosen == null) "📊 संपूर्ण गोचर विश्लेषण" else "📊 ${chosen.graha.marathi} — संपूर्ण गोचर विश्लेषण",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState())
            ) {
                if (chosen == null) {
                    Text(
                        "आजच्या वास्तविक वेळेतील गोचरस्थितीवर आधारित खालील 9 ग्रहांचे स्वतंत्र सुमारे 1000 शब्दांचे विश्लेषण उपलब्ध आहे. ग्रह कुठे आहे, कोणत्या भावात आहे, का ते महत्त्वाचे आहे, राशी, राशीस्वामी, जन्मग्रह, दृष्टी, नक्षत्र, चरण, जीवनक्षेत्र, अनुकूल/प्रतिकूल संकेत, कालावधी आणि संयुक्त निष्कर्ष या सर्व स्तरांचा क्रमाने विचार केला जातो.",
                        fontSize = 15.sp, lineHeight = 23.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    planets.forEach { p ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selected = p.graha },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, FrameworkBorder),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(13.dp)) {
                                Text("${frameworkPlanetEmoji(p.graha.marathi)} ${p.graha.marathi}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "सध्या ${p.transit.rashi} • ${p.transit.house}वा भाव • ${p.transit.degrees}° • ${p.transit.nakshatra} • चरण ${p.transit.pada}",
                                    color = FrameworkAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                                Text("टॅप करा → पूर्ण गोचर विश्लेषण", color = FrameworkSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                            }
                        }
                    }
                } else {
                    Text(
                        "सध्याची स्थिती: ${chosen.transit.rashi} ${chosen.transit.degrees}° • ${chosen.transit.house}वा भाव (जन्म चंद्रराशीपासून) • ${chosen.transit.nakshatra}, चरण ${chosen.transit.pada}",
                        color = FrameworkAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp, modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        completeTransitAnalysis(chosen),
                        fontSize = 16.sp,
                        lineHeight = 27.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { selected = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("← 9 ग्रहांच्या यादीकडे")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद करा", fontSize = 16.sp, fontWeight = FontWeight.Bold) } }
    )
}

private fun completeTransitAnalysis(p: FrameworkPlanet): String {
    val g = p.graha
    val t = p.transit
    val sign = t.rashi
    val house = t.house
    val birthHouse = p.birthHouse
    val birthSign = p.birthRashi
    val common = """
${g.marathi} सध्या ${sign} राशीत ${t.degrees}° अंशावर, ${house}व्या भावातून गोचर करीत आहे. येथे ‘कुठे आहे?’ हा पहिला प्रश्न आहे; पण ज्योतिषीय विश्लेषण तेथे थांबत नाही. ‘का महत्त्वाचे आहे?’ यासाठी ग्रहाचे नैसर्गिक कारकत्व, गोचर भाव, गोचर राशी, राशीस्वामी, जन्मकुंडलीतील त्याची मूळ स्थिती, दृष्टी, नक्षत्र व नक्षत्रस्वामी, चरण व नवांश, तसेच जन्म चंद्रराशीपासूनचा संदर्भ एकत्र पाहावा लागतो. त्यामुळे हा मजकूर केवळ ग्रह-राशीचे सामान्य राशिभविष्य नसून सध्याच्या गोचराची reading-reference पद्धत आहे.

जन्मकुंडलीत ${g.marathi} ${birthHouse}व्या भावात ${birthSign} राशीत आहे. हा जन्मस्थितीचा स्थिर पाया आहे. गोचर हा त्या स्थिर पायावर सध्या कोणता भाग सक्रिय होतो हे दाखवतो. जन्मग्रहाचा अर्थ बदलत नाही; मात्र त्याच्या कारकत्वाशी संबंधित घटना, निर्णय, भावना किंवा परिस्थिती कोणत्या जीवनक्षेत्रात प्रकर्षाने जाणवू शकतात हे गोचरामुळे बदलते. म्हणून ‘जन्मात हा ग्रह असा आहे, त्यामुळे आज नक्की असेच होईल’ असा सरळ निष्कर्ष योग्य नाही. जन्मस्थिती + वर्तमान गोचर + इतर supporting combinations हा अधिक शिस्तबद्ध मार्ग आहे.

${planetNature(g)}

${planetHouseMeaning(g, house)}

${planetSignMeaning(g, sign, t.rashiLord)}

${planetBirthLink(g, birthHouse, birthSign)}

दृष्टीचा स्तर देखील महत्त्वाचा आहे. ${g.marathi} ची सध्याची दृष्टी ${t.aspects} अशी आहे. दृष्टी ज्या भावांवर पडते, त्या भावांचे विषय आणि त्या भावांत असलेले जन्मग्रह यांचा स्वतंत्र अभ्यास करावा. जर दृष्टीच्या भावात एखादा महत्त्वाचा जन्मग्रह असेल तर गोचराचा प्रभाव त्या ग्रहाच्या कारकत्वाशी जोडला जाऊ शकतो. मात्र दृष्टी दिसली म्हणून घटना निश्चित होते असे मानायचे नाही; ती एक supporting signal आहे. विशेषतः मंगळ, गुरु आणि शनि यांच्या विशेष दृष्टींचा विचार करताना संबंधित भावस्वामी, जन्मग्रहाचे बल आणि इतर गोचर यांची पुष्टी पाहणे आवश्यक आहे.

नक्षत्र हा पुढचा सूक्ष्म स्तर आहे. सध्याचे नक्षत्र ${t.nakshatra}, त्याचा स्वामी ${t.nakshatraLord} आणि चरण ${t.pada} आहे. ग्रह कोणत्या नक्षत्रात आहे याचा अर्थ ग्रहाचे फलित त्या नक्षत्रस्वामीच्या विषयांशी जोडले जाते. म्हणून नक्षत्रस्वामी जन्मकुंडलीत कुठे आहे, कोणत्या भावांचा स्वामी आहे, कोणत्या ग्रहांशी संबंध आहे आणि त्याची ताकद कशी आहे हे पाहणे आवश्यक आहे. नक्षत्र हा ‘अधिक सूक्ष्म कारण-साखळी’ देतो; तो स्वतंत्रपणे संपूर्ण फलित ठरवत नाही.

चरण ${t.pada} आणि नवांश ${t.navamshaRashi}, स्वामी ${t.navamshaLord} हा आणखी सूक्ष्म modifier आहे. चरणामुळे नक्षत्राच्या चार भागांपैकी नेमका कोणता भाग सक्रिय आहे हे समजते. नवांशाचा उपयोग ग्रहाच्या सूक्ष्म अभिव्यक्तीचा अभ्यास करण्यासाठी करता येतो. त्यामुळे मुख्य निष्कर्ष आधी रास आणि भावातून घ्यायचा आणि नंतर नक्षत्र-चरण-नवांशाने refine करायचा. उलट क्रमाने फक्त चरण पाहून मोठे भाकीत करणे टाळावे.

${lifeDomainAnalysis(g, house, sign)}

‘का?’ या प्रश्नाचे उत्तर देताना तीन वेगळे स्तर कायम लक्षात ठेवावेत. पहिला म्हणजे ग्रहाचे नैसर्गिक कारकत्व; दुसरा म्हणजे तो ज्या भावातून गोचर करतो त्या भावाचे विषय; आणि तिसरा म्हणजे गोचर राशी व तिचा स्वामी. उदाहरणार्थ, एखादा ग्रह धनकारक असला तरी तो 12व्या भावातून गोचर करीत असल्यास खर्च, परदेश, गुंतवणूक, विलगीकरण किंवा release यांसारखे विषय समोर येऊ शकतात. त्याचा अर्थ ‘पैसा वाईट’ असा सरळ नसतो. त्याचप्रमाणे कठीण मानला जाणारा ग्रह योग्य भाव, मजबूत राशीस्वामी आणि supportive जन्मयोगांमुळे शिस्त, संरचना किंवा दीर्घकालीन फायदा देऊ शकतो.

सध्याच्या संकेताची तीव्रता मोजताना अंशांची स्थिती, ग्रहाचा वेग, वक्री/मार्गी स्थिती, राशीस्वामीची ताकद, नक्षत्रस्वामीची जन्मस्थिती, संबंधित भावातील जन्मग्रह आणि इतर महत्त्वाचे गोचर हे पाहावे. जलद ग्रहांचे परिणाम दिवस/आठवड्यांच्या पातळीवर अधिक बदलू शकतात; गुरु व शनि यांसारख्या मंद ग्रहांचे प्रभाव अधिक दीर्घकाळ जाणवू शकतात. राहू-केतूंचा विचार करताना त्यांच्या अक्षाचा आणि संबंधित भावांचा संयुक्त अर्थ पाहावा. म्हणून ‘आज गोचर आहे म्हणजे आजच घटना’ हा निष्कर्ष टाळावा; activation window हा अधिक योग्य विचार आहे.

${supportMitigation(g)}

व्यवसाय/नोकरीच्या पातळीवर या गोचराचा अर्थ स्वतंत्रपणे तपासता येतो: अधिकार, कामाचा भार, client/public dealing, sales, cash flow, compliance, competition, partnership, technology, travel किंवा professional reputation यापैकी कोणता विषय सक्रिय आहे ते भाव आणि ग्रहाच्या कारकत्वातून ठरवावे. आर्थिक पातळीवर उत्पन्न आणि खर्च दोन्ही बाजू पाहाव्यात. घर/मालमत्ता/वाहनासाठी 4था भाव, 2रा-8वा-11वा-12वा भाव आणि संबंधित ग्रहांचा संबंध तपासावा. संबंधांसाठी 7वा, 2रा, 5वा, 8वा आणि शुक्र/गुरु/चंद्र यांच्या स्थितीचा संदर्भ महत्त्वाचा ठरू शकतो. शिक्षणासाठी 2रा, 4था, 5वा, 9वा आणि बुध/गुरु यांचा संदर्भ पाहावा.

आरोग्याच्या संदर्भात ग्रह-भाव संबंध हे पारंपरिक संकेत म्हणूनच वापरावेत. ‘शनि म्हणजे हाडे’ किंवा ‘मंगळ म्हणजे रक्त’ असे एकच सूत्र वापरून रोगनिदान करणे योग्य नाही. 1, 6, 8, 12 भाव, संबंधित ग्रह, राशी, नक्षत्र, दशा आणि इतर पुष्टीकारक योगांचा व्यापक संदर्भ आवश्यक आहे. प्रत्यक्ष शारीरिक लक्षणे असतील तर वैद्यकीय तपासणीला प्राधान्य द्यावे. या विश्लेषणाचा उद्देश health-symbolism समजणे आहे, diagnosis देणे नाही.

Classical study मध्ये बृहत् पराशर होरा शास्त्र, फलदीपिका, बृहत् जातक, सारावली आणि जातक पारिजात यांसारख्या ग्रंथांत ग्रह, भाव, राशी, दृष्टी आणि योग यांच्या फलिताचे विविध नियम आढळतात. येथे त्यांचा उपयोग ‘ग्रह + भाव + राशी + संबंध’ या पारंपरिक चौकटीसाठी केला आहे; प्रत्येक वाक्य हे एखाद्या ग्रंथातील शब्दशः उद्धरण समजू नये. अॅपमधील computational logic ही त्या पारंपरिक चौकटीची आधुनिक, नियमाधारित मांडणी आहे.

${observationAndConclusion(g, house, sign)}

अंतिम निष्कर्ष असा घ्यायचा की ${g.marathi} सध्या ${sign} राशीत ${house}व्या भावाचा विषय सक्रिय करीत आहे; पण त्या परिणामाची दिशा ${t.rashiLord} राशीस्वामी, जन्मस्थिती (${birthHouse}वा भाव, ${birthSign}), ${t.nakshatra} नक्षत्र व ${t.nakshatraLord} नक्षत्रस्वामी, चरण ${t.pada}, तसेच दृष्टी आणि इतर गोचर यांवर अवलंबून refine होते. म्हणून या ग्रहाकडे ‘चांगला’ किंवा ‘वाईट’ म्हणून न पाहता ‘कुठले जीवनक्षेत्र सक्रिय झाले, कोणता mechanism काम करतो, कोणती supporting किंवा mitigating स्थिती आहे आणि किती काळ ती राहू शकते?’ या चार प्रश्नांनी पाहणे हे अधिक अचूक आहे.
""".trimIndent()
    return common
}

private fun planetNature(g: Graha): String = when (g) {
    Graha.SURYA -> "☀️ सूर्य — आत्मबल, अधिकार, वडील/वरिष्ठ, शासन, प्रतिष्ठा, नेतृत्व, शरीरातील vitality आणि निर्णयक्षमता यांचे प्रमुख कारकत्व मानले जाते. गोचर सूर्य एखाद्या भावात आला की त्या भावाशी संबंधित visibility, authority आणि self-expression वाढण्याची शक्यता अभ्यासली जाते. कठीण संबंध असल्यास ego clash, वरिष्ठांशी तणाव किंवा ‘माझेच योग्य’ अशी भूमिका दिसू शकते. सकारात्मक स्थितीत नेतृत्व, clarity, recognition आणि निर्णयक्षमता उपयोगी पडते."
    Graha.CHANDRA -> "🌙 चंद्र — मन, भावना, सवय, स्मृती, माता, लोकसंपर्क, पोषण, झोप आणि मानसिक ग्रहणशीलतेचा प्रमुख संकेत आहे. चंद्र वेगवान असल्याने त्याचा गोचर अनुभव इतर ग्रहांच्या तुलनेत लवकर बदलतो. तो ज्या भावातून जातो त्या भावाचा विषय मनाच्या पातळीवर अधिक जाणवू शकतो. म्हणून चंद्रगोचराला घटना निश्चित करणारा एकमेव घटक न मानता daily emotional climate म्हणून वाचणे योग्य आहे."
    Graha.MANGAL -> "♂️ मंगळ — ऊर्जा, धाडस, कृती, स्पर्धा, जमीन, तांत्रिक काम, यंत्रणा, भाऊ/बंधू, शस्त्रक्रिया-सदृश पारंपरिक symbolism आणि संघर्षनिवारणाचा संकेत आहे. मंगळ गोचर कृतीची गरज वाढवू शकतो. योग्य वापरात तो काम वेगाने पुढे नेतो; चुकीच्या वापरात घाई, राग, वाद किंवा अनावश्यक risk वाढवू शकतो. म्हणून मंगळाच्या गोचरात ‘energy कुठे वापरायची?’ हा प्रश्न महत्त्वाचा आहे."
    Graha.BUDH -> "☿️ बुध — बुद्धी, गणित, लेखन, बोलणे, व्यापार, accounting, analysis, data, negotiation आणि learning यांचा प्रमुख संकेत आहे. बुधाचा गोचर ज्या भावातून होतो तिथे माहिती, निर्णय, communication आणि calculation सक्रिय होतात. मजबूत संदर्भात deal-making, learning, planning आणि problem-solving सुधारू शकतात; तणावात गैरसमज, over-analysis, चुकीची गणना किंवा contradictory communication होऊ शकते."
    Graha.GURU -> "♃ गुरु — ज्ञान, शिक्षक, सल्ला, विस्तार, धर्म/तत्त्वज्ञान, उच्च शिक्षण, संरक्षण, संपन्नता आणि व्यापक दृष्टीचा प्रमुख संकेत आहे. गुरु गोचर एखाद्या भावातील विषयाला विस्तार देतो असे पारंपरिक वाचन आहे. पण विस्तार म्हणजे नेहमीच लाभ नाही; कधी जबाबदारी, खर्च किंवा एखाद्या विषयाचा आकार वाढणे असेही होऊ शकते. म्हणून ‘काय वाढत आहे?’ हा प्रश्न गुरुच्या बाबतीत महत्त्वाचा आहे."
    Graha.SHUKRA -> "♀️ शुक्र — संबंध, विवाह, समन्वय, कला, सौंदर्य, सुविधा, वाहन, भौतिक सुख, मूल्यव्यवस्था आणि आर्थिक आकर्षण यांचा प्रमुख संकेत आहे. शुक्राचा गोचर सामाजिक संपर्क, comfort आणि relationship themes सक्रिय करू शकतो. चांगल्या संदर्भात harmony, negotiation, design, luxury किंवा relationship support वाढू शकतो; असंतुलित संदर्भात खर्च, अति-सुखलोलुपता किंवा अपेक्षांचा ताण दिसू शकतो."
    Graha.SHANI -> "♄ शनि — शिस्त, विलंब, कर्म, जबाबदारी, कामगार, सेवा, संरचना, नियम, दीर्घकालीन प्रयत्न, सीमारेषा आणि टिकाऊ परिणामांचा संकेत आहे. शनि ज्या भावातून जातो त्या भावातील गोष्टींकडे गांभीर्याने पाहण्याची वेळ येते. परिणाम उशिरा मिळू शकतो, पण योग्य पद्धतीने केलेला प्रयत्न अधिक टिकाऊ बनू शकतो. शनि ‘नकार’ पेक्षा ‘परीक्षा, संरचना आणि accountability’ या चौकटीत वाचणे अधिक अचूक आहे."
    Graha.RAHU -> "☊ राहू — तीव्र इच्छा, unconventional मार्ग, परदेश, technology, मोठे scale, publicity, भ्रम, shortcuts आणि नवीन प्रयोग यांचा छाया-संकेत आहे. राहू एखाद्या भावात असताना त्या भावाची इच्छा किंवा भूक वाढू शकते. योग्य विवेक असेल तर innovation आणि मोठी संधी मिळू शकते; विवेक कमी असेल तर overreach, भ्रम, चुकीची माहिती किंवा अनावश्यक risk वाढू शकतो."
    Graha.KETU -> "☋ केतू — विरक्ती, संशोधन, सूक्ष्म निरीक्षण, आध्यात्मिकता, detachment, अचानक कट, specialised knowledge आणि ‘याचा खरा अर्थ काय?’ असा प्रश्न विचारण्याची प्रवृत्ती यांचा संकेत आहे. केतू ज्या भावातून जातो तिथे काही वेळा बाह्य आकर्षण कमी होते आणि अंतर्मुखता वाढते. सकारात्मक स्थितीत deep research व concentration; असंतुलित स्थितीत confusion, disconnection किंवा काम अर्धवट सोडण्याची प्रवृत्ती दिसू शकते."
}

private fun planetHouseMeaning(g: Graha, h: Int): String {
    val base = when(h) {
        1 -> "1ला भाव स्वरूप, शरीर, व्यक्तिमत्त्व आणि सुरुवातीचा आहे."
        2 -> "2रा भाव धन, संचय, कुटुंब, वाणी आणि आहाराशी संबंधित आहे."
        3 -> "3रा भाव पराक्रम, प्रयत्न, communication, लेखन, marketing, भावंडे आणि छोटे प्रवास यांचा आहे."
        4 -> "4था भाव घर, माता, सुख, मानसिक शांतता, शिक्षणाची पायाभरणी, जमीन/मालमत्ता आणि वाहन यांचा आहे."
        5 -> "5वा भाव बुद्धी, शिक्षण, सर्जनशीलता, संतती, निर्णय आणि speculation यांचा आहे."
        6 -> "6वा भाव रोग, सेवा, कर्ज, स्पर्धा, शत्रू आणि routine work यांचा आहे."
        7 -> "7वा भाव विवाह, भागीदारी, clients, trade आणि public dealing यांचा आहे."
        8 -> "8वा भाव अचानक बदल, संशोधन, गुप्त विषय, संयुक्त संसाधने आणि transformation यांचा आहे."
        9 -> "9वा भाव भाग्य, उच्च ज्ञान, गुरु, तत्त्वज्ञान आणि दूरचा प्रवास यांचा आहे."
        10 -> "10वा भाव कर्म, profession, पद, authority, reputation आणि public role यांचा आहे."
        11 -> "11वा भाव लाभ, उत्पन्न, इच्छा पूर्ती, network, मित्र आणि मोठ्या संपर्कांचा आहे."
        else -> "12वा भाव खर्च, परदेश, विश्रांती, एकांत, release आणि अंतर्मुखतेचा आहे."
    }
    return "${g.marathi} या भावातून जात असल्याने या भावाच्या विषयांवर त्याचे नैसर्गिक कारकत्व लागू होते. $base त्यामुळे मुख्य प्रश्न असा: ‘${g.marathi} ज्या गोष्टींचा कारक आहे त्या गोष्टी या भावाच्या विषयांशी कशा जोडल्या जात आहेत?’ उदाहरणार्थ, ग्रह पैसा/संबंध/कर्म/बुद्धीचा कारक असेल तर त्याचा भावानुसार परिणाम वेगळा दिसतो."
}

private fun planetSignMeaning(g: Graha, sign: String, lord: String): String = "गोचर रास ${sign} आहे आणि तिचा स्वामी ${lord} आहे. रास ही ग्रहाला अभिव्यक्तीचे वातावरण देते. ${sign} राशीचा स्वभाव, तत्त्व आणि गती ग्रहाच्या नैसर्गिक कारकत्वाला दिशा देतात. त्यामुळे ${g.marathi} ला फक्त ‘${sign} मध्ये’ म्हणून न वाचता ‘${g.marathi} चे कारकत्व + ${sign} चा स्वभाव + ${lord} ची जन्मस्थिती’ या chain ने वाचावे. जर राशीस्वामी मजबूत असेल तर त्या क्षेत्राला support मिळू शकतो; कमजोर/afflicted असल्यास friction वाढू शकतो."

private fun planetBirthLink(g: Graha, house: Int, sign: String): String = "जन्मकुंडलीतील ${g.marathi} ${house}व्या भावात ${sign} राशीत आहे. त्यामुळे वर्तमान गोचर त्याच्या जन्मभूमिकेला activate करणारा trigger ठरू शकतो. जन्मग्रह कोणत्या भावांचा स्वामी आहे, त्याच्यावर कोणत्या ग्रहांची दृष्टी आहे, तो कोणत्या नक्षत्रात आहे आणि त्याची dignity काय आहे हे पुढील तपासणीचे मुद्दे आहेत. गोचर आणि जन्मस्थिती एकाच विषयावर repeated संकेत देत असतील तर interpretation अधिक मजबूत मानता येतो; परस्परविरोधी संकेत असतील तर मिश्र फलित आणि परिस्थितीनुसार परिणाम अपेक्षित असतो."

private fun lifeDomainAnalysis(g: Graha, h: Int, sign: String): String = when(g) {
    Graha.SURYA -> "जीवनक्षेत्र: सूर्याच्या गोचरात career/authority, वरिष्ठांशी संबंध, सरकारी किंवा formal कामे, प्रतिष्ठा, वडील/पितृतुल्य व्यक्ती, आत्मविश्वास, निर्णय आणि health-vitality यांचा संदर्भ घ्यावा. ${h}वा भाव हा या वेळी मुख्य stage आहे; ${sign} राशी त्या stage वर सूर्य कसा काम करतो ते ठरवते."
    Graha.CHANDRA -> "जीवनक्षेत्र: चंद्राच्या गोचरात मनःस्थिती, घरातील वातावरण, माता, लोकसंपर्क, झोप, दैनंदिन सवयी, भावनिक निर्णय आणि public response हे विषय प्रमुख असतात. ${h}वा भाव मनाला कोणता अनुभव देतो आणि ${sign} रास त्या अनुभवाची शैली कशी बनवते हे पाहावे."
    Graha.MANGAL -> "जीवनक्षेत्र: मंगळासाठी कामाची गती, technical activity, machinery, property, competition, initiative, physical effort, travel आणि conflict-management पाहावे. ${h}वा भाव ऊर्जा कुठे खर्च होते ते सांगतो; ${sign} रास ती ऊर्जा कोणत्या शैलीत व्यक्त होते ते सांगते."
    Graha.BUDH -> "जीवनक्षेत्र: बुधासाठी business, accounts, communication, sales, writing, contracts, study, data आणि negotiation तपासा. ${h}वा भाव माहिती/व्यवहाराचा मुख्य क्षेत्र देतो; ${sign} रास thinking आणि communication ची शैली बदलते."
    Graha.GURU -> "जीवनक्षेत्र: गुरुच्या गोचरात शिक्षण, सल्ला, expansion, finance, mentor support, children/knowledge, ethics आणि long-term planning तपासा. ${h}वा भाव कुठे विस्तार होतो ते सांगतो; ${sign} रास विस्ताराचा प्रकार ठरवते."
    Graha.SHUKRA -> "जीवनक्षेत्र: शुक्रासाठी relationship, marriage, comfort, vehicle, design, luxury, negotiation, finance आणि social harmony पाहावी. ${h}वा भाव संबंध/सुखाचा कोणता भाग सक्रिय करतो आणि ${sign} रास त्या गोष्टींची expression कशी बदलते ते तपासा."
    Graha.SHANI -> "जीवनक्षेत्र: शनि career responsibility, workload, systems, compliance, employees, debt/service, property structure आणि long-term commitments सक्रिय करू शकतो. ${h}वा भाव ‘कुठे accountability वाढते?’ हे सांगतो; ${sign} रास शिस्त कोणत्या शैलीत घ्यावी ते दाखवते."
    Graha.RAHU -> "जीवनक्षेत्र: राहूसाठी technology, foreign links, online/public reach, unconventional business, ambition, sudden scale, experimentation आणि misinformation-risk तपासा. ${h}वा भाव इच्छा कुठे वाढते ते सांगतो; ${sign} रास त्या इच्छेचा मार्ग दाखवते."
    Graha.KETU -> "जीवनक्षेत्र: केतूसाठी research, specialised skill, spiritual practice, detachment, hidden work, technical diagnosis आणि sudden disengagement तपासा. ${h}वा भाव कुठे बाह्य आसक्ती कमी होते ते सांगतो; ${sign} रास अंतर्मुखतेची दिशा देते."
}

private fun supportMitigation(g: Graha): String = when(g) {
    Graha.SURYA -> "Support/mitigation: गुरु किंवा शुभ ग्रहांची दृष्टी, मजबूत सूर्य, योग्य 10वा/9वा भाव आणि सकारात्मक authority relationships सूर्याच्या कठीण बाजू कमी करू शकतात."
    Graha.CHANDRA -> "Support/mitigation: मजबूत चंद्र, शुभ गुरु/शुक्र संबंध, स्थिर 4था भाव आणि चांगली routine चंद्राच्या भावनिक अस्थिरतेला संतुलित करू शकतात."
    Graha.MANGAL -> "Support/mitigation: शिस्तबद्ध physical work, मजबूत बुधाने दिलेला विचारपूर्वक निर्णय, गुरुची शुभ दृष्टी आणि सुरक्षित procedure मंगळाच्या घाईचा उपयोगी मार्ग बनवू शकतात."
    Graha.BUDH -> "Support/mitigation: लिखित communication, double-checking, मजबूत गुरुचा विवेक आणि स्पष्ट documentation बुधाच्या error-risk कमी करण्यास मदत करतात."
    Graha.GURU -> "Support/mitigation: मजबूत 2रा/5वा/9वा/11वा भाव, शुभ गुरु-संबंध आणि disciplined finance गुरुच्या expansion ला टिकाऊ दिशा देतात."
    Graha.SHUKRA -> "Support/mitigation: स्पष्ट boundaries, budget discipline, mutual respect आणि मजबूत 7वा/11वा/2रा भाव शुक्राच्या सुखकारक बाजूला अधिक स्थिर करतात."
    Graha.SHANI -> "Support/mitigation: नियमितता, documentation, नियमांचे पालन, कौशल्यवृद्धी आणि realistic timelines शनीच्या विलंबाला productive structure मध्ये बदलतात."
    Graha.RAHU -> "Support/mitigation: fact-checking, risk limits, transparent contracts, technical due diligence आणि गुरु/बुधाचा विवेक राहूच्या भ्रमकारी बाजूला नियंत्रण देतात."
    Graha.KETU -> "Support/mitigation: स्पष्ट objective, documentation, research discipline आणि भावनिक घाई न करता निर्णय घेणे केतूच्या detachment ला specialised focus मध्ये रूपांतरित करू शकते."
}

private fun observationAndConclusion(g: Graha, h: Int, sign: String): String = when(g) {
    Graha.SURYA -> "निरीक्षण: वरिष्ठांशी संवाद, authority decisions, सरकारी/औपचारिक कामे, self-confidence आणि energy level मध्ये काय बदलतो ते नोंदवा. निष्कर्षासाठी ego-clash पेक्षा responsibility आणि visibility दोन्ही मोजा."
    Graha.CHANDRA -> "निरीक्षण: mood, sleep, घरचे वातावरण, लोकांशी प्रतिक्रिया आणि निर्णयातील भावनिकता नोंदवा. चंद्र जलद असल्याने एका दिवसाच्या अनुभवावर मोठा life-event निष्कर्ष काढू नका."
    Graha.MANGAL -> "निरीक्षण: घाईचे निर्णय, machinery/vehicle handling, arguments, physical workload आणि property-related action याकडे लक्ष द्या. निष्कर्षात ऊर्जा productive झाली की conflict मध्ये गेली हे महत्त्वाचे."
    Graha.BUDH -> "निरीक्षण: calls, messages, accounts, documents, sales, negotiations आणि learning efficiency नोंदवा. चुकीची माहिती आली तर ती कुठल्या communication chain मध्ये आली ते तपासा."
    Graha.GURU -> "निरीक्षण: नवीन शिकण्याची संधी, mentor support, financial expansion, मोठे plans आणि जबाबदारी वाढणे नोंदवा. निष्कर्षात growth sustainable आहे का हे तपासा."
    Graha.SHUKRA -> "निरीक्षण: संबंध, ग्राहक/भागीदारांशी harmony, खर्च, vehicle/comfort purchases आणि creative work नोंदवा. सुख वाढले तरी budget आणि expectations नियंत्रित आहेत का ते पाहा."
    Graha.SHANI -> "निरीक्षण: pending work, deadlines, compliance, staff/workload, delays आणि long-term commitments नोंदवा. निष्कर्षात अडथळा कोणता आणि त्यातून तयार होणारी structure कोणती हे वेगळे करा."
    Graha.RAHU -> "निरीक्षण: अचानक opportunity, online/foreign connection, technology, मोठी ambition आणि information quality तपासा. निष्कर्षात innovation आणि illusion यांची सीमा स्पष्ट ठेवा."
    Graha.KETU -> "निरीक्षण: कोणत्या विषयापासून मन दूर होते, कोणत्या विषयात deep focus वाढतो आणि कुठे अचानक break होतो ते नोंदवा. निष्कर्षात detachment productive research आहे की avoidant withdrawal हे तपासा."
}

@Composable
private fun FrameworkStudyPopup(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color(0xFF111827),
        textContentColor = Color(0xFF1F2937),
        title = {
            Text(
                title,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    body,
                    fontSize = 18.sp,
                    lineHeight = 30.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("बंद करा", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun FrameworkScreen(profile: BirthProfile, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<FrameworkKind?>(null) }
    BackHandler { if (selected != null) selected = null else onBack() }
    if (selected == null) FrameworkHome(profile, onBack, { selected = it })
    else FrameworkDetail(profile, selected!!, onBack = { selected = null })
}

@Composable
fun KundliReferenceButton(
    profile: BirthProfile,
    textColor: Color = Color.White
) {
    val context = LocalContext.current
    var show by remember(profile) { mutableStateOf(false) }
    var showTransit by remember(profile) { mutableStateOf(false) }
    var coords by remember(profile.birthPlace) { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(profile.birthPlace) {
        coords = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) null else Geocoder(context, java.util.Locale.getDefault())
                    .getFromLocationName(profile.birthPlace, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
            }.getOrNull()
        }
    }
    val birth = remember(profile, coords) {
        if (coords == null) emptyMap() else BirthChartCalculator.calculate(
            profile.birthDate, profile.birthTime, coords!!.first, coords!!.second
        )
    }
    val transit = remember(profile, coords) {
        if (coords == null) emptyMap() else FrameworkCalculator.calculate(
            profile, coords!!.first, coords!!.second, FrameworkKind.MEDICAL
        ).associate { it.graha to it.transit }
    }
    TextButton(
        onClick = { if (birth.isNotEmpty()) show = true },
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("▣ कुंडली", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    if (show && birth.isNotEmpty()) {
        KundliReferencePopup(
            birth = birth,
            transit = transit,
            onDismiss = { show = false },
            onOpenTransit = { showTransit = true }
        )
    }
    if (showTransit) {
        TransitAnalysisDialogHost(
            profile = profile,
            show = true,
            onDismiss = { showTransit = false }
        )
    }
}

@Composable
private fun FrameworkHome(profile: BirthProfile, onBack: () -> Unit, onSelect: (FrameworkKind) -> Unit) {
    Column(Modifier.fillMaxSize().background(FrameworkBg).statusBarsPadding().navigationBarsPadding()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FrameworkBg,
            shadowElevation = 4.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("← मागे", color = FrameworkText, fontSize = 16.sp) }
                    Text("🧠 Framework", color = FrameworkAccent, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KundliReferenceButton(profile, textColor = FrameworkText)
                        TransitAnalysisButton(profile, textColor = FrameworkText)
                    }
                }
                Text("ग्रहस्थिती → प्रश्न → कारण → परिणाम → तुलना → अभ्यास", color = FrameworkSecondary,
                    fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), textAlign = TextAlign.Center)
            }
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
        FrameworkKind.entries.forEach { kind ->
            Card(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(kind) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FrameworkCard),
                border = BorderStroke(1.dp, FrameworkBorder)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(kind.icon, fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(kind.title, color = FrameworkText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(kind.subtitle, color = FrameworkSecondary, fontSize = 12.sp)
                    }
                    Text("›", color = Color(0xFFFFC83D), fontSize = 28.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        NoteCard("महत्त्वाचा अभ्यास नियम",
            "भाकीत शेवटी येईल. प्रत्येक आधीच्या घटकासाठी प्रश्न, उपलब्ध गणना आणि त्यातून निघणारा अर्थ आधी वाचता येईल.")
        }
    }
}


@Composable
private fun KundliReferencePopup(
    birth: Map<Graha, BirthChartCalculator.PlanetPosition>,
    transit: Map<Graha, FrameworkDay>,
    onDismiss: () -> Unit,
    onOpenTransit: () -> Unit
) {
    val ascRashiIndex = remember(birth) {
        birth[Graha.CHANDRA]?.let { moon ->
            ((moon.rashiIndex - (moon.house - 1)) + 12) % 12
        } ?: 0
    }
    val moonRashiIndex = birth[Graha.CHANDRA]?.rashiIndex ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF7F9FC),
        titleContentColor = FrameworkText,
        textContentColor = FrameworkText,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "▣ माझी जन्मकुंडली — Reading Reference",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "जन्मग्रह सामान्य अक्षरात • गोचर ग्रह त्यांच्या ग्रह-रंगाच्या हलक्या चौकोनी बॉक्समध्ये • सर्व अंश (°) स्पष्ट दाखवले आहेत.",
                    color = FrameworkSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = onOpenTransit,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text("📊 गोचर विश्लेषण उघडा", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                KundliChartCard(
                    title = "जन्मलग्न कुंडली (D-1)",
                    subtitle = "भाव लग्नापासून • लग्न : ${Rashi.entries[ascRashiIndex].marathi}",
                    referenceRashiIndex = ascRashiIndex,
                    birth = birth,
                    transit = transit,
                    useMoonAsFirstHouse = false
                )
                Spacer(Modifier.height(12.dp))
                KundliChartCard(
                    title = "चंद्र कुंडली (राशी कुंडली)",
                    subtitle = "भाव चंद्रराशीपासून • चंद्र : ${Rashi.entries[moonRashiIndex].marathi}",
                    referenceRashiIndex = moonRashiIndex,
                    birth = birth,
                    transit = transit,
                    useMoonAsFirstHouse = true
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "साधा ग्रह = जन्मस्थिती | रंगीत बॉक्स = आजचा गोचर | बॉक्समध्ये ग्रहाचे नाव + अचूक अंश. ग्रहाचा रंग दोन्ही कुंडल्यांत समान राहील.",
                    color = FrameworkSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद करा") } }
    )
}

@Composable
private fun KundliChartCard(
    title: String,
    subtitle: String,
    referenceRashiIndex: Int,
    birth: Map<Graha, BirthChartCalculator.PlanetPosition>,
    transit: Map<Graha, FrameworkDay>,
    useMoonAsFirstHouse: Boolean
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (useMoonAsFirstHouse) Color(0xFFF2F8FF) else Color(0xFFFFFAF0)),
        border = BorderStroke(1.dp, if (useMoonAsFirstHouse) Color(0xFFB9D6F5) else Color(0xFFE6C98A)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = if (useMoonAsFirstHouse) Color(0xFF145B9A) else Color(0xFF8D1D1D), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = FrameworkSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
            NorthIndianKundliCanvas(
                referenceRashiIndex = referenceRashiIndex,
                birth = birth,
                transit = transit,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
        }
    }
}

private fun degreeText(v: Double): String = String.format(java.util.Locale.US, "%.2f", v % 30.0)

private fun chartPlanetShort(graha: Graha): String = when (graha) {
    Graha.SURYA -> "सूर्य"
    Graha.CHANDRA -> "चंद्र"
    Graha.MANGAL -> "मंगळ"
    Graha.BUDH -> "बुध"
    Graha.GURU -> "गुरु"
    Graha.SHUKRA -> "शुक्र"
    Graha.SHANI -> "शनि"
    Graha.RAHU -> "राहू"
    Graha.KETU -> "केतू"
}

private fun transitPlanetColor(graha: Graha): Color = when (graha) {
    Graha.SURYA -> Color(0xFFFFD6A5)
    Graha.CHANDRA -> Color(0xFFFFF0A8)
    Graha.MANGAL -> Color(0xFFFFC7C2)
    Graha.BUDH -> Color(0xFFC8F3C5)
    Graha.GURU -> Color(0xFFFFE0A3)
    Graha.SHUKRA -> Color(0xFFF6C6E8)
    Graha.SHANI -> Color(0xFFD8C7F6)
    Graha.RAHU -> Color(0xFFC8E1FF)
    Graha.KETU -> Color(0xFFD5F1C8)
}

private fun transitPlanetTextColor(graha: Graha): Color = when (graha) {
    Graha.SURYA -> Color(0xFF9A4B00)
    Graha.CHANDRA -> Color(0xFF756300)
    Graha.MANGAL -> Color(0xFF9B2C2C)
    Graha.BUDH -> Color(0xFF216A2A)
    Graha.GURU -> Color(0xFF8A5A00)
    Graha.SHUKRA -> Color(0xFF8A236C)
    Graha.SHANI -> Color(0xFF55339A)
    Graha.RAHU -> Color(0xFF245A9B)
    Graha.KETU -> Color(0xFF397A27)
}

@Composable
private fun NorthIndianKundliCanvas(
    referenceRashiIndex: Int,
    birth: Map<Graha, BirthChartCalculator.PlanetPosition>,
    transit: Map<Graha, FrameworkDay>,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(0xFF202020)
    val signColor = Color(0xFF2947A3)
    val ascColor = Color(0xFFB71C1C)

    Canvas(modifier = modifier.padding(2.dp)) {
        val w = size.width
        val h = size.height
        val minSide = minOf(w, h)
        val cx = w / 2f
        val cy = h / 2f
        val inset = minSide * 0.035f
        val l = inset
        val r = w - inset
        val t = inset
        val b = h - inset
        val midX = cx
        val midY = cy

        drawLine(lineColor, androidx.compose.ui.geometry.Offset(l, t), androidx.compose.ui.geometry.Offset(r, t), strokeWidth = 2.2f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(r, t), androidx.compose.ui.geometry.Offset(r, b), strokeWidth = 2.2f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(r, b), androidx.compose.ui.geometry.Offset(l, b), strokeWidth = 2.2f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(l, b), androidx.compose.ui.geometry.Offset(l, t), strokeWidth = 2.2f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(l, t), androidx.compose.ui.geometry.Offset(r, b), strokeWidth = 1.8f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(r, t), androidx.compose.ui.geometry.Offset(l, b), strokeWidth = 1.8f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(midX, t), androidx.compose.ui.geometry.Offset(r, midY), strokeWidth = 1.8f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(r, midY), androidx.compose.ui.geometry.Offset(midX, b), strokeWidth = 1.8f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(midX, b), androidx.compose.ui.geometry.Offset(l, midY), strokeWidth = 1.8f)
        drawLine(lineColor, androidx.compose.ui.geometry.Offset(l, midY), androidx.compose.ui.geometry.Offset(midX, t), strokeWidth = 1.8f)

        val centers = arrayOf(
            androidx.compose.ui.geometry.Offset(cx, h * 0.285f),
            androidx.compose.ui.geometry.Offset(w * 0.285f, h * 0.14f),
            androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.285f),
            androidx.compose.ui.geometry.Offset(w * 0.285f, cy),
            androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.715f),
            androidx.compose.ui.geometry.Offset(w * 0.285f, h * 0.86f),
            androidx.compose.ui.geometry.Offset(cx, h * 0.715f),
            androidx.compose.ui.geometry.Offset(w * 0.715f, h * 0.86f),
            androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.715f),
            androidx.compose.ui.geometry.Offset(w * 0.715f, cy),
            androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.285f),
            androidx.compose.ui.geometry.Offset(w * 0.715f, h * 0.14f)
        )

        val nativeCanvas = drawContext.canvas.nativeCanvas
        val signPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = signColor.toArgb()
            textSize = minSide * 0.063f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val birthPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = minSide * 0.046f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val ascPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = ascColor.toArgb()
            textSize = minSide * 0.040f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val boxPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val boxTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = minSide * 0.037f
        }

        val birthByRashi = birth.entries.groupBy { it.value.rashiIndex }
        val transitByRashi = transit.entries.groupBy { Rashi.entries.indexOfFirst { r -> r.marathi == it.value.rashi }.let { idx -> if (idx >= 0) idx else 0 } }

        centers.forEachIndexed { index, center ->
            val house = index + 1
            val signIndex = (referenceRashiIndex + house - 1) % 12
            nativeCanvas.drawText((signIndex + 1).toString(), center.x, center.y - minSide * 0.095f, signPaint)

            val natalPlanets = birthByRashi[signIndex].orEmpty().map { it.key to it.value }
            val transitPlanets = transitByRashi[signIndex].orEmpty().map { it.key to it.value }

            var y = center.y - minSide * 0.035f
            natalPlanets.forEach { (g, pos) ->
                nativeCanvas.drawText("${chartPlanetShort(g)} ${degreeText(pos.longitude)}", center.x, y, birthPaint)
                y += minSide * 0.043f
            }

            if (transitPlanets.isNotEmpty()) {
                y += minSide * 0.006f
                transitPlanets.forEach { (g, day) ->
                    val label = "गो ${chartPlanetShort(g)} ${day.degrees}°"
                    // Keep the pastel background only as wide as the actual transit label.
                    // This makes the transit overlay visually distinct without wasting chart space.
                    val horizontalPad = minSide * 0.016f
                    val boxWidth = boxTextPaint.measureText(label) + horizontalPad * 2f
                    val boxHeight = minSide * 0.068f
                    val left = center.x - boxWidth / 2f
                    val top = y - boxHeight * 0.82f
                    val right = center.x + boxWidth / 2f
                    val bottom = y + boxHeight * 0.18f
                    boxPaint.color = transitPlanetColor(g).toArgb()
                    nativeCanvas.drawRoundRect(left, top, right, bottom, boxHeight * 0.22f, boxHeight * 0.22f, boxPaint)
                    boxTextPaint.color = transitPlanetTextColor(g).toArgb()
                    nativeCanvas.drawText(label, center.x, y, boxTextPaint)
                    y += minSide * 0.074f
                }
            }
            if (house == 1) {
                nativeCanvas.drawText("ल", center.x, center.y + minSide * 0.11f, ascPaint)
            }
        }
    }
}

@Composable
private fun FrameworkDetail(profile: BirthProfile, kind: FrameworkKind, onBack: () -> Unit) {
    val context = LocalContext.current
    var showKundliReference by remember { mutableStateOf(false) }
    var showTransitAnalysis by remember { mutableStateOf(false) }
    var coords by remember(profile.birthPlace) { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(profile.birthPlace) {
        coords = withContext(Dispatchers.IO) {
            runCatching {
                if (!Geocoder.isPresent()) null else Geocoder(context, java.util.Locale.getDefault())
                    .getFromLocationName(profile.birthPlace, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
            }.getOrNull()
        }
    }
    val data = remember(profile, coords, kind) {
        if (coords == null) emptyList() else FrameworkCalculator.calculate(profile, coords!!.first, coords!!.second, kind)
    }
    val birthChart = remember(profile, coords) {
        if (coords == null) emptyMap() else BirthChartCalculator.calculate(profile.birthDate, profile.birthTime, coords!!.first, coords!!.second)
    }
    val transitReference = remember(data) {
        data.associate { it.graha to it.transit }
    }
    if (showKundliReference && birthChart.isNotEmpty()) {
        KundliReferencePopup(
            birth = birthChart,
            transit = transitReference,
            onDismiss = { showKundliReference = false },
            onOpenTransit = { showTransitAnalysis = true }
        )
    }
    TransitAnalysisDialogHost(
        profile = profile,
        show = showTransitAnalysis,
        onDismiss = { showTransitAnalysis = false }
    )
    Column(Modifier.fillMaxSize().background(FrameworkBg).statusBarsPadding().navigationBarsPadding()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FrameworkBg,
            shadowElevation = 4.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("← मागे", color = FrameworkText, fontSize = 16.sp) }
                    Text("${kind.icon} ${kind.title}", color = FrameworkAccent, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showKundliReference = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("⚙ कुंडली", color = FrameworkText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        TransitAnalysisButton(profile, textColor = FrameworkText)
                    }
                }
                Text("जन्मकुंडलीतील भाव = जन्मलग्नापासून  •  गोचर भाव = जन्म चंद्रराशीपासून",
                    color = FrameworkSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(12.dp)) {
            Spacer(Modifier.height(2.dp))
            NoteCard("अभ्यासाची पद्धत",
                "प्रत्येक ग्रहासाठी आधी प्रश्न वाचायचे, नंतर त्या प्रश्नाचे सध्याचे उत्तर पाहायचे. शेवटी सर्व संकेत जोडून संयुक्त Logic आणि भाकीत वाचायचे.")
            if (coords == null) Text("जन्मठिकाणाचे coordinates शोधत आहे...", color = Color.Gray, modifier = Modifier.padding(12.dp))
            if (kind == FrameworkKind.VASTU) VastuInfo()
            data.forEach { planet -> PlanetStudyCard(planet, kind) }
            if (data.isEmpty() && coords != null) Text("विश्लेषणासाठी जन्ममाहिती तपासा.", color = FrameworkSecondary, modifier = Modifier.padding(16.dp))
        }
    }
}

private fun frameworkPlanetEmoji(name: String): String = when (name) {
    "सूर्य" -> "☀️"; "चंद्र" -> "🌙"; "मंगळ" -> "♂️"; "बुध" -> "☿️"; "गुरु" -> "♃"; "शुक्र" -> "♀️"; "शनि" -> "♄"; "राहू" -> "☊"; "केतू" -> "☋"; else -> "•"
}

@Composable
private fun PlanetStudyCard(p: FrameworkPlanet, kind: FrameworkKind) {
    var open by remember { mutableStateOf(false) }
    var comparison by remember { mutableStateOf(false) }
    val gold = FrameworkAccent
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FrameworkCard),
        border = BorderStroke(1.dp, FrameworkBorder)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
                Text("${frameworkPlanetEmoji(p.graha.marathi)} ${p.graha.marathi}", color = FrameworkText, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (open) "⌃" else "⌄", color = gold, fontSize = 24.sp)
            }
            Text("आज: ${p.transit.house}वा भाव • ${p.transit.rashi} • ${p.transit.nakshatra} • चरण ${p.transit.pada}", color = gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            ConceptPreview(p)
            if (open) {
                Spacer(Modifier.height(8.dp))
                StudySectionCard("① गोचर ग्रह कोणता? — ग्रहाचा पूर्ण विचार", p.sections[0])
                StudySectionCard("② कोणत्या भावातून गोचर करतो? — भावाचा सूक्ष्म विचार", p.sections[1])
                StudySectionCard("③ गोचर राशी — राशीचा सूक्ष्म विचार", p.sections[2])
                StudySectionCard("④ जन्मकुंडलीतील ग्रह — जन्मभूमिका", p.sections[3])
                StudySectionCard("⑤ दृष्टी — दृष्टीतील भाव व ग्रह", p.sections[4])
                StudySectionCard("⑥ नक्षत्र — नक्षत्र स्वामीपर्यंत विचार", p.sections[5])
                StudySectionCard("⑦ चरण — नवांशासह सूक्ष्म विचार", p.sections[6])
                StudySectionCard("⑧ जोडणी — जन्म + गोचर + इतर संकेत", p.sections[7])
                if (p.sections.size > 8) {
                    StudySectionCard("⑨ ${p.sections[8].title} — क्षेत्रीय Concept", p.sections[8])
                }
                Spacer(Modifier.height(6.dp))
                var logicPopup by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { logicPopup = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("🧠 संयुक्त Logic + भाकीत कसे तयार झाले?")
                }
                if (logicPopup) {
                    FrameworkStudyPopup(
                        "🧠 ${p.graha.marathi} — संयुक्त Logic आणि निष्कर्ष",
                        "${p.reasoning}\n\n🔮 अंतिम भाकीत\n${p.prediction}",
                    ) { logicPopup = false }
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { comparison = !comparison }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (comparison) "⌃ Comparison बंद करा" else "📊 Comparison — मागील 2 दिवस + आज + पुढील 2 दिवस")
                }
                if (comparison) ComparisonTable(p.comparison, p.birthHouse, p.birthRashi)
            }
        }
    }
}

@Composable
private fun ConceptPreview(p: FrameworkPlanet) {
    val section = p.sections.getOrNull(8) ?: return
    val first = section.points.getOrNull(0)?.second.orEmpty()
    val second = section.points.getOrNull(1)?.second.orEmpty()
    Card(
        Modifier.fillMaxWidth().padding(top = 9.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F7F3)),
        border = BorderStroke(1.dp, Color(0xFFD5E5DA)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(11.dp)) {
            Text("📌 ${section.title}", color = Color(0xFF23613A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (first.isNotBlank()) Text("• मुख्य विषय: $first", color = FrameworkText, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 4.dp))
            if (second.isNotBlank()) Text("• सध्याचा संदर्भ: $second", color = FrameworkText, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 3.dp))
            Text("टॅप करून ${section.title} चा संपूर्ण अभ्यास वाचा", color = Color(0xFF23613A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
        }
    }
}

@Composable
private fun StudySectionCard(title: String, section: StudySection) {
    var popup by remember { mutableStateOf(false) }
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { popup = true },
        colors = CardDefaults.cardColors(containerColor = FrameworkCard),
        border = BorderStroke(1.dp, FrameworkBorder)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = FrameworkAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "टॅप करा → मोठ्या अक्षरात संपूर्ण अभ्यास वाचा",
                    color = FrameworkSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Text("›", color = FrameworkAccent, fontSize = 25.sp)
        }
    }
    if (popup) {
        val body = section.points.joinToString("\n\n") { (question, answer) ->
            "❓ $question\n→ $answer"
        }
        FrameworkStudyPopup(title, body) { popup = false }
    }
}

@Composable
private fun ComparisonTable(rows: List<FrameworkDay>, birthHouse: Int, birthRashi: String) {
    var selectedDay by remember { mutableStateOf<FrameworkDay?>(null) }

    Spacer(Modifier.height(8.dp))

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
        border = BorderStroke(1.dp, Color(0xFFD7DEE8)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "📊 5-दिवसीय सूक्ष्म तुलना",
                color = Color(0xFF172033),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "जन्मभाव: लग्नापासून  •  गोचरभाव: चंद्रापासून",
                color = Color(0xFF526071),
                fontSize = 12.sp
            )
            Text(
                "जन्म: ${birthHouse}वा भाव — $birthRashi",
                color = Color(0xFF526071),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    rows.forEachIndexed { i, r ->
        val label = when (i) {
            0 -> "मागील 2 दिवस"
            1 -> "मागील 1 दिवस"
            2 -> "आज"
            3 -> "पुढील 1 दिवस"
            else -> "पुढील 2 दिवस"
        }
        val isToday = i == 2

        Card(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { selectedDay = r },
            colors = CardDefaults.cardColors(
                containerColor = if (isToday) Color(0xFFFFF8E6) else Color(0xFFF7F9FC)
            ),
            border = BorderStroke(
                1.dp,
                if (isToday) Color(0xFFE7B52B) else Color(0xFFD7DEE8)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            label,
                            color = if (isToday) Color(0xFF9A6900) else Color(0xFF526071),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            r.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            color = Color(0xFF172033),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "सविस्तर पहा  ›",
                        color = Color(0xFF1769AA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("गोचर रास", color = Color(0xFF6B7280), fontSize = 11.sp)
                        Text(r.rashi, color = Color(0xFF172033), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("गोचर भाव", color = Color(0xFF6B7280), fontSize = 11.sp)
                        Text("${r.house}वा भाव", color = Color(0xFF172033), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1.2f)) {
                        Text("नक्षत्र / चरण", color = Color(0xFF6B7280), fontSize = 11.sp)
                        Text("${r.nakshatra} / ${r.pada}", color = Color(0xFF172033), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(9.dp))

                Surface(
                    color = if (isToday) Color(0xFFFFE9A8) else Color(0xFFEAF1F8),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        "बदल: ${r.change}",
                        color = Color(0xFF374151),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }

    selectedDay?.let { r ->
        val detail =
            "📅 ${r.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}\n\n" +
            "🌕 गोचर रास: ${r.rashi}\n" +
            "🏠 गोचर भाव: ${r.house}वा भाव (चंद्रापासून)\n" +
            "° अंश: ${r.degrees}\n\n" +
            "⭐ नक्षत्र: ${r.nakshatra}\n" +
            "🔹 चरण: ${r.pada}\n" +
            "⭐ नक्षत्र स्वामी: ${r.nakshatraLord}\n\n" +
            "♈ नवांश: ${r.navamshaRashi}\n" +
            "स्वामी: ${r.navamshaLord}\n" +
            "राशी स्वामी: ${r.rashiLord}\n\n" +
            "👁️ दृष्टी: ${r.aspects}\n" +
            "दृष्टीतील जन्मग्रह: ${r.aspectPlanets}\n\n" +
            "🎯 सक्रिय विषय: ${r.topic}\n\n" +
            "🔄 बदल / अभ्यास: ${r.change}"

        FrameworkStudyPopup(
            "📊 ${r.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))} — सूक्ष्म अभ्यास",
            detail
        ) { selectedDay = null }
    }
}

@Composable
private fun VastuInfo() {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = FrameworkCard), border = BorderStroke(1.dp, FrameworkBorder)) {
        Column(Modifier.padding(14.dp)) {
            Text("🏠 वास्तु अभ्यास — प्रश्नावली", color = FrameworkAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            listOf(
                "कोणती दिशा? → उत्तर, दक्षिण, पूर्व, पश्चिम, ईशान्य, आग्नेय, नैऋत्य, वायव्य.",
                "त्या दिशेचा पारंपरिक वास्तु अर्थ काय?",
                "पंचमहाभूताशी संबंध काय?",
                "वास्तुपुरुष मंडलातील स्थान काय?",
                "मुख्य प्रवेश, kitchen, bedroom, पूजा, office, धनस्थान कुठे आहे?",
                "ब्रह्मस्थान मोकळे/संतुलित आहे का?",
                "कुंडलीतील ग्रहसंकेताशी वास्तु घटकाचा तुलनात्मक संबंध काय?",
                "स्थिर वास्तु नियम आणि दैनिक गोचर वेगळे कसे ठेवायचे?"
            ).forEach { Text("❓ $it", color = FrameworkText, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 5.dp)) }
        }
    }
}

@Composable
private fun NoteCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(11.dp)) {
            Text(title, color = Color(0xFFFFC83D), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text, color = FrameworkSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private object FrameworkCalculator {

    fun calculate(profile: BirthProfile, lat: Double, lon: Double, kind: FrameworkKind): List<FrameworkPlanet> {
        val birth = BirthChartCalculator.calculate(profile.birthDate, profile.birthTime, lat, lon)
        val moonIndex = Rashi.entries.indexOfFirst { it.marathi == profile.birthMoonRashi }.let { if (it >= 0) it else 0 }
        val nowMillis = System.currentTimeMillis()
        val today = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.of("Asia/Kolkata")).toLocalDate()
        val allBodies = Graha.entries
        val birthByHouse = birth.entries.groupBy { it.value.house }.mapValues { it.value.map { e -> e.key.marathi } }
        val daySnapshots = (-3..2).associateWith { offset ->
            val atMillis = LiveTransitCalculator.sameLocalClockMillis(nowMillis, offset.toLong())
            atMillis to LiveTransitCalculator.positionsAt(atMillis)
        }
        return allBodies.map { g ->
            val bp = birth[g] ?: BirthChartCalculator.PlanetPosition(0, 1)
            val birthRashi = Rashi.entries[bp.rashiIndex]
            val birthNak = Nakshatra.entries[bp.nakshatraIndex]
            val birthInfo = JyotishMaster.getInfo(birthRashi, birthNak, bp.pada)
            val days = (-2..2).map { offset ->
                val snapshot = daySnapshots[offset]!!
                day(moonIndex, g, today.plusDays(offset.toLong()), offset, kind, birthByHouse,
                    snapshot.second, daySnapshots[offset - 1]?.second)
            }
            val now = days[2]
            val subject = subjects[g] ?: "ग्रहाशी संबंधित पारंपरिक विषय"
            val sections = buildSections(g, bp, birthRashi, birthNak, birthInfo, now, kind, birthByHouse)
            FrameworkPlanet(
                g, bp.house, birthRashi.marathi, birthNak.marathi, bp.pada, birthInfo.nakshatraLord,
                birthInfo.navamshaRashi, birthInfo.navamshaLord, birthInfo.rashiLord, degreeText(bp.longitude),
                now, subject, sections,
                buildReasoning(g, bp, birthRashi, birthNak, birthInfo, now, kind, birthByHouse),
                buildPrediction(g, now, bp, kind), days
            )
        }
    }

    private fun day(moonIndex: Int, g: Graha, date: LocalDate, offset: Int, kind: FrameworkKind,
                    birthByHouse: Map<Int, List<String>>, transitPositions: Map<Graha, Double>,
                    previousTransitPositions: Map<Graha, Double>?): FrameworkDay {
        val rawLongitude = transitPositions[g] ?: 0.0
        val idx = rashiIndex(rawLongitude)
        val house = (idx - moonIndex + 12) % 12 + 1
        val nakIndex = (rawLongitude / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val nak = Nakshatra.entries[nakIndex]
        val pada = (((rawLongitude % (360.0 / 27.0)) / (360.0 / 108.0)).toInt() + 1).coerceIn(1, 4)
        val info = JyotishMaster.getInfo(Rashi.entries[idx], nak, pada)
        val aspectHouses = aspectHouses(g, house)
        val aspectText = if (aspectHouses.isEmpty()) "—" else aspectHouses.joinToString(", ") { "${it}वा भाव" }
        val aspectPlanets = aspectHouses.flatMap { birthByHouse[it].orEmpty() }.distinct().joinToString(", ").ifBlank { "त्या भावात जन्मग्रह नाही" }
        val topic = houseMeaning(house, kind)
        val change = when (offset) {
            0 -> "आजची आधारस्थिती"
            else -> {
                val prev = previousDay(moonIndex, g, date.minusDays(1), kind, birthByHouse, previousTransitPositions)
                buildChange(prev, house, idx, nak.marathi, pada)
            }
        }
        return FrameworkDay(date, Rashi.entries[idx].marathi, house, nak.marathi, pada, info.nakshatraLord,
            info.navamshaRashi, info.navamshaLord, info.rashiLord, degreeText(rawLongitude), aspectText,
            aspectHouses, aspectPlanets, topic, change)
    }

    private fun previousDay(moonIndex: Int, g: Graha, date: LocalDate, kind: FrameworkKind,
                            birthByHouse: Map<Int, List<String>>, previousTransitPositions: Map<Graha, Double>?): FrameworkDay {
        val positions = previousTransitPositions ?: emptyMap()
        val rawLongitude = positions[g] ?: 0.0
        val idx = rashiIndex(rawLongitude)
        val house = (idx - moonIndex + 12) % 12 + 1
        val nakIndex = (rawLongitude / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        val nak = Nakshatra.entries[nakIndex]
        val pada = (((rawLongitude % (360.0 / 27.0)) / (360.0 / 108.0)).toInt() + 1).coerceIn(1, 4)
        val info = JyotishMaster.getInfo(Rashi.entries[idx], nak, pada)
        val aspectHouses = aspectHouses(g, house)
        val aspectText = if (aspectHouses.isEmpty()) "—" else aspectHouses.joinToString(", ") { "${it}वा भाव" }
        val aspectPlanets = aspectHouses.flatMap { birthByHouse[it].orEmpty() }.distinct().joinToString(", ").ifBlank { "त्या भावात जन्मग्रह नाही" }
        return FrameworkDay(date, Rashi.entries[idx].marathi, house, nak.marathi, pada, info.nakshatraLord,
            info.navamshaRashi, info.navamshaLord, info.rashiLord, degreeText(rawLongitude), aspectText,
            aspectHouses, aspectPlanets, houseMeaning(house, kind), "तुलनेसाठी मागील दिवसाची स्थिती")
    }

    private fun buildChange(prev: FrameworkDay, house: Int, idx: Int, nak: String, pada: Int): String {
        val changes = mutableListOf<String>()
        if (prev.house != house) changes += "भाव ${prev.house}वा → ${house}वा"
        if (prev.rashi != Rashi.entries[idx].marathi) changes += "रास ${prev.rashi} → ${Rashi.entries[idx].marathi}"
        if (prev.nakshatra != nak) changes += "नक्षत्र ${prev.nakshatra} → $nak"
        if (prev.pada != pada) changes += "चरण ${prev.pada} → $pada"
        return if (changes.isEmpty()) "मुख्य स्थिती कायम; सूक्ष्म बदल तपासा." else changes.joinToString(" • ")
    }

    private fun buildSections(g: Graha, bp: BirthChartCalculator.PlanetPosition, birthRashi: Rashi,
                              birthNak: Nakshatra, birthInfo: JyotishInfo, now: FrameworkDay, kind: FrameworkKind,
                              birthByHouse: Map<Int, List<String>>): List<StudySection> {
        val subject = subjects[g] ?: "संबंधित ग्रहविषय"
        val aspects = aspectHouses(g, now.house)
        val aspectBirth = aspects.flatMap { birthByHouse[it].orEmpty() }.distinct().joinToString(", ").ifBlank { "दृष्टीच्या भावात जन्मग्रह नाही" }
        val baseSections = listOf(
            StudySection("ग्रह", listOf(
                "हा ग्रह कोणता?" to "${g.marathi}",
                "ग्रहाचे सर्व प्रमुख कारकत्व काय?" to subject,
                "हा ग्रह कोणत्या व्यक्ती/घटनांचे प्रतिनिधित्व करतो?" to planetPeopleEvents(g),
                "सकारात्मक बाजू कोणती?" to planetPositive(g),
                "सावध बाजू कोणती?" to planetCaution(g),
                "या ग्रहाचे कारकत्व गोचर भावाशी कसे मिसळायचे?" to "आधी ग्रहाचे कारकत्व समजा; मग ते सध्याच्या गोचर भावाच्या विषयावर लावा."
            )),
            StudySection("गोचर भाव", listOf(
                "गोचर भाव कोणता?" to "${now.house}वा भाव — reference: जन्म चंद्रराशीपासून",
                "या भावाचे प्रमुख विषय कोणते?" to houseFullMeaning(now.house),
                "या भावाचा आर्थिक/व्यवसाय/मानसिक अर्थ काय?" to houseDomainMeaning(now.house, kind),
                "ग्रह + भाव यांचा प्राथमिक अर्थ काय?" to "${g.marathi} चे कारकत्व + ${now.house}व्या भावाचे विषय = पहिला interpretation.",
                "या भावातून कोणत्या घटना/क्षेत्राकडे लक्ष द्यायचे?" to now.topic
            )),
            StudySection("गोचर रास", listOf(
                "गोचर रास कोणती?" to now.rashi,
                "राशीचा स्वामी कोण?" to now.rashiLord,
                "राशीचे तत्त्व/स्वभाव काय?" to rashiNature(now.rashi),
                "ग्रह आणि राशीचे नाते काय?" to planetRashiRelation(g, now.rashi),
                "ग्रह स्वगृही/उच्च/नीच/मित्र/शत्रू आहे का?" to dignity(g, now.rashi),
                "राशीमुळे ग्रहाच्या फलितात काय बदलतो?" to "राशी ग्रहाच्या नैसर्गिक कारकत्वाला दिशा व अभिव्यक्तीचे वातावरण देते."
            )),
            StudySection("जन्मकुंडलीतील ग्रह", listOf(
                "जन्मकुंडलीतील ग्रह कोणत्या भावात आहे?" to "${bp.house}वा भाव — भाव reference: जन्मलग्नापासून",
                "जन्मराशी कोणती?" to birthRashi.marathi,
                "जन्मग्रह कोणते मूलभूत जीवनक्षेत्र जोडतो?" to subject,
                "जन्मग्रहाचा नक्षत्र/चरण कोणता?" to "${birthNak.marathi} / ${bp.pada} चरण",
                "जन्म नक्षत्र स्वामी कोण?" to birthInfo.nakshatraLord,
                "जन्म नवांश कोणता?" to "${birthInfo.navamshaRashi} — स्वामी ${birthInfo.navamshaLord}",
                "जन्मग्रह + आजचा गोचर कसा जोडायचा?" to "जन्मग्रहाची मूलभूत भूमिका स्थिर ठेवून सध्याच्या गोचर भाव/राशीमुळे ती कोणत्या क्षेत्रात सक्रिय झाली ते पाहायचे."
            )),
            StudySection("दृष्टी", listOf(
                "गोचर ग्रहाची दृष्टी कोणती?" to now.aspects,
                "दृष्टी कोणत्या भावांवर पडते?" to aspects.joinToString(", ") { "${it}वा भाव" }.ifBlank { "—" },
                "त्या भावांचे विषय काय?" to aspects.joinToString(" • ") { "${it}वा: ${houseFullMeaning(it)}" },
                "दृष्टीतील जन्मग्रह कोणते?" to aspectBirth,
                "दृष्टीतील ग्रहांचे कारकत्व काय विचारायचे?" to "दृष्टी पडलेल्या प्रत्येक जन्मग्रहाचे कारकत्व + त्याचा भाव + त्या भावाचा स्वामी वेगळा विचारायचा.",
                "दृष्टीचा अंतिम उपयोग काय?" to "मुख्य गोचर भावाबाहेरील अतिरिक्त सक्रिय जीवनक्षेत्र शोधणे."
            )),
            StudySection("नक्षत्र", listOf(
                "गोचर नक्षत्र कोणते?" to "${now.nakshatra}",
                "नक्षत्र स्वामी कोण?" to now.nakshatraLord,
                "नक्षत्राचे पारंपरिक विषय कोणते?" to nakshatraMeaning(now.nakshatra),
                "नक्षत्र स्वामी जन्मकुंडलीत कुठे आहे?" to "नक्षत्र स्वामी ${now.nakshatraLord}; त्याची जन्मकुंडलीतील भाव/राशी/स्वामित्व स्वतंत्रपणे तपासा.",
                "गोचर ग्रह → नक्षत्र → नक्षत्र स्वामी हा chain काय सांगतो?" to "ग्रहाचे फलित नक्षत्र स्वामीच्या जन्मस्थितीशी जोडून सूक्ष्म केले जाते.",
                "जन्म नक्षत्राशी काय तुलना करायची?" to "जन्म नक्षत्र, गोचर नक्षत्र आणि ताराबल यांचा संबंध तपासा."
            )),
            StudySection("चरण", listOf(
                "गोचर चरण कोणता?" to "${now.pada} चरण",
                "चरणाची नवांश रास कोणती?" to now.navamshaRashi,
                "नवांश स्वामी कोण?" to now.navamshaLord,
                "चरणामुळे काय सूक्ष्म फरक पडतो?" to "नक्षत्राची दिशा अधिक सूक्ष्म करून नवांश/चरणातून फलिताची अभिव्यक्ती कोणत्या स्वरूपात दिसू शकते ते अभ्यासायचे.",
                "नक्षत्र स्वामी + नवांश स्वामी यांचा संबंध काय?" to "दोन्ही स्वामींची जन्मकुंडलीतील स्थिती तुलना करून सूक्ष्म interpretation करायचे."
            )),
            StudySection("तुलनात्मक जोडणी", listOf(
                "जन्मग्रहाचा भाव कोणता?" to "${bp.house}वा — लग्नापासून",
                "गोचर ग्रहाचा भाव कोणता?" to "${now.house}वा — चंद्रापासून",
                "${bp.house}वा → ${now.house}वा या बदलाचा अभ्यास काय?" to "जन्मातील स्थिर ग्रहभूमिका सध्याच्या सक्रिय गोचर क्षेत्राशी जोडायची.",
                "चंद्रकुंडली काय सांगते?" to "मन/अनुभवाच्या पातळीवर गोचर भावाचा विषय कसा जाणवू शकतो ते पाहायचे.",
                "लग्नकुंडली काय सांगते?" to "प्रत्यक्ष जीवनातील व्यवहार/घटना पातळीवरील भावसंबंध पाहायचा.",
                "दशा उपलब्ध असल्यास काय विचारायचे?" to "दशा ग्रह सध्याच्या गोचर विषयाला support करतो का ते तपासायचे.",
                "अंतिम भाकीत कधी करायचे?" to "सर्व स्वतंत्र संकेतांचा समान संदर्भात मेळ बसल्यानंतरच."
            ))
        )
        return baseSections.mapIndexed { index, section ->
            if (index <= 6) {
                val domain = domainStudyPoint(kind, index, g, bp, birthRashi, now)
                section.copy(points = section.points + domain)
            } else section
        } + conceptSection(g, now, bp, kind)
    }

    private fun domainStudyPoint(
        kind: FrameworkKind,
        index: Int,
        g: Graha,
        bp: BirthChartCalculator.PlanetPosition,
        birthRashi: Rashi,
        now: FrameworkDay
    ): Pair<String, String> {
        return when (kind) {
            FrameworkKind.MEDICAL -> medicalSectionPoint(index, g, bp, birthRashi, now)
            FrameworkKind.BUSINESS -> businessSectionPoint(index, g, bp, birthRashi, now)
            FrameworkKind.EDUCATION -> educationSectionPoint(index, g, bp, birthRashi, now)
            FrameworkKind.VASTU -> vastuSectionPoint(index, g, bp, birthRashi, now)
        }
    }

    private fun medicalSectionPoint(index: Int, g: Graha, bp: BirthChartCalculator.PlanetPosition, birthRashi: Rashi, now: FrameworkDay): Pair<String, String> = when(index) {
        0 -> "🩺 आरोग्याच्या दृष्टीने या ग्रहाचा पूर्ण विचार काय?" to "${medicalPlanet(g)}. शरीरातील संबंधित function, vitality आणि पारंपरिक health symbolism प्रथम समजून घ्या."
        1 -> "🩺 या गोचर भावाचा सूक्ष्म health अर्थ काय?" to "${medicalHouse(now.house)}. त्या भावाशी जोडलेले body/health themes, routine, stress, recovery आणि chronic/acute symbolism वेगळे तपासा."
        2 -> "🩺 या गोचर राशीचा health संदर्भ काय?" to "${now.rashi} राशीशी जोडलेला पारंपरिक body-region/constitution theme, राशी स्वामी ${now.rashiLord} आणि ग्रहाचे health significations एकत्र तपासा."
        3 -> "🩺 जन्मग्रहाची आरोग्यभूमिका काय?" to "जन्मग्रह ${bp.house}वा भाव — ${birthRashi.marathi}. जन्मस्थिती baseline tendency म्हणून वाचा; 1, 6, 8, 12 भावांशी संबंध आणि ग्रहबल स्वतंत्रपणे तपासा."
        4 -> "🩺 दृष्टीतील भाव/ग्रहांचा health विचार काय?" to "दृष्टी ${now.aspects}. दृष्टी पडलेल्या भावाचा health अर्थ आणि तिथे असलेल्या जन्मग्रहांचे body/system significations जोडून अतिरिक्त health theme शोधा."
        5 -> "🩺 नक्षत्राचा आरोग्याशी संबंध काय?" to "${now.nakshatra} — स्वामी ${now.nakshatraLord}. नक्षत्र स्वामीची जन्मस्थिती पाहून गोचर ग्रहाचा health theme कोणत्या दिशेने refine होतो ते तपासा."
        else -> "🩺 चरणाचा आरोग्याशी सूक्ष्म संबंध काय?" to "चरण ${now.pada}, नवांश ${now.navamshaRashi} (${now.navamshaLord}). चरण/नवांश हा health interpretation चा सूक्ष्म modifier म्हणून वाचा; स्वतंत्र diagnosis म्हणून नाही."
    }

    private fun businessSectionPoint(index: Int, g: Graha, bp: BirthChartCalculator.PlanetPosition, birthRashi: Rashi, now: FrameworkDay): Pair<String, String> = when(index) {
        0 -> "💼 व्यवसायाच्या दृष्टीने या ग्रहाचा पूर्ण विचार काय?" to "${businessPlanet(g)}. नेतृत्व, पैसा, sales, communication, operations, partnership किंवा risk पैकी ग्रहाचा मुख्य business role ओळखा."
        1 -> "💼 या गोचर भावाचा सूक्ष्म business अर्थ काय?" to "${businessHouse(now.house)}. त्या भावाचा revenue, client, competition, operations, management, expenses किंवा growth शी संबंध तपासा."
        2 -> "💼 या गोचर राशीचा business संदर्भ काय?" to "${now.rashi} + राशी स्वामी ${now.rashiLord}. व्यवसायातील decision style, market approach, communication, stability किंवा expansion वर राशीचा context लावा."
        3 -> "💼 जन्मग्रहाची business भूमिका काय?" to "जन्मग्रह ${bp.house}वा भाव — ${birthRashi.marathi}. व्यक्तीची मूलभूत business working style, risk appetite, resources आणि professional strengths यासाठी baseline म्हणून वाचा."
        4 -> "💼 दृष्टीतील भाव/ग्रहांचा business विचार काय?" to "दृष्टी ${now.aspects}. संबंधित भावातील business function आणि तिथल्या जन्मग्रहांचे roles जोडून secondary opportunity किंवा risk शोधा."
        5 -> "💼 नक्षत्राचा business विचार काय?" to "${now.nakshatra} — स्वामी ${now.nakshatraLord}. नक्षत्र स्वामीची जन्मस्थिती पाहून business theme कोणत्या प्रकारे execute/manifest होतो ते refine करा."
        else -> "💼 चरणाचा business विचार काय?" to "चरण ${now.pada}, नवांश ${now.navamshaRashi} (${now.navamshaLord}). निर्णय, execution, market style किंवा risk handling च्या सूक्ष्म अभिव्यक्तीसाठी चरण/नवांशाचा modifier म्हणून वापर करा."
    }

    private fun educationSectionPoint(index: Int, g: Graha, bp: BirthChartCalculator.PlanetPosition, birthRashi: Rashi, now: FrameworkDay): Pair<String, String> = when(index) {
        0 -> "📚 शिक्षणाच्या दृष्टीने या ग्रहाचा पूर्ण विचार काय?" to "${educationPlanet(g)}. learning, memory, concentration, communication, logic, creativity किंवा higher study पैकी ग्रहाची मुख्य भूमिका ओळखा."
        1 -> "📚 या गोचर भावाचा सूक्ष्म education अर्थ काय?" to "${educationHouse(now.house)}. अभ्यासाचे वातावरण, learning process, examination, practice, competition किंवा higher education यापैकी सक्रिय क्षेत्र शोधा."
        2 -> "📚 या गोचर राशीचा education संदर्भ काय?" to "${now.rashi} + राशी स्वामी ${now.rashiLord}. शिकण्याची शैली, expression, discipline, creativity, analytical approach किंवा subject preference यावर राशीचा context लावा."
        3 -> "📚 जन्मग्रहाची education भूमिका काय?" to "जन्मग्रह ${bp.house}वा भाव — ${birthRashi.marathi}. learning pattern, basic education, intelligence-related themes आणि व्यक्तीची स्थिर अभ्यासभूमिका baseline म्हणून तपासा."
        4 -> "📚 दृष्टीतील भाव/ग्रहांचा education विचार काय?" to "दृष्टी ${now.aspects}. संबंधित भावातील learning theme आणि दृष्टीतील जन्मग्रहाचे learning role जोडून concentration, examination, communication किंवा higher-study चे secondary संकेत शोधा."
        5 -> "📚 नक्षत्राचा education विचार काय?" to "${now.nakshatra} — स्वामी ${now.nakshatraLord}. नक्षत्र स्वामीची जन्मस्थिती पाहून learning motivation, subject orientation किंवा study pattern कसा refine होतो ते तपासा."
        else -> "📚 चरणाचा education विचार काय?" to "चरण ${now.pada}, नवांश ${now.navamshaRashi} (${now.navamshaLord}). concentration, expression, practice, specialisation किंवा subject-depth च्या सूक्ष्म फरकासाठी चरण/नवांश वापरा."
    }

    private fun vastuSectionPoint(index: Int, g: Graha, bp: BirthChartCalculator.PlanetPosition, birthRashi: Rashi, now: FrameworkDay): Pair<String, String> = when(index) {
        0 -> "🏠 वास्तूच्या दृष्टीने या ग्रहाचा पूर्ण विचार काय?" to "${vastuPlanet(g)}. दिशा, प्रकाश, अग्नी, जल, भार, movement, comfort किंवा spiritual/research space यापैकी ग्रहाचा symbolism ओळखा."
        1 -> "🏠 या गोचर भावाचा सूक्ष्म वास्तु संदर्भ काय?" to "${vastuHouse(now.house)}. घरातील/office मधील संबंधित space-use, प्रवेश, काम, झोप, storage किंवा central-use theme शी तुलना करा."
        2 -> "🏠 या गोचर राशीचा वास्तु संदर्भ काय?" to "${now.rashi} + राशी स्वामी ${now.rashiLord}. राशीच्या तत्त्व/स्वभावाशी दिशा, पंचमहाभूत आणि space usage यांची तुलनात्मक सांगड घाला."
        3 -> "🏠 जन्मग्रहाची वास्तु भूमिका काय?" to "जन्मग्रह ${bp.house}वा भाव — ${birthRashi.marathi}. स्थिर जन्मसंकेतांचा space-use symbolism शी संबंध तपासा; हा daily transit पेक्षा वेगळा layer आहे."
        4 -> "🏠 दृष्टीतील भाव/ग्रहांचा वास्तु विचार काय?" to "दृष्टी ${now.aspects}. संबंधित भावांचे space themes आणि तिथल्या जन्मग्रहांचे symbolism जोडून कोणत्या जागेकडे लक्ष द्यायचे ते शोधा."
        5 -> "🏠 नक्षत्राचा वास्तु विचार काय?" to "${now.nakshatra} — स्वामी ${now.nakshatraLord}. नक्षत्र स्वामीच्या symbolism वरून space-use किंवा direction-related comparison अधिक सूक्ष्म करा."
        else -> "🏠 चरणाचा वास्तु विचार काय?" to "चरण ${now.pada}, नवांश ${now.navamshaRashi} (${now.navamshaLord}). direction/element/space-use interpretation मधील सूक्ष्म modifier म्हणून वापरा; स्थिर वास्तु नियमांना पर्याय म्हणून नाही."
    }

    private fun conceptSection(g: Graha, now: FrameworkDay, bp: BirthChartCalculator.PlanetPosition, kind: FrameworkKind): StudySection {
        return when (kind) {
            FrameworkKind.MEDICAL -> StudySection("Medical Concept", medicalConceptPoints(g, now.house))
            FrameworkKind.BUSINESS -> StudySection("Business Concept", businessConceptPoints(g, now.house))
            FrameworkKind.EDUCATION -> StudySection("Education Concept", educationConceptPoints(g, now.house))
            FrameworkKind.VASTU -> StudySection("Vastu Concept", vastuConceptPoints(g, now.house))
        }
    }

    private fun medicalConceptPoints(g: Graha, h: Int): List<Pair<String, String>> = listOf(
        "या ग्रहाचा पारंपरिक health-related concept काय?" to medicalPlanet(g),
        "सध्याचा गोचर भाव कोणत्या health theme शी जोडला जातो?" to medicalHouse(h),
        "physiological system कोणता अभ्यासायचा?" to medicalSystem(g, h),
        "हा अभ्यास कसा करायचा?" to "ग्रहाचे पारंपरिक कारकत्व + भावाचा health theme + रास/नक्षत्र/दृष्टी यांची तुलना करायची; एकाच संकेतावर निष्कर्ष काढायचा नाही.",
        "काय लक्षात ठेवायचे?" to "Medical Astrology हा पारंपरिक/शैक्षणिक संकेत-अभ्यास आहे; तो वैद्यकीय diagnosis, prognosis किंवा treatment चा पर्याय नाही."
    )

    private fun businessConceptPoints(g: Graha, h: Int): List<Pair<String, String>> = listOf(
        "या ग्रहाचा business concept काय?" to businessPlanet(g),
        "सध्याचा भाव कोणत्या business function शी जोडला जातो?" to businessHouse(h),
        "finance/sales/marketing/operations/management/partnership/risk पैकी काय पाहायचे?" to businessFunction(g, h),
        "जन्मग्रहाचा business role काय?" to "जन्मभाव + ग्रहकारकत्वातून व्यक्तीची मूलभूत कार्यशैली; गोचरातून सध्याचा active business area.",
        "काय लक्षात ठेवायचे?" to "Opportunity आणि risk दोन्ही लिहायचे; एकाच ग्रहाला फक्त profit किंवा फक्त loss म्हणून पाहायचे नाही."
    )

    private fun educationConceptPoints(g: Graha, h: Int): List<Pair<String, String>> = listOf(
        "या ग्रहाचा education concept काय?" to educationPlanet(g),
        "सध्याचा भाव कोणत्या learning theme शी जोडला जातो?" to educationHouse(h),
        "learning/memory/concentration/communication/higher education/examination/skill पैकी काय सक्रिय?" to educationFunction(g, h),
        "जन्मग्रहाची भूमिका काय?" to "जन्मग्रहाची बुद्धी/शिक्षणाशी संबंधित पारंपरिक भूमिका आणि जन्मभाव स्थिर आधार देतात; गोचर सध्याची सक्रिय दिशा दाखवतो.",
        "काय लक्षात ठेवायचे?" to "शैक्षणिक निकालासाठी प्रत्यक्ष अभ्यास, वातावरण आणि प्रयत्न महत्त्वाचे आहेत; ज्योतिषीय भाग हा अभ्यासाचा interpretive layer आहे."
    )

    private fun vastuConceptPoints(g: Graha, h: Int): List<Pair<String, String>> = listOf(
        "या ग्रहाचा Vastu concept काय?" to vastuPlanet(g),
        "सध्याचा भाव कोणत्या space-use theme शी तुलना करायचा?" to vastuHouse(h),
        "दिशा व पंचमहाभूत कसे तपासायचे?" to "दिशा, पंचमहाभूत, वास्तुपुरुष मंडल आणि space usage स्वतंत्रपणे तपासून नंतर ग्रहसंकेताशी तुलना करायची.",
        "कोणत्या जागा तपासायच्या?" to "मुख्य प्रवेश, workplace/office, bedroom, kitchen, पूजा/अभ्यास जागा, धन/संचय क्षेत्र आणि Brahmasthan.",
        "काय लक्षात ठेवायचे?" to "वास्तुचे स्थिर घटक आणि दैनिक गोचर वेगळे ठेवायचे; गोचराला वास्तुचा एकमेव कारण म्हणून वापरायचे नाही."
    )

    private fun medicalPlanet(g: Graha): String = when(g){
        Graha.SURYA->"जीवनशक्ती, शरीराची उष्णता, हृदय/दृष्टीशी संबंधित पारंपरिक संकेत"
        Graha.CHANDRA->"मन, भावना, द्रव/पोषण, झोप व संवेदनशीलतेशी संबंधित पारंपरिक संकेत"
        Graha.MANGAL->"ऊर्जा, रक्त/उष्णता, inflammation/injury शी संबंधित पारंपरिक संकेत"
        Graha.BUDH->"मज्जासंस्था, त्वचा/संवाद-समन्वयाशी संबंधित पारंपरिक संकेत"
        Graha.GURU->"वाढ, पोषण, यकृत/चयापचयाशी संबंधित पारंपरिक संकेत"
        Graha.SHUKRA->"प्रजनन, हार्मोनल/सौंदर्य व मूत्र-जनन क्षेत्राशी संबंधित पारंपरिक संकेत"
        Graha.SHANI->"हाडे, सांधे, chronicity/दीर्घकालीन प्रक्रियांशी संबंधित पारंपरिक संकेत"
        Graha.RAHU->"असामान्य/अस्पष्ट लक्षणे, toxins/allergy-सदृश पारंपरिक theme"
        Graha.KETU->"सूक्ष्म/न्यूरोलॉजिकल, detachment व अनपेक्षित बदलांचे पारंपरिक theme"
    }
    private fun medicalHouse(h:Int)=when(h){1->"शरीर/constitution";2->"आहार व मुख-क्षेत्र";3->"हात-खांदे/श्वसन-समन्वय";4->"छाती/मन";5->"उदर/पचन";6->"रोग, सेवा, routine";7->"संबंधित balance";8->"दीर्घकालीन/गूढ health themes";9->"ज्ञान/मानसिक दृष्टी";10->"दैनंदिन कार्यक्षमता";11->"recovery/support network";12->"विश्रांती, झोप, isolation";else->"आरोग्य theme"}
    private fun medicalSystem(g:Graha,h:Int):String="${medicalPlanet(g)} + ${medicalHouse(h)}; नंतर रास, नक्षत्र आणि दृष्टीने theme refine करा."

    private fun businessPlanet(g:Graha)=when(g){
        Graha.SURYA->"leadership, authority, government, brand reputation"
        Graha.CHANDRA->"public demand, customer mood, adaptability"
        Graha.MANGAL->"execution, competition, machinery, action"
        Graha.BUDH->"sales, communication, accounting, analysis, marketing"
        Graha.GURU->"strategy, advisory, expansion, knowledge"
        Graha.SHUKRA->"branding, luxury, design, customer relations"
        Graha.SHANI->"operations, labour, compliance, systems, long-term structure"
        Graha.RAHU->"technology, foreign markets, unconventional growth"
        Graha.KETU->"specialisation, research, detachment from routine"
    }
    private fun businessHouse(h:Int)=when(h){2->"finance/cash flow";3->"sales/marketing/communication";6->"operations/competition/debt";7->"partnership/clients/trade";8->"risk, tax, joint resources";10->"management/career/authority";11->"profit/network/growth";12->"expenses/foreign/overhead";else->houseFullMeaning(h)}
    private fun businessFunction(g:Graha,h:Int):String="ग्रहाचा business role '${businessPlanet(g)}' आणि भावाचा function '${businessHouse(h)}' एकत्र वाचा."

    private fun educationPlanet(g:Graha)=when(g){
        Graha.SURYA->"confidence, leadership, self-expression"
        Graha.CHANDRA->"memory, emotional learning, receptivity"
        Graha.MANGAL->"competitive drive, practical/action learning"
        Graha.BUDH->"learning, memory, communication, logic, analysis"
        Graha.GURU->"higher education, wisdom, mentoring, conceptual learning"
        Graha.SHUKRA->"arts, creativity, aesthetics, social learning"
        Graha.SHANI->"discipline, repetition, patience, structured study"
        Graha.RAHU->"technology, unconventional subjects, experimentation"
        Graha.KETU->"deep research, concentration, specialised study"
    }
    private fun educationHouse(h:Int)=when(h){2->"speech/basic learning";3->"communication, writing, practice";4->"foundation, school environment, emotional security";5->"learning, memory, intelligence, examination";6->"routine, competition, service-oriented skills";9->"higher education, philosophy";10->"career-oriented learning";11->"results, networks, opportunities";12->"retreat, foreign education, rest";else->houseFullMeaning(h)}
    private fun educationFunction(g:Graha,h:Int):String="ग्रहाचा learning role '${educationPlanet(g)}' आणि भावाचा education theme '${educationHouse(h)}' एकत्र अभ्यासा."

    private fun vastuPlanet(g:Graha)=when(g){
        Graha.SURYA->"प्रकाश, authority, central vitality; पूर्व/सूर्य-संबंधित symbolism"
        Graha.CHANDRA->"जल, मन, comfort; उत्तर-पश्चिम/जल-संबंधित symbolism"
        Graha.MANGAL->"अग्नी, ऊर्जा, उपकरणे; दक्षिण/आग्नेय-संबंधित symbolism"
        Graha.BUDH->"communication, learning, व्यापार; उत्तर-संबंधित symbolism"
        Graha.GURU->"ज्ञान, विस्तार, पूजा/शिक्षण; ईशान्य-संबंधित symbolism"
        Graha.SHUKRA->"सुख, सौंदर्य, सुविधा; आग्नेय/दक्षिण-पूर्वाशी पारंपरिक तुलना"
        Graha.SHANI->"रचना, भार, शिस्त, storage; नैऋत्य-संबंधित symbolism"
        Graha.RAHU->"असामान्य/technology spaces; वायव्य/उत्तर-पश्चिमाशी तुलनात्मक symbolism"
        Graha.KETU->"detachment, spiritual/research space; सूक्ष्म/आध्यात्मिक जागेची तुलना"
    }
    private fun vastuHouse(h:Int)=when(h){1->"entrance/identity space";2->"storage/wealth and speech-related space";4->"home comfort/central living";5->"study/creative space";6->"service/work and maintenance";7->"partnership/client-facing space";8->"hidden/storage/maintenance issues";9->"prayer/learning/temple-like space";10->"workplace/office";11->"networking/gains space";12->"sleep/rest/foreign or secluded space";else->"space usage theme"}

    private fun buildReasoning(g: Graha, bp: BirthChartCalculator.PlanetPosition, birthRashi: Rashi, birthNak: Nakshatra,
                               birthInfo: JyotishInfo, now: FrameworkDay, kind: FrameworkKind,
                               birthByHouse: Map<Int, List<String>>): String {
        val aspects = aspectHouses(g, now.house)
        val aspectBirth = aspects.flatMap { birthByHouse[it].orEmpty() }.distinct().joinToString(", ").ifBlank { "जन्मग्रह नाही" }
        return buildString {
            appendLine("1. ${g.marathi}: ${subjects[g] ?: "संबंधित ग्रहविषय"}.")
            appendLine("2. गोचर भाव: ${now.house}वा भाव, जन्म चंद्रराशीपासून; विषय: ${houseFullMeaning(now.house)}.")
            appendLine("3. गोचर रास: ${now.rashi}; राशी स्वामी: ${now.rashiLord}; स्थिती: ${dignity(g, now.rashi)}.")
            appendLine("4. जन्मग्रह: ${bp.house}वा भाव, ${birthRashi.marathi}; नक्षत्र ${birthNak.marathi}, चरण ${bp.pada}, नक्षत्र स्वामी ${birthInfo.nakshatraLord}.")
            appendLine("5. दृष्टी: ${now.aspects}; दृष्टीतील जन्मग्रह: $aspectBirth.")
            appendLine("6. नक्षत्र: ${now.nakshatra}; स्वामी ${now.nakshatraLord}; विषय: ${nakshatraMeaning(now.nakshatra)}.")
            appendLine("7. चरण: ${now.pada}; नवांश ${now.navamshaRashi}; नवांश स्वामी ${now.navamshaLord}.")
            appendLine("8. संयुक्त Logic: ग्रहाचे कारकत्व + गोचर भाव + रास + जन्मग्रहाची मूलभूत भूमिका + दृष्टी + नक्षत्र + चरण/नवांश हे एकत्र वाचून सक्रिय जीवनक्षेत्र ठरवायचे.")
        }
    }

    private fun buildPrediction(g: Graha, now: FrameworkDay, bp: BirthChartCalculator.PlanetPosition, kind: FrameworkKind): String =
        "अभ्यासात्मक निष्कर्ष: ${g.marathi} च्या कारकत्वाचा सध्याच्या ${now.house}व्या गोचर भावाशी (${houseFullMeaning(now.house)}) संबंध जोडला जातो. ${now.rashi} रास, ${now.nakshatra} नक्षत्र, चरण ${now.pada} आणि दृष्टी हे परिणामाचे सूक्ष्म modifier आहेत. जन्मग्रह ${bp.house}व्या भावातील स्थिर पार्श्वभूमी देतो. त्यामुळे अंतिम फलित हे एकाच घटकावर नाही तर संपूर्ण संकेतांच्या संयोगावर आधारित आहे."

    private fun aspectHouses(g: Graha, house: Int): List<Int> {
        val distances = when (g) { Graha.MANGAL -> listOf(4,7,8); Graha.GURU -> listOf(5,7,9); Graha.SHANI -> listOf(3,7,10); else -> listOf(7) }
        return distances.map { ((house + it - 2) % 12) + 1 }
    }

    private fun houseFullMeaning(h: Int): String = when (h) {
        1 -> "स्वरूप, शरीर, व्यक्तिमत्त्व, आरंभ, आत्मदृष्टी"
        2 -> "धन, कुटुंब, वाणी, आहार, संचय"
        3 -> "पराक्रम, प्रयत्न, communication, लेखन, marketing, भावंडे, छोटे प्रवास"
        4 -> "घर, माता, सुख, मानसिक शांतता, शिक्षणाची पायाभरणी, मालमत्ता"
        5 -> "बुद्धी, शिक्षण, सर्जनशीलता, संतती, निर्णय, speculation"
        6 -> "रोग, सेवा, कर्ज, शत्रू, स्पर्धा, दैनंदिन काम"
        7 -> "विवाह, भागीदारी, clients, व्यापार, public dealing"
        8 -> "अचानक बदल, संशोधन, गुप्त विषय, दीर्घकालीन/संयुक्त संसाधने"
        9 -> "भाग्य, धर्म/तत्त्वज्ञान, गुरु, उच्च ज्ञान, दूरचा प्रवास"
        10 -> "कर्म, profession, पद, authority, reputation, public role"
        11 -> "लाभ, उत्पन्न, इच्छा पूर्ती, मित्र, नेटवर्क, मोठे संपर्क"
        12 -> "खर्च, परदेश, विश्रांती, रुग्णालय/एकांत, सोडून देणे"
        else -> "जीवनक्षेत्र"
    }

    private fun houseDomainMeaning(h: Int, kind: FrameworkKind): String = when (kind) {
        FrameworkKind.MEDICAL -> when(h){1->"शरीर/स्वास्थ्य";2->"आहार/वाणी";3->"हात-खांदे/प्रयत्न";4->"छाती/मन";5->"पचन/उदर";6->"रोग/सेवा";7->"संबंध";8->"दीर्घकालीन संकेत";9->"ज्ञान/भाग्य";10->"कर्म";11->"लाभ/सामाजिक सक्रियता";12->"विश्रांती/खर्च";else->"आरोग्य क्षेत्र"}
        FrameworkKind.BUSINESS -> when(h){2->"पैसा/भांडवल";3->"marketing/communication";6->"competition/operations";7->"partnership/clients";10->"business/career";11->"profit/network";12->"expenses/foreign";else->houseFullMeaning(h)}
        FrameworkKind.EDUCATION -> when(h){4->"मूलभूत शिक्षण/मन";5->"बुद्धी/learning";9->"higher education";2->"वाणी/आहार";3->"प्रयत्न/communication";10->"career";else->houseFullMeaning(h)}
        FrameworkKind.VASTU -> "दिशा/घर/वास्तुशी तुलनात्मक अभ्यास"
    }

    private fun houseMeaning(h: Int, kind: FrameworkKind): String = houseDomainMeaning(h, kind)

    private val subjects = mapOf(
        Graha.SURYA to "आत्मविश्वास, अधिकार, सरकारी काम, वडील/वरिष्ठ, प्रतिष्ठा, नेतृत्व, पद, मान-सन्मान, निर्णयक्षमता",
        Graha.CHANDRA to "मन, भावना, सवय, संवेदनशीलता, माता, जनसंपर्क, प्रवाहशीलता",
        Graha.MANGAL to "ऊर्जा, धाडस, कृती, स्पर्धा, जमीन, तांत्रिक काम, भाऊ/बंधू",
        Graha.BUDH to "बुद्धी, संवाद, व्यापार, गणित, लेखन, analysis, marketing",
        Graha.GURU to "ज्ञान, गुरु, मार्गदर्शन, विस्तार, शिक्षण, भाग्य, मूल्यव्यवस्था",
        Graha.SHUKRA to "संबंध, सुख, कला, सौंदर्य, पैसा, सुविधा, वाहन/भौतिक सुख",
        Graha.SHANI to "शिस्त, विलंब, कामगार, जबाबदारी, सेवा, दीर्घकालीन प्रयत्न, संरचना",
        Graha.RAHU to "आकांक्षा, परकीय विषय, असामान्य मार्ग, तंत्रज्ञान, भ्रम, अचानक विस्तार",
        Graha.KETU to "विरक्ती, संशोधन, अंतर्मुखता, आध्यात्मिकता, सूक्ष्म निरीक्षण, तुटकपणा"
    )

    private fun planetPeopleEvents(g: Graha): String = when(g){
        Graha.SURYA->"वडील, वरिष्ठ, अधिकारी, सरकारी संस्था, नेतृत्वाची भूमिका"
        Graha.CHANDRA->"माता, कुटुंबातील भावनिक संबंध, जनता, स्त्रीत्व/पालनपोषण"
        Graha.MANGAL->"भाऊ, प्रतिस्पर्धी, सैनिक/तांत्रिक व्यक्ती, कृतीप्रधान प्रसंग"
        Graha.BUDH->"व्यापारी, विद्यार्थी, लेखक, accountant, communicator"
        Graha.GURU->"गुरु, शिक्षक, सल्लागार, ज्येष्ठ मार्गदर्शक"
        Graha.SHUKRA->"जोडीदार, कला/सौंदर्य क्षेत्र, सुविधा देणारे संबंध"
        Graha.SHANI->"कामगार, कर्मचारी, सेवकवर्ग, वरिष्ठ जबाबदारी/संस्था"
        Graha.RAHU->"परकीय/असामान्य संपर्क, technology, unconventional networks"
        Graha.KETU->"संशोधन, एकांत, detachment, सूक्ष्म/आध्यात्मिक विषय"
    }

    private fun planetPositive(g: Graha): String = when(g){
        Graha.SURYA->"आत्मविश्वास, नेतृत्व, authority, मान-सन्मान"
        Graha.CHANDRA->"समजूतदारपणा, adaptability, लोकांशी जोडणी"
        Graha.MANGAL->"धाडस, कृती, स्पर्धात्मकता, ऊर्जा"
        Graha.BUDH->"बुद्धी, संवाद, गणना, व्यापारकौशल्य"
        Graha.GURU->"ज्ञान, संरक्षण, विस्तार, मार्गदर्शन"
        Graha.SHUKRA->"समन्वय, कला, संबंध, सुविधा"
        Graha.SHANI->"शिस्त, सहनशीलता, सातत्य, रचना"
        Graha.RAHU->"नवीन तंत्रज्ञान, मोठी आकांक्षा, unconventional opportunity"
        Graha.KETU->"संशोधन, एकाग्र अंतर्मुखता, सूक्ष्म निरीक्षण"
    }

    private fun planetCaution(g: Graha): String = when(g){
        Graha.SURYA->"अहंकार, हट्ट, वरिष्ठांशी तणाव, अतिअधिकारभाव"
        Graha.CHANDRA->"अस्थिर मन, भावनिक प्रतिक्रिया, over-sensitivity"
        Graha.MANGAL->"घाई, राग, संघर्ष, अपघाती जोखीम"
        Graha.BUDH->"अति-विचार, गैरसमज, चुकीची गणना/communication"
        Graha.GURU->"अति-विस्तार, अति-आशावाद, खर्च/अति-विश्वास"
        Graha.SHUKRA->"अति-सुखलोलुपता, संबंधातील अपेक्षा, अनावश्यक खर्च"
        Graha.SHANI->"विलंब, भीती, जडपणा, जबाबदारीचा ताण"
        Graha.RAHU->"भ्रम, अतिआकांक्षा, shortcuts, अस्पष्टता"
        Graha.KETU->"तुटकपणा, उदासीनता, अचानक दूर होणे"
    }

    private fun rashiNature(rashi: String): String = when(rashi){
        "मेष"->"अग्नी, चर, सुरुवात/कृती"; "वृषभ"->"पृथ्वी, स्थिर, संसाधन/संचय"; "मिथुन"->"वायू, द्विस्वभाव, संवाद/बुद्धी"; "कर्क"->"जल, चर, भावना/घर"; "सिंह"->"अग्नी, स्थिर, authority/leadership"; "कन्या"->"पृथ्वी, द्विस्वभाव, analysis/service"; "तुळ"->"वायू, चर, संबंध/समतोल"; "वृश्चिक"->"जल, स्थिर, गूढता/तीव्रता"; "धनु"->"अग्नी, द्विस्वभाव, ज्ञान/विस्तार"; "मकर"->"पृथ्वी, चर, कर्म/रचना"; "कुंभ"->"वायू, स्थिर, समूह/नवीनता"; "मीन"->"जल, द्विस्वभाव, अंतर्ज्ञान/विस्तार"; else->"राशीचे स्वरूप"
    }

    private fun planetRashiRelation(g: Graha, rashi: String): String = when {
        g == Graha.SURYA && rashi == "सिंह" -> "स्वगृही"
        g == Graha.CHANDRA && rashi == "कर्क" -> "स्वगृही"
        g == Graha.MANGAL && (rashi == "मेष" || rashi == "वृश्चिक") -> "स्वगृही"
        g == Graha.BUDH && (rashi == "मिथुन" || rashi == "कन्या") -> "स्वगृही"
        g == Graha.GURU && (rashi == "धनु" || rashi == "मीन") -> "स्वगृही"
        g == Graha.SHUKRA && (rashi == "वृषभ" || rashi == "तुळ") -> "स्वगृही"
        g == Graha.SHANI && (rashi == "मकर" || rashi == "कुंभ") -> "स्वगृही"
        else -> "राशी-ग्रह संबंध स्वतंत्रपणे तपासा."
    }

    private fun dignity(g: Graha, rashi: String): String = when {
        g == Graha.SURYA && rashi == "सिंह" -> "स्वगृही — पारंपरिक दृष्ट्या बळकट"
        g == Graha.SURYA && rashi == "मेष" -> "उच्च"
        g == Graha.SURYA && rashi == "तुळ" -> "नीच"
        g == Graha.CHANDRA && rashi == "कर्क" -> "स्वगृही"
        g == Graha.CHANDRA && rashi == "वृषभ" -> "उच्च"
        g == Graha.CHANDRA && rashi == "वृश्चिक" -> "नीच"
        g == Graha.MANGAL && (rashi == "मेष" || rashi == "वृश्चिक") -> "स्वगृही"
        g == Graha.MANGAL && rashi == "मकर" -> "उच्च"
        g == Graha.MANGAL && rashi == "कर्क" -> "नीच"
        g == Graha.BUDH && (rashi == "मिथुन" || rashi == "कन्या") -> "स्वगृही"
        g == Graha.BUDH && rashi == "कन्या" -> "उच्च/स्वगृही"
        g == Graha.BUDH && rashi == "मीन" -> "नीच"
        g == Graha.GURU && (rashi == "धनु" || rashi == "मीन") -> "स्वगृही"
        g == Graha.GURU && rashi == "कर्क" -> "उच्च"
        g == Graha.GURU && rashi == "मकर" -> "नीच"
        g == Graha.SHUKRA && (rashi == "वृषभ" || rashi == "तुळ") -> "स्वगृही"
        g == Graha.SHUKRA && rashi == "मीन" -> "उच्च"
        g == Graha.SHUKRA && rashi == "कन्या" -> "नीच"
        g == Graha.SHANI && (rashi == "मकर" || rashi == "कुंभ") -> "स्वगृही"
        g == Graha.SHANI && rashi == "तुळ" -> "उच्च"
        g == Graha.SHANI && rashi == "मेष" -> "नीच"
        else -> planetRashiRelation(g, rashi)
    }

    private fun nakshatraMeaning(n: String): String = when(n){
        "अश्विनी"->"वेग, आरंभ, उपचार/चपळता"; "भरणी"->"जबाबदारी, धारणशक्ती, परिवर्तन"; "कृत्तिका"->"शुद्धीकरण, निर्णय, तीक्ष्णता"; "रोहिणी"->"वृद्धी, आकर्षण, निर्मिती"; "मृगशीर्ष"->"शोध, उत्सुकता, प्रवास"; "आर्द्रा"->"तीव्र बदल, संशोधन, disruption"; "पुनर्वसू"->"पुनरागमन, पुनर्बांधणी, विस्तार"; "पुष्य"->"पोषण, शिस्त, संरक्षण"; "आश्लेषा"->"गूढता, रणनीती, अंतर्मुखता"; "मघा"->"पूर्वज, प्रतिष्ठा, अधिकार"; "पूर्वाफाल्गुनी"->"सुख, संबंध, सर्जनशीलता"; "उत्तराफाल्गुनी"->"करार, जबाबदारी, स्थैर्य"; "हस्त"->"कौशल्य, नियंत्रण, हस्तकौशल्य"; "चित्रा"->"रचना, सौंदर्य, सर्जनशीलता"; "स्वाती"->"स्वातंत्र्य, व्यापार, adaptability"; "विशाखा"->"ध्येय, विस्तार, स्पर्धात्मक साध्य"; "अनुराधा"->"मैत्री, नेटवर्क, devotion"; "ज्येष्ठा"->"जबाबदारी, संरक्षण, वरिष्ठता"; "मूळ"->"मुळाशी जाणे, संशोधन, परिवर्तन"; "पूर्वाषाढा"->"प्रेरणा, विजय, प्रभाव"; "उत्तराषाढा"->"स्थैर्य, नेतृत्व, दीर्घकालीन यश"; "श्रवण"->"ऐकणे, शिक्षण, माहिती"; "धनिष्ठा"->"संसाधने, ताल, समूह"; "शतभिषा"->"उपचार, संशोधन, गोपनीयता"; "पूर्वाभाद्रपदा"->"तीव्र आदर्श, परिवर्तन, तपस्या"; "उत्तराभाद्रपदा"->"स्थैर्य, खोल विचार, संयम"; "रेवती"->"मार्गदर्शन, प्रवास, पूर्णता"; else->"नक्षत्राचे पारंपरिक विषय"
    }

    private fun rashiIndex(v: Double) = (v/30.0).toInt().coerceIn(0,11)
}
