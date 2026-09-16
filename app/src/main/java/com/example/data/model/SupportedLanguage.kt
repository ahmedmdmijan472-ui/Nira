package com.example.data.model

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
    val sampleGreeting: String,
    val region: String = "Global",
    val bcp47: String = "en-US"
) {
    fun matchesSearch(query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = query.trim().lowercase()
        return name.lowercase().contains(cleanQuery) ||
                nativeName.lowercase().contains(cleanQuery) ||
                code.lowercase().contains(cleanQuery) ||
                region.lowercase().contains(cleanQuery)
    }

    companion object {
        val REGIONS = listOf("All", "South Asia", "East Asia", "Middle East", "Europe", "Americas")

        val ALL = listOf(
            SupportedLanguage("en", "English", "English", "🇺🇸", "Hello! I am NIRA. How can I assist you today?", "Global", "en-US"),
            SupportedLanguage("bn", "Bangla", "বাংলা", "🇧🇩", "নমস্কার! আমি নীরা (NIRA)। আমি আপনাকে কীভাবে সাহায্য করতে পারি?", "South Asia", "bn-BD"),
            SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳", "नमस्ते! मैं नीरा (NIRA) हूँ। आज मैं आपकी क्या मदद कर सकती हूँ?", "South Asia", "hi-IN"),
            SupportedLanguage("ar", "Arabic", "العربية", "🇸🇦", "مرحباً! أنا نيرا (NIRA). كيف يمكنني مساعدتك اليوم؟", "Middle East", "ar-SA"),
            SupportedLanguage("es", "Spanish", "Español", "🇪🇸", "¡Hola! Soy NIRA. ¿En qué puedo ayudarte hoy?", "Americas", "es-ES"),
            SupportedLanguage("fr", "French", "Français", "🇫🇷", "Bonjour ! Je suis NIRA. Comment puis-je vous aider aujourd'hui ?", "Europe", "fr-FR"),
            SupportedLanguage("de", "German", "Deutsch", "🇩🇪", "Hallo! Ich bin NIRA. Wie kann ich Ihnen heute helfen?", "Europe", "de-DE"),
            SupportedLanguage("zh", "Chinese", "中文", "🇨🇳", "你好！我是 NIRA。今天有什么我可以帮你的？", "East Asia", "zh-CN"),
            SupportedLanguage("ja", "Japanese", "日本語", "🇯🇵", "こんにちは！私は NIRA です。今日はどのようなご用件でしょうか？", "East Asia", "ja-JP"),
            SupportedLanguage("ko", "Korean", "한국어", "🇰🇷", "안녕하세요! 저는 NIRA입니다. 오늘 어떤 도움이 필요하신가요?", "East Asia", "ko-KR"),
            SupportedLanguage("pt", "Portuguese", "Português", "🇧🇷", "Olá! Eu sou a NIRA. Como posso te ajudar hoje?", "Americas", "pt-BR"),
            SupportedLanguage("ru", "Russian", "Русский", "🇷🇺", "Здравствуйте! Я NIRA. Чем могу помочь вам сегодня?", "Europe", "ru-RU"),
            SupportedLanguage("it", "Italian", "Italiano", "🇮🇹", "Ciao! Sono NIRA. Come posso aiutarti oggi?", "Europe", "it-IT"),
            SupportedLanguage("tr", "Turkish", "Türkçe", "🇹🇷", "Merhaba! Ben NIRA. Bugün size nasıl yardımcı olabilirim?", "Middle East", "tr-TR"),
            SupportedLanguage("ur", "Urdu", "اردو", "🇵🇰", "السلام علیکم! میں نیرا (NIRA) ہوں۔ آج میں آپ کی کیا مدد کر سکتی ہوں؟", "South Asia", "ur-PK")
        )

        fun findByCode(code: String): SupportedLanguage {
            val prefix = code.split("-", "_").first().lowercase()
            return ALL.find { it.code.lowercase() == prefix } ?: ALL.first()
        }

        fun detectLanguage(text: String): SupportedLanguage {
            // Check for specific scripts
            val containsBangla = text.any { it in '\u0980'..'\u09FF' }
            if (containsBangla) return findByCode("bn")

            val containsDevanagari = text.any { it in '\u0900'..'\u097F' }
            if (containsDevanagari) return findByCode("hi")

            val containsArabic = text.any { it in '\u0600'..'\u06FF' }
            if (containsArabic) return findByCode("ar")

            val containsChinese = text.any { it in '\u4E00'..'\u9FFF' }
            if (containsChinese) return findByCode("zh")

            val containsJapanese = text.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' }
            if (containsJapanese) return findByCode("ja")

            val containsKorean = text.any { it in '\uAC00'..'\uD7AF' || it in '\u1100'..'\u11FF' }
            if (containsKorean) return findByCode("ko")

            // Simple common vocabulary checks for Western languages
            val lower = text.lowercase()
            if (listOf("hola", "gracias", "por favor", "¿", "buenos días", "amigo", "cómo estás").any { lower.contains(it) }) {
                return findByCode("es")
            }
            if (listOf("bonjour", "merci", "s'il vous plaît", "salut", "comment ça va").any { lower.contains(it) }) {
                return findByCode("fr")
            }
            if (listOf("hallo", "guten tag", "danke", "bitte", "wie geht").any { lower.contains(it) }) {
                return findByCode("de")
            }
            if (listOf("ciao", "grazie", "buongiorno", "per favore").any { lower.contains(it) }) {
                return findByCode("it")
            }
            if (listOf("olá", "obrigado", "obrigada", "bom dia").any { lower.contains(it) }) {
                return findByCode("pt")
            }

            return findByCode("en")
        }
    }
}
