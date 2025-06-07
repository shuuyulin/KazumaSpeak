package com.example.japaneselearning.utils

class SimpleJapaneseRomanizer {

    // Hiragana to Romaji mapping
    private val hiraganaToRomaji = mapOf(
        // Basic vowels
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",

        // K sounds
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",

        // S sounds
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",

        // T sounds
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "だ" to "da", "ぢ" to "di", "づ" to "du", "で" to "de", "ど" to "do",

        // N sounds
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",

        // H sounds
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",

        // M sounds
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",

        // Y sounds
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",

        // R sounds
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",

        // W sounds
        "わ" to "wa", "ゐ" to "wi", "ゑ" to "we", "を" to "wo",

        // N
        "ん" to "n",

        // Combinations (ya, yu, yo)
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",

        // Special
        "ー" to "", "っ" to "", "ッ" to ""
    )

    // Common Japanese phrases with known romanizations
    private val commonPhrases = mapOf(
        "こんにちは" to "konnichiwa",
        "ありがとう" to "arigatou",
        "ありがとうございます" to "arigatou gozaimasu",
        "おはよう" to "ohayou",
        "おはようございます" to "ohayou gozaimasu",
        "すみません" to "sumimasen",
        "はじめまして" to "hajimemashite",
        "げんきですか" to "genki desu ka",
        "きょうはいいてんきですね" to "kyou wa ii tenki desu ne",
        "いただきます" to "itadakimasu",
        "ごちそうさまでした" to "gochisousama deshita",
        "おやすみなさい" to "oyasumi nasai",
        "さようなら" to "sayounara"
    )

    fun romanize(kanaText: String): String {
        // First check if it's a common phrase
        val cleanText = kanaText.replace(" ", "").replace("　", "")
        val commonResult = commonPhrases[cleanText]
        if (commonResult != null) {
            return commonResult
        }

        // Character by character romanization
        val result = StringBuilder()
        var i = 0

        while (i < kanaText.length) {
            var processed = false

            // Try 3-character combinations first (rare)
            if (i <= kanaText.length - 3) {
                val threeChar = kanaText.substring(i, i + 3)
                val threeCharResult = hiraganaToRomaji[threeChar]
                if (threeCharResult != null) {
                    result.append(threeCharResult)
                    i += 3
                    processed = true
                }
            }

            // Try 2-character combinations if not processed
            if (!processed && i <= kanaText.length - 2) {
                val twoChar = kanaText.substring(i, i + 2)
                val twoCharResult = hiraganaToRomaji[twoChar]
                if (twoCharResult != null) {
                    result.append(twoCharResult)
                    i += 2
                    processed = true
                } else {
                    // Handle small tsu (っ) - double next consonant
                    if (twoChar[0] == 'っ') {
                        val nextChar = twoChar[1].toString()
                        val nextRomaji = hiraganaToRomaji[nextChar]
                        if (nextRomaji != null && nextRomaji.isNotEmpty()) {
                            result.append(nextRomaji[0]) // Double the first consonant
                            result.append(nextRomaji)
                            i += 2
                            processed = true
                        }
                    }
                }
            }

            // Single character if not processed
            if (!processed) {
                val char = kanaText[i].toString()
                val romaji = hiraganaToRomaji[char]
                if (romaji != null) {
                    result.append(romaji)
                } else {
                    // Keep spaces and other characters
                    if (char == " " || char == "　") {
                        result.append(" ")
                    } else {
                        result.append(char)
                    }
                }
                i++
            }
        }

        return result.toString().trim()
    }

    // Convert katakana to hiragana for romanization
    fun katakanaToHiragana(katakana: String): String {
        val result = StringBuilder()
        for (char in katakana) {
            val code = char.code
            if (code in 0x30A1..0x30F6) {
                // Convert katakana to hiragana
                result.append((code - 0x60).toChar())
            } else {
                result.append(char)
            }
        }
        return result.toString()
    }

    // Helper method to convert any Japanese text to kana for romanization
    fun prepareForRomanization(text: String): String {
        // Convert katakana to hiragana, keep hiragana as is
        // This is a simple approach - in a real app you might want more sophisticated conversion
        return katakanaToHiragana(text)
    }
}