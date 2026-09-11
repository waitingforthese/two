package com.mahaesuvidha.chandrapanchangalarm.model

/** Traditional devotional mappings used by the Aaradhana feature. */
data class AaradhanaInfo(
    val deity: String,
    val mantra: String,
    /** Scholarly/reference note for the selected daily-japa text. */
    val source: String = "पारंपरिक ग्रह-जप संदर्भ",
    /** Human-readable pronunciation guide; kept separate from Sanskrit display text. */
    val pronunciation: String = "",
    /** Exact-mantra recording reference when a matching public recording was found. */
    val audioReferenceUrl: String = "",
    val audioReferenceLabel: String = ""
)

object AaradhanaMaster {
    private val nakshatra = mapOf(
        "अश्विनी" to AaradhanaInfo("अश्विनीकुमार", "ॐ अश्विभ्यां नमः"),
        "भरणी" to AaradhanaInfo("यम", "ॐ यमाय नमः"),
        "कृत्तिका" to AaradhanaInfo("अग्नी", "ॐ अग्नये नमः"),
        "रोहिणी" to AaradhanaInfo("प्रजापती", "ॐ प्रजापतये नमः"),
        "मृगशीर्ष" to AaradhanaInfo("सोम", "ॐ सोमाय नमः"),
        "आर्द्रा" to AaradhanaInfo("रुद्र", "ॐ रुद्राय नमः"),
        "पुनर्वसू" to AaradhanaInfo("अदिती", "ॐ अदित्यै नमः"),
        "पुष्य" to AaradhanaInfo("बृहस्पती", "ॐ बृहस्पतये नमः"),
        "आश्लेषा" to AaradhanaInfo("नाग", "ॐ नागाय नमः"),
        "मघा" to AaradhanaInfo("पितृ", "ॐ पितृभ्यो नमः"),
        "पूर्वाफाल्गुनी" to AaradhanaInfo("भग", "ॐ भगाय नमः"),
        "उत्तराफाल्गुनी" to AaradhanaInfo("अर्यमा", "ॐ अर्यम्णे नमः"),
        "हस्त" to AaradhanaInfo("सविता", "ॐ सवित्रे नमः"),
        "चित्रा" to AaradhanaInfo("त्वष्टा", "ॐ त्वष्ट्रे नमः"),
        "स्वाती" to AaradhanaInfo("वायू", "ॐ वायवे नमः"),
        "विशाखा" to AaradhanaInfo("इंद्र-अग्नी", "ॐ इन्द्राग्निभ्यां नमः"),
        "अनुराधा" to AaradhanaInfo("मित्र", "ॐ मित्राय नमः"),
        "ज्येष्ठा" to AaradhanaInfo("इंद्र", "ॐ इन्द्राय नमः"),
        "मूळ" to AaradhanaInfo("निरृति", "ॐ निरृतये नमः"),
        "पूर्वाषाढा" to AaradhanaInfo("आपः", "ॐ अद्भ्यो नमः"),
        "उत्तराषाढा" to AaradhanaInfo("विश्वेदेव", "ॐ विश्वेभ्यो देवेभ्यो नमः"),
        "श्रवण" to AaradhanaInfo("विष्णू", "ॐ विष्णवे नमः"),
        "धनिष्ठा" to AaradhanaInfo("वसू", "ॐ वसुभ्यो नमः"),
        "शतभिषा" to AaradhanaInfo("वरुण", "ॐ वरुणाय नमः"),
        "पूर्वाभाद्रपदा" to AaradhanaInfo("अजैकपाद", "ॐ अजैकपादाय नमः"),
        "उत्तराभाद्रपदा" to AaradhanaInfo("अहिर्बुध्न्य", "ॐ अहिर्बुध्न्याय नमः"),
        "रेवती" to AaradhanaInfo("पूषा", "ॐ पूष्णे नमः")
    )

