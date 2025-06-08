package com.example.japaneselearning.utils

class KanjiToKanaConverter {
    
    // Common kanji to hiragana mappings
    private val kanjiToKana = mapOf(
        // Time and dates
        "今日" to "きょう",
        "明日" to "あした",
        "昨日" to "きのう",
        "今" to "いま",
        "時間" to "じかん",
        "朝" to "あさ",
        "夜" to "よる",
        "昼" to "ひる",
        
        // Weather
        "天気" to "てんき",
        "雨" to "あめ",
        "雪" to "ゆき",
        "風" to "かぜ",
        "暑い" to "あつい",
        "寒い" to "さむい",
        "涼しい" to "すずしい",
        "暖かい" to "あたたかい",

        // Basic adjectives
        "良い" to "いい",
        "悪い" to "わるい",
        "新しい" to "あたらしい",
        "古い" to "ふるい",
        "大きい" to "おおきい",
        "小さい" to "ちいさい",
        "高い" to "たかい",
        "安い" to "やすい",
        "美しい" to "うつくしい",
        "幸" to "しあわ",
        
        // People and family
        "人" to "ひと",
        "友達" to "ともだち",
        "家族" to "かぞく",
        "母" to "はは",
        "父" to "ちち",
        "兄" to "あに",
        "姉" to "あね",
        "弟" to "おとうと",
        "妹" to "いもうと",
        "子供" to "こども",
        
        // Places
        "家" to "いえ",
        "学校" to "がっこう",
        "会社" to "かいしゃ",
        "病院" to "びょういん",
        "駅" to "えき",
        "空港" to "くうこう",
        "店" to "みせ",
        "銀行" to "ぎんこう",
        "図書館" to "としょかん",
        "公園" to "こうえん",
        
        // Food
        "食べ物" to "たべもの",
        "水" to "みず",
        "お茶" to "おちゃ",
        "米" to "こめ",
        "魚" to "さかな",
        "肉" to "にく",
        "野菜" to "やさい",
        "果物" to "くだもの",
        
        // Actions and verbs (common forms)
        "行く" to "いく",
        "来る" to "くる",
        "見る" to "みる",
        "聞く" to "きく",
        "話す" to "はなす",
        "読む" to "よむ",
        "書く" to "かく",
        "食べる" to "たべる",
        "飲む" to "のむ",
        "寝る" to "ねる",
        "起きる" to "おきる",
        "働く" to "はたらく",
        "勉強" to "べんきょう",
        "運動" to "うんどう",
        
        // Numbers
        "一" to "いち",
        "二" to "に",
        "三" to "さん",
        "四" to "よん",
        "五" to "ご",
        "六" to "ろく",
        "七" to "なな",
        "八" to "はち",
        "九" to "きゅう",
        "十" to "じゅう",
        "百" to "ひゃく",
        "千" to "せん",
        "万" to "まん",
        
        // Common expressions
        "元気" to "げんき",
        "大丈夫" to "だいじょうぶ",
        "本当" to "ほんとう",
        "大切" to "たいせつ",
        "簡単" to "かんたん",
        "難しい" to "むずかしい",
        "楽しい" to "たのしい",
        "面白い" to "おもしろい",
        "忙しい" to "いそがしい",
        
        // Countries
        "日本" to "にほん",
        "中国" to "ちゅうごく",
        "韓国" to "かんこく",
        "アメリカ" to "あめりか",
        "イギリス" to "いぎりす",
        "フランス" to "ふらんす",
        "ドイツ" to "どいつ",
        
        // Transportation
        "車" to "くるま",
        "電車" to "でんしゃ",
        "飛行機" to "ひこうき",
        "自転車" to "じてんしゃ",
        "バス" to "ばす",
        "船" to "ふね",
        
        // Common sentence components
        "私" to "わたし",
        "僕" to "ぼく",
        "君" to "きみ",
        "彼" to "かれ",
        "彼女" to "かのじょ",
        "先生" to "せんせい",
        "学生" to "がくせい",
        "年" to "とし",
        "月" to "つき",
        "日" to "ひ",
        "週間" to "しゅうかん",
        
        // Common adjective endings
        "です" to "です",
        "ます" to "ます",
        "でした" to "でした",
        "ました" to "ました",
        "ません" to "ません",
        "ですね" to "ですね",
        "ですか" to "ですか"
    )
    
    /**
     * Convert Japanese text with kanji to kana-only text
     * This is a simple implementation using HashMap lookup
     */
    fun convertToKana(japaneseWithKanji: String): String {
        var result = japaneseWithKanji
        
        // Sort by length (longest first) to handle compound words correctly
        val sortedEntries = kanjiToKana.entries.sortedByDescending { it.key.length }
        
        for ((kanji, kana) in sortedEntries) {
            result = result.replace(kanji, kana)
        }
        
        return result
    }
    
    /**
     * Check if text contains kanji characters
     */
    fun containsKanji(text: String): Boolean {
        return text.any { char ->
            // Kanji Unicode ranges
            char.code in 0x4E00..0x9FAF || // CJK Unified Ideographs
            char.code in 0x3400..0x4DBF || // CJK Extension A
            char.code in 0x20000..0x2A6DF  // CJK Extension B
        }
    }
    
    /**
     * Get conversion coverage percentage
     */
    fun getConversionCoverage(text: String): Float {
        val originalKanjiCount = text.count { containsKanji(it.toString()) }
        if (originalKanjiCount == 0) return 100f
        
        val converted = convertToKana(text)
        val remainingKanjiCount = converted.count { containsKanji(it.toString()) }
        
        return ((originalKanjiCount - remainingKanjiCount).toFloat() / originalKanjiCount) * 100f
    }
    
    /**
     * Add new kanji-kana mapping (for user customization)
     */
    fun addMapping(kanji: String, kana: String) {
        // In a real app, you'd save this to SharedPreferences or database
        // For now, this is just a placeholder for the concept
    }
}