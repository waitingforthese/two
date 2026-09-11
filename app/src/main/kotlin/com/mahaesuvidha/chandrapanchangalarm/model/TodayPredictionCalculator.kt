package com.mahaesuvidha.chandrapanchangalarm.model


data class TransitPredictionRow(
    val graha: String,
    val birthHouse: Int?,
    val birthRashi: String?,
    val birthPosition: String,
    val rashi: String,
    val house: Int,
    val score: Int,
    val effect: String
)

data class TodayPrediction(
    val birthMoonRashi: String,
    val currentMoonRashi: String,
    val currentNakshatra: String,
    val taraName: String,
    val taraScore: Int,
    val totalScore: Int,
    val grade: String,
    val headline: String,
    val rows: List<TransitPredictionRow>,
    val summary: String,
    val doText: String,
    val avoidText: String
)

object TodayPredictionCalculator {

    private val rashis = Rashi.entries
    private val nakshatras = Nakshatra.entries

    private data class Rule(val score: Int, val effect: String)

    // Traditional Moon-sign transit baseline. The final result also considers
    // the current Moon's tara-bala and planetary aspects.
    private val rules: Map<Graha, Map<Int, Rule>> = mapOf(
        Graha.SURYA to mapOf(
            1 to Rule(-1, "स्वतः व मनावर ताण; आत्मविश्वास ठेवावा"),
            2 to Rule(-1, "धन-वाणीबाबत संयम आवश्यक"),
            3 to Rule(2, "प्रयत्न, धाडस व संपर्कासाठी अनुकूल"),
            4 to Rule(-1, "घरगुती व मानसिक ताण संभवतो"),
            5 to Rule(-1, "निर्णय घाईने घेऊ नयेत"),
            6 to Rule(2, "स्पर्धा व अडचणींवर मात करण्यास मदत"),
            7 to Rule(-1, "संबंध व भागीदारीत संयम आवश्यक"),
            8 to Rule(-2, "अचानक अडथळ्यांपासून सावध"),
            9 to Rule(-1, "भाग्य व गुरुजनांच्या बाबतीत संयम"),
            10 to Rule(2, "कर्म, नोकरी व व्यवसायासाठी सक्रियता"),
            11 to Rule(2, "लाभ व उत्पन्नासाठी अनुकूल"),
            12 to Rule(-1, "खर्च व थकवा वाढू शकतो")
        ),
        Graha.CHANDRA to mapOf(
            1 to Rule(1, "मन व वैयक्तिक कामांसाठी अनुकूल"),
            2 to Rule(-1, "कुटुंब व आर्थिक विचार वाढू शकतात"),
            3 to Rule(2, "उत्साह, संवाद व प्रयत्नांना चालना"),
            4 to Rule(-1, "भावनिक चढ-उतार संभवतात"),
            5 to Rule(-1, "भावनांवर आधारित निर्णय टाळावेत"),
            6 to Rule(1, "काम व स्पर्धेत सक्रियता"),
            7 to Rule(1, "भेटीगाठी व संबंधांसाठी अनुकूल"),
            8 to Rule(-2, "मन अस्थिर होऊ शकते; सावध राहावे"),
            9 to Rule(1, "ज्ञान व प्रवासाची इच्छा वाढू शकते"),
            10 to Rule(1, "कामाकडे लक्ष वाढते"),
            11 to Rule(2, "लाभ व इच्छापूर्तीचे संकेत"),
            12 to Rule(-1, "खर्च व विश्रांतीची गरज")
        ),
        Graha.MANGAL to mapOf(
            1 to Rule(-1, "उतावळेपणा व चिडचिड टाळावी"),
            2 to Rule(-1, "वाणी व आर्थिक व्यवहारात सावधगिरी"),
            3 to Rule(2, "धाडस, पराक्रम व प्रयत्नांना बळ"),
            4 to Rule(-1, "घर/मालमत्तेबाबत वाद टाळावेत"),
            5 to Rule(-1, "घाईचे निर्णय टाळावेत"),
            6 to Rule(2, "स्पर्धा व विरोधावर मात करण्याची क्षमता"),
            7 to Rule(-2, "भागीदारी व संबंधात वादाची शक्यता"),
            8 to Rule(-2, "अचानक अडथळे; जोखीम टाळावी"),
            9 to Rule(-1, "प्रवासात घाई टाळावी"),
            10 to Rule(2, "कामात ऊर्जा व कृतीशीलता"),
            11 to Rule(2, "लाभासाठी जोरदार प्रयत्न"),
            12 to Rule(-1, "अनावश्यक खर्च व ताण टाळावा")
        ),
        Graha.BUDH to mapOf(
            1 to Rule(1, "बुद्धी व संवाद सक्रिय"),
            2 to Rule(2, "धन, व्यवहार व वाणीला चालना"),
            3 to Rule(1, "संवाद, लेखन व प्रयत्नांसाठी चांगला"),
            4 to Rule(2, "शिक्षण, कागदपत्रे व घरगुती नियोजन"),
            5 to Rule(1, "बुद्धी, शिक्षण व सर्जनशीलतेस अनुकूल"),
            6 to Rule(2, "व्यवहार, सेवा व स्पर्धेत फायदा"),
            7 to Rule(1, "व्यवसायिक संवाद व भागीदारी"),
            8 to Rule(2, "संशोधन व गुप्त बाबींमध्ये विचारशक्ती"),
            9 to Rule(1, "ज्ञान व प्रवास नियोजनासाठी चांगला"),
            10 to Rule(2, "व्यवसाय, नोकरी व निर्णयांसाठी अनुकूल"),
            11 to Rule(2, "व्यापार, संपर्क व लाभासाठी चांगला"),
            12 to Rule(-1, "कागदपत्रे व खर्चात चुका टाळाव्यात")
        ),
        Graha.GURU to mapOf(
            1 to Rule(1, "मनाला आधार; ज्ञान व मार्गदर्शन मिळू शकते"),
            2 to Rule(2, "धन, कुटुंब व वाणीला शुभ आधार"),
            3 to Rule(-1, "प्रयत्न करावे लागतील; परिणाम हळू मिळू शकतो"),
            4 to Rule(-1, "घरगुती जबाबदाऱ्या वाढू शकतात"),
            5 to Rule(2, "शिक्षण, बुद्धी व संततीसाठी शुभ"),
            6 to Rule(-1, "कामाचा विस्तार; कर्ज/खर्चाकडे लक्ष"),
            7 to Rule(2, "विवाह, संबंध व भागीदारीसाठी अनुकूल"),
            8 to Rule(-1, "अंतर्गत बदल व अनिश्चितता; संयम आवश्यक"),
            9 to Rule(2, "भाग्य, ज्ञान, गुरुजन व प्रवासासाठी शुभ"),
            10 to Rule(-1, "कामात बदल/जबाबदारी; शिस्त आवश्यक"),
            11 to Rule(2, "लाभ, उत्पन्न व इच्छा पूर्ण होण्यास मदत"),
            12 to Rule(-1, "दान/प्रवास/खर्च वाढू शकतो; नियोजन आवश्यक")
        ),
        Graha.SHUKRA to mapOf(
            1 to Rule(2, "सुख, आकर्षण व सामाजिक संबंधांसाठी चांगला"),
            2 to Rule(2, "धन, कुटुंब व सुखसोयींसाठी अनुकूल"),
            3 to Rule(1, "संपर्क व सर्जनशील प्रयत्नांना चालना"),
            4 to Rule(2, "घर, वाहन व सुखासाठी अनुकूल"),
            5 to Rule(2, "प्रेम, कला, शिक्षण व सर्जनशीलतेसाठी शुभ"),
            6 to Rule(-1, "खर्च व संबंधातील गैरसमज टाळावेत"),
            7 to Rule(2, "विवाह, प्रेम व भागीदारीसाठी अनुकूल"),
            8 to Rule(-1, "आर्थिक/भावनिक बाबतीत अतिरेक टाळावा"),
            9 to Rule(1, "प्रवास, कला व ज्ञानासाठी चांगला"),
            10 to Rule(1, "व्यवसायिक संबंध व प्रतिमेस मदत"),
            11 to Rule(2, "लाभ, मैत्री व सामाजिक संपर्कासाठी शुभ"),
            12 to Rule(-1, "सुखसोयींवर अनावश्यक खर्च टाळावा")
        ),
        Graha.SHANI to mapOf(
            1 to Rule(-2, "जबाबदारी, विलंब व मानसिक ताण; शिस्त आवश्यक"),
            2 to Rule(-2, "धन व कुटुंबाबाबत संयम; खर्च नियोजन आवश्यक"),
            3 to Rule(2, "मेहनत, शिस्त व प्रयत्नांतून यश"),
            4 to Rule(-1, "घरगुती जबाबदाऱ्या व मानसिक ताण"),
            5 to Rule(-1, "शिक्षण/संतती विषयात विलंब; संयम आवश्यक"),
            6 to Rule(2, "दीर्घ प्रयत्नांतून शत्रू/अडचणींवर नियंत्रण"),
            7 to Rule(-2, "संबंध/भागीदारीत जबाबदारी व विलंब"),
            8 to Rule(-2, "अडथळे व विलंब; जोखीम कमी ठेवावी"),
            9 to Rule(-1, "भाग्यापेक्षा मेहनतीवर भर द्यावा"),
            10 to Rule(-1, "कामाची जबाबदारी व दबाव वाढू शकतो"),
            11 to Rule(2, "मेहनतीतून स्थिर लाभ व उत्पन्न"),
            12 to Rule(-2, "खर्च, विलंब व एकांत; नियोजन आवश्यक")
        ),
        Graha.RAHU to mapOf(
            1 to Rule(-1, "गोंधळ व अस्थिरता; निर्णय तपासून घ्यावेत"),
            2 to Rule(-1, "वाणी व आर्थिक व्यवहारात सावध"),
            3 to Rule(2, "धाडसी प्रयत्न व संपर्कातून फायदा"),
            4 to Rule(-1, "घरगुती अस्थिरता संभवते"),
            5 to Rule(-1, "अति कल्पना/जोखीम टाळावी"),
            6 to Rule(2, "स्पर्धेत अनपेक्षित फायदा"),
            7 to Rule(-1, "संबंधात गैरसमज टाळावेत"),
            8 to Rule(-2, "अचानक बदल; जोखीम टाळावी"),
            9 to Rule(-1, "विश्वास ठेवण्यापूर्वी पडताळणी करावी"),
            10 to Rule(2, "करिअरमध्ये नवीन दिशा/संधी"),
            11 to Rule(2, "लाभ व नवीन संपर्कांची शक्यता"),
            12 to Rule(-1, "अनियोजित खर्च व विचलन टाळावे")
        ),
        Graha.KETU to mapOf(
            1 to Rule(-1, "अलिप्तता/गोंधळ; मन स्थिर ठेवावे"),
            2 to Rule(-1, "वाणी व कुटुंबीय संवादात अंतर टाळावे"),
            3 to Rule(2, "अडचणींवर मात करण्याची अंतर्गत शक्ती"),
            4 to Rule(-1, "घरगुती समाधानात चढ-उतार"),
            5 to Rule(-1, "एकाग्रता व निर्णयात सावधगिरी"),
            6 to Rule(2, "अडचणी व स्पर्धेत मात करण्यास मदत"),
            7 to Rule(-1, "संबंधात दुरावा टाळावा"),
            8 to Rule(-1, "अनिश्चितता; गुप्त/जोखमीचे व्यवहार टाळावेत"),
            9 to Rule(2, "आध्यात्मिकता व अंतर्मुख ज्ञानासाठी अनुकूल"),
            10 to Rule(-1, "कामात अलिप्तता; उद्दिष्ट स्पष्ट ठेवावे"),
            11 to Rule(1, "लाभ मिळू शकतो पण समाधान कमी असू शकते"),
            12 to Rule(2, "आध्यात्मिकता, अंतर्मुखता व एकांतासाठी अनुकूल")
        )
    )

    private val taraNames = listOf(
        "जन्म", "संपत", "विपत", "क्षेम", "प्रत्यारी",
        "साधक", "नैधन / वध", "मित्र", "परममित्र"
    )

    private fun rashiIndex(longitude: Double): Int =
        (longitude / 30.0).toInt().coerceIn(0, 11)

    private fun houseFromMoon(moonIndex: Int, transitIndex: Int): Int =
        (transitIndex - moonIndex + 12) % 12 + 1

    private fun planetBodies(): List<Graha> = listOf(
        Graha.SURYA, Graha.CHANDRA, Graha.MANGAL, Graha.BUDH,
        Graha.GURU, Graha.SHUKRA, Graha.SHANI, Graha.RAHU
    )

    private fun taraFor(birthNakshatra: String, currentNakshatra: Nakshatra): Pair<String, Int> {
        val birth = nakshatras.firstOrNull { it.marathi == birthNakshatra } ?: return ("माहिती उपलब्ध नाही" to 0)
        val n = (nakshatras.indexOf(currentNakshatra) - nakshatras.indexOf(birth) + 27) % 27
        val tara = n % 9
        return taraNames[tara] to when (tara) {
            0 -> 0
            1 -> 1
            2 -> -2
            3 -> 1
            4 -> -2
            5 -> 2
            6 -> -2
            7 -> 1
            8 -> 2
            else -> 0
        }
    }

    private fun aspectBonus(graha: Graha, house: Int, targetHouse: Int): Int {
        val distances = mutableListOf(7)
        when (graha) {
            Graha.MANGAL -> distances += listOf(4, 8)
            Graha.GURU -> distances += listOf(5, 9)
            Graha.SHANI -> distances += listOf(3, 10)
            else -> Unit
        }
        val targetDistance = (targetHouse - house + 12) % 12 + 1
        return if (targetDistance in distances) 1 else 0
    }

    fun calculate(
        birthMoonRashi: String,
        birthNakshatra: String,
        birthDate: String,
        birthTime: String,
        currentMoonRashi: String,
        currentNakshatra: Nakshatra,
        now: Long = System.currentTimeMillis(),
        birthCoordinates: Pair<Double, Double>? = null
    ): TodayPrediction {
        val birthIndex = rashis.indexOfFirst { it.marathi == birthMoonRashi }.let { if (it < 0) 0 else it }
        val birthPositions = birthCoordinates?.let { (lat, lon) ->
            BirthChartCalculator.calculate(birthDate, birthTime, lat, lon)
        } ?: emptyMap()

        val rows = mutableListOf<TransitPredictionRow>()
        planetBodies().forEach { graha ->
            val idx = rashiIndex(LiveTransitCalculator.longitudeAt(graha, now))
            val house = houseFromMoon(birthIndex, idx)
            val rule = rules[graha]?.get(house) ?: Rule(0, "सामान्य")
            val birth = birthPositions[graha]
            val birthPosition = birth?.let { "${it.house}वा भाव — ${rashis[it.rashiIndex].marathi}" } ?: "जन्मस्थिती उपलब्ध नाही"
            rows += TransitPredictionRow(
                graha.marathi,
                birth?.house,
                birth?.let { rashis[it.rashiIndex].marathi },
                birthPosition,
                rashis[idx].marathi,
                house,
                rule.score,
                rule.effect
            )
        }

        // Ketu is always 180° from Rahu.
        val rahuRow = rows.firstOrNull { it.graha == "राहू" }
        if (rahuRow != null) {
            val ketuIdx = (rashis.indexOfFirst { it.marathi == rahuRow.rashi } + 6) % 12
            val house = houseFromMoon(birthIndex, ketuIdx)
            val rule = rules[Graha.KETU]?.get(house) ?: Rule(0, "सामान्य")
            val pos = rows.indexOfFirst { it.graha == "राहू" } + 1
            val birth = birthPositions[Graha.KETU]
            val birthPosition = birth?.let { "${it.house}वा भाव — ${rashis[it.rashiIndex].marathi}" } ?: "जन्मस्थिती उपलब्ध नाही"
            rows.add(pos, TransitPredictionRow(
                "केतू",
                birth?.house,
                birth?.let { rashis[it.rashiIndex].marathi },
                birthPosition,
                rashis[ketuIdx].marathi,
                house,
                rule.score,
                rule.effect
            ))
        }

        // Small aspect contribution to the Moon-sign first/second focus areas.
        val adjusted = rows.map { row ->
            val graha = Graha.entries.firstOrNull { it.marathi == row.graha }
            if (graha == null) row else {
                val bonus = aspectBonus(graha, row.house, 1)
                if (bonus == 0) row else row.copy(
                    score = (row.score + bonus).coerceIn(-2, 2),
                    effect = row.effect + " • चंद्रभावावर दृष्टी"
                )
            }
        }

        val (taraName, taraScore) = taraFor(birthNakshatra, currentNakshatra)
        val total = adjusted.sumOf { it.score } + taraScore
        val grade = when {
            total >= 9 -> "अत्यंत अनुकूल"
            total >= 4 -> "अनुकूल"
            total >= -3 -> "मिश्र"
            else -> "सावध"
        }

        val best = adjusted.filter { it.score >= 2 }.sortedByDescending { it.score }.take(3)
        val caution = adjusted.filter { it.score <= -2 }.sortedBy { it.score }.take(3)

        val headline = when {
            total >= 9 -> "आज अनेक बाबतीत पुढाकार घेण्यासाठी चांगला दिवस."
            total >= 4 -> "आज प्रगतीच्या संधी आहेत; योग्य नियोजन करा."
            total >= -3 -> "आजचा दिवस मिश्र आहे; चांगल्या संधींसोबत काही बाबतीत सावधगिरी आवश्यक."
            else -> "आज महत्त्वाचे निर्णय शांतपणे आणि विचारपूर्वक घ्या."
        }

        val summary = buildString {
            append(headline)
            if (best.isNotEmpty()) {
                append(" अनुकूल संकेत: ")
                append(best.joinToString(", ") { "${it.graha} ${it.house}वा भाव" })
                append(".")
            }
            if (caution.isNotEmpty()) {
                append(" सावध क्षेत्रे: ")
                append(caution.joinToString(", ") { "${it.graha} ${it.house}वा भाव" })
                append(".")
            }
        }

        val doText = when {
            total >= 4 -> "महत्त्वाची कामे, संपर्क, नियोजन आणि प्रलंबित कामांना गती द्या."
            else -> "नियोजन, नियमित कामे आणि आवश्यक संपर्क शांतपणे पूर्ण करा."
        }

        val avoidText = if (taraName.contains("विपत") || taraName.contains("प्रत्यारी") || taraName.contains("वध") || total < 0)
            "घाईचे निर्णय, अनावश्यक वाद आणि मोठी जोखीम टाळा."
        else
            "अनावश्यक खर्च आणि अतिआत्मविश्वास टाळा."

        return TodayPrediction(
            birthMoonRashi = birthMoonRashi,
            currentMoonRashi = currentMoonRashi,
            currentNakshatra = currentNakshatra.marathi,
            taraName = taraName,
            taraScore = taraScore,
            totalScore = total,
            grade = grade,
            headline = headline,
            rows = adjusted,
            summary = summary,
            doText = doText,
            avoidText = avoidText
        )
    }
}