    private val yoga = mapOf(
        "विष्कंभ" to AaradhanaInfo("विष्णू", "ॐ विष्णवे नमः"),
        "प्रीति" to AaradhanaInfo("कामदेव", "ॐ कामाय नमः"),
        "आयुष्मान" to AaradhanaInfo("सूर्य", "ॐ सूर्याय नमः"),
        "सौभाग्य" to AaradhanaInfo("ब्रह्मा", "ॐ ब्रह्मणे नमः"),
        "शोभन" to AaradhanaInfo("विष्णू", "ॐ विष्णवे नमः"),
        "अतिगंड" to AaradhanaInfo("गणेश", "ॐ गं गणपतये नमः"),
        "सुकर्मा" to AaradhanaInfo("इंद्र", "ॐ इन्द्राय नमः"),
        "धृति" to AaradhanaInfo("पृथ्वी", "ॐ पृथिव्यै नमः"),
        "शूल" to AaradhanaInfo("रुद्र", "ॐ रुद्राय नमः"),
        "गंड" to AaradhanaInfo("गणेश", "ॐ गं गणपतये नमः"),
        "वृद्धि" to AaradhanaInfo("विष्णू", "ॐ विष्णवे नमः"),
        "ध्रुव" to AaradhanaInfo("ध्रुव", "ॐ ध्रुवाय नमः"),
        "व्याघात" to AaradhanaInfo("वायू", "ॐ वायवे नमः"),
        "हर्षण" to AaradhanaInfo("सूर्य", "ॐ सूर्याय नमः"),
        "वज्र" to AaradhanaInfo("इंद्र", "ॐ इन्द्राय नमः"),
        "सिद्धि" to AaradhanaInfo("गणेश", "ॐ गं गणपतये नमः"),
        "व्यतीपात" to AaradhanaInfo("रुद्र", "ॐ रुद्राय नमः"),
        "वरीयान" to AaradhanaInfo("कुबेर", "ॐ कुबेराय नमः"),
        "परिघ" to AaradhanaInfo("शनि", "ॐ शनैश्चराय नमः"),
        "शिव" to AaradhanaInfo("शिव", "ॐ नमः शिवाय"),
        "सिद्ध" to AaradhanaInfo("विष्णू", "ॐ विष्णवे नमः"),
        "साध्य" to AaradhanaInfo("साध्य देव", "ॐ साध्येभ्यो नमः"),
        "शुभ" to AaradhanaInfo("लक्ष्मी", "ॐ श्रीं महालक्ष्म्यै नमः"),
        "शुक्ल" to AaradhanaInfo("चंद्र", "ॐ सोमाय नमः"),
        "ब्रह्म" to AaradhanaInfo("ब्रह्मा", "ॐ ब्रह्मणे नमः"),
        "इंद्र" to AaradhanaInfo("इंद्र", "ॐ इन्द्राय नमः"),
        "वैधृति" to AaradhanaInfo("विष्णू", "ॐ विष्णवे नमः")
    )

    private val karana = mapOf(
        "बव" to AaradhanaInfo("इंद्र", "ॐ इन्द्राय नमः"),
        "बालव" to AaradhanaInfo("ब्रह्मा", "ॐ ब्रह्मणे नमः"),
        "कौलव" to AaradhanaInfo("मित्र", "ॐ मित्राय नमः"),
        "तैतिल" to AaradhanaInfo("अर्यमा", "ॐ अर्यम्णे नमः"),
        "गर" to AaradhanaInfo("पृथ्वी", "ॐ पृथिव्यै नमः"),
        "वणिज" to AaradhanaInfo("लक्ष्मी", "ॐ श्रीं महालक्ष्म्यै नमः"),
        "विष्टि" to AaradhanaInfo("शनि", "ॐ शनैश्चराय नमः"),
        "शकुनि" to AaradhanaInfo("कालभैरव", "ॐ कालभैरवाय नमः"),
        "चतुष्पाद" to AaradhanaInfo("रुद्र", "ॐ रुद्राय नमः"),
        "नाग" to AaradhanaInfo("नाग", "ॐ नागाय नमः"),
        "किंस्तुघ्न" to AaradhanaInfo("सूर्य", "ॐ सूर्याय नमः")
    )

    /**
     * Short daily-japa set selected for the app.
     *
     * These are the commonly published moola/samanya forms, cross-checked against
     * established Navagraha mantra collections. They are intentionally kept separate
     * from longer Vedic/tantric prayoga texts so the daily Tara Aaradhana remains
     * practical and the TTS pronunciation remains stable.
     */
    fun forPlanet(graha: Graha): AaradhanaInfo {
        val volume1 = "https://music.amazon.in/tracks/B0BYNHHQL3"
        val volume2 = "https://music.amazon.in/tracks/B0BYNLWVL3"
        return when (graha) {
            Graha.SURYA -> AaradhanaInfo("सूर्य", "ॐ घृणि सूर्याय नमः", "सूर्य सामान्य/मूल मंत्र — Drik Panchang; Navagraha collections cross-check", "ॐ घृणि सूर्याय नमः", volume1, "Exact mantra recording — Navgraha Mantra Vol. 1")
            Graha.CHANDRA -> AaradhanaInfo("चंद्र", "ॐ सोम सोमाय नमः", "चंद्र मूल मंत्र — Drik Panchang", "ॐ सोम सोमाय नमः", volume1, "Exact mantra recording — Navgraha Mantra Vol. 1")
            Graha.MANGAL -> AaradhanaInfo("मंगळ", "ॐ अं अंगारकाय नमः", "मंगळ मूल मंत्र — Drik Panchang", "ॐ अं अंगारकाय नमः", volume1, "Exact mantra recording — Navgraha Mantra Vol. 1")
            Graha.BUDH -> AaradhanaInfo("बुध", "ॐ बुं बुधाय नमः", "बुध मूल मंत्र — Drik Panchang", "ॐ बुं बुधाय नमः", volume1, "Exact mantra recording — Navgraha Mantra Vol. 1")
            Graha.GURU -> AaradhanaInfo("गुरु", "ॐ बृं बृहस्पतये नमः", "बृहस्पति मूल मंत्र — पारंपरिक Brihaspati mantra reference; cross-checked with Navagraha sources", "ॐ बृं बृहस्पतये नमः", volume1, "Exact mantra recording — Navgraha Mantra Vol. 1")
            Graha.SHUKRA -> AaradhanaInfo("शुक्र", "ॐ शुं शुक्राय नमः", "शुक्र मूल मंत्र — Drik Panchang", "ॐ शुं शुक्राय नमः", volume2, "Exact mantra recording — Navgraha Mantra Vol. 2")
            Graha.SHANI -> AaradhanaInfo("शनि", "ॐ शं शनैश्चराय नमः", "शनि मूल मंत्र — Drik Panchang", "ॐ शं शनैश्चराय नमः", volume2, "Exact mantra recording — Navgraha Mantra Vol. 2")
            Graha.RAHU -> AaradhanaInfo("राहू", "ॐ रां राहवे नमः", "राहु मूल मंत्र — Drik Panchang", "ॐ रां राहवे नमः", volume2, "Exact mantra recording — Navgraha Mantra Vol. 2")
            Graha.KETU -> AaradhanaInfo("केतू", "ॐ कें केतवे नमः", "केतु मूल मंत्र — Drik Panchang", "ॐ कें केतवे नमः", volume2, "Exact mantra recording — Navgraha Mantra Vol. 2")
        }
    }

    fun forNakshatra(name: String): AaradhanaInfo = nakshatra[name] ?: AaradhanaInfo("ईश्वर", "ॐ नमः शिवाय")
    fun forYoga(name: String): AaradhanaInfo = yoga[name] ?: AaradhanaInfo("ईश्वर", "ॐ नमः शिवाय")
    fun forKarana(name: String): AaradhanaInfo = karana[name] ?: AaradhanaInfo("ईश्वर", "ॐ नमः शिवाय")
}
