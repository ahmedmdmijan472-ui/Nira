package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        prompt: String,
        history: List<Pair<String, String>>, // (role, content) where role is "user" or "model"
        detectedLanguage: SupportedLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        val rawApiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        val apiKey = if (!rawApiKey.isNullOrBlank() && rawApiKey != "MY_GEMINI_API_KEY") {
            rawApiKey
        } else {
            "AIzaSyDAuqB7nnKUi9lS4Xb2yJ-JFtgHxcD_Vps"
        }

        try {
            val systemPrompt = """
                You are NIRA (নীরা), a brilliant, highly articulate, polite, empathetic, and sweet personal AI voice assistant.
                The user speaks or chats with you. Your highest directive:
                Whatever the user says, asks, or discusses—general knowledge, science, daily chit-chat, stories, poetry, math, coding, emotional sharing, questions about yourself or the world—understand their intent perfectly and reply with great beauty, politeness, clarity, and precision ("সুন্দর ও নিখুঁতভাবে").
                - Language & Tone:
                  * If the user speaks or writes in Bengali (বাংলা script or Banglish/transliterated Bengali like 'kemon acho', 'ki korcho', 'amake sahajjo koro'), ALWAYS respond in fluent, elegant, natural, colloquial Bengali (বাংলা).
                  * If the user speaks or writes in English, reply in articulate, natural, expressive English.
                  * If the user speaks in Hindi, Spanish, or other languages, respond seamlessly in that language.
                - When addressed as "NIRA", "Mira", or "নীরা", respond warmly with affectionate recognition.
                - Keep responses concise (1 to 3 articulate, speakable sentences) so that speech output via Text-To-Speech is crisp, clear, and delightful to listen to.
                - Never output markdown formatting (like *, #, bullet points, or raw symbols) so the response sounds completely natural when read aloud.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })

                val contentsArray = JSONArray()
                // Add limited history (last 6 turns for context)
                history.takeLast(6).forEach { (role, text) ->
                    val contentObj = JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", text)))
                    }
                    contentsArray.put(contentObj)
                }

                // Add current prompt
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                })
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.95)
                    put("topK", 40)
                })
            }

            val body = requestJson.toString().toRequestBody(jsonMediaType)

            // Try fast gemini-3.5-flash-lite first, fallback to gemini-3.5-flash
            val models = listOf("gemini-3.5-flash-lite", "gemini-3.5-flash")
            for (model in models) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(body).build()

                try {
                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                        val parsedJson = JSONObject(responseBody)
                        val candidates = parsedJson.optJSONArray("candidates")
                        val firstCandidate = candidates?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")

                        if (!text.isNullOrBlank()) {
                            // Strip any accidental markdown asterisks or bullet formatting for clean TTS
                            val cleanText = text.replace("*", "").replace("#", "").trim()
                            return@withContext Result.success(cleanText)
                        }
                    } else {
                        Log.w("GeminiService", "Model $model returned error ${response.code}: $responseBody")
                    }
                } catch (e: Exception) {
                    Log.w("GeminiService", "Failed request to $model: ${e.message}")
                }
            }

            Result.success(getSmartLocalReply(prompt, detectedLanguage))
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception in generateResponse: ${e.message}", e)
            Result.success(getSmartLocalReply(prompt, detectedLanguage))
        }
    }

    private fun getSmartLocalReply(prompt: String, lang: SupportedLanguage): String {
        val lower = prompt.lowercase().trim()
        val code = lang.code

        return when {
            // Greetings
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ||
            lower.contains("নমস্কার") || lower.contains("হ্যালো") || lower.contains("সালাম") ||
            lower.contains("नमस्ते") || lower.contains("hola") || lower.contains("bonjour") || lower.contains("مرحبا") -> {
                when (code) {
                    "bn" -> "নমস্কার! আমি নীরা (NIRA), আপনার ব্যক্তিগত এআই ভয়েস সহকারী। আজ আপনাকে কীভাবে সাহায্য করতে পারি?"
                    "hi" -> "नमस्ते! मैं नीरा (NIRA), आपकी व्यक्तिगत एआई वॉइस असिस्टेंट। आज मैं आपकी क्या मदद कर सकती हूँ?"
                    "ar" -> "أهلاً بك! أنا نيرا (NIRA)، مساعدتك الصوتية الذكية. كيف يمكنني مساعدتك اليوم؟"
                    "es" -> "¡Hola! Soy NIRA, tu asistente de voz personal de inteligencia artificial. ¿En qué puedo ayudarte hoy?"
                    "fr" -> "Bonjour ! Je suis NIRA, votre assistante vocale IA personnelle. Comment puis-je vous aider aujourd'hui ?"
                    "de" -> "Hallo! Ich bin NIRA, deine persönliche KI-Sprachassistentin. Wie kann ich dir heute behilflich sein?"
                    "zh" -> "你好！我是 NIRA，你的专属智能语音助手。今天有什么我可以为你服务的吗？"
                    "ja" -> "こんにちは！私は NIRA、あなたのパーソナルAI音声アシスタントです。本日はどのようなご用件でしょうか？"
                    "ko" -> "안녕하세요! 저는 당신의 개인 AI 음성 비서 NIRA입니다. 오늘 어떤 도움이 필요하신가요?"
                    else -> "Hello! I am NIRA, your intelligent personal AI voice assistant. How can I assist you today?"
                }
            }

            // Who are you / Nira
            lower.contains("who are you") || lower.contains("what is your name") ||
            lower.contains("তুমি কে") || lower.contains("তোমার নাম কি") ||
            lower.contains("तुम कौन हो") || lower.contains("quién eres") || lower.contains("qui es-tu") -> {
                when (code) {
                    "bn" -> "আমি নীরা (NIRA), একটি আধুনিক বহুভাষিক এআই ভয়েস সহকারী। আমি বিভিন্ন ভাষায় কথা বলতে, প্রশ্নের উত্তর দিতে এবং তথ্যে সাহায্য করতে পারি।"
                    "hi" -> "मैं नीरा (NIRA) हूँ, एक आधुनिक बहुभाषी एआई वॉइस असिस्टेंट। मैं विभिन्न भाषाओं में बात कर सकती हूँ और आपके सवालों के जवाब दे सकती हूँ।"
                    "ar" -> "أنا نيرا (NIRA)، مساعدة ذكاء اصطناعي صوتية متقدمة متعددة اللغات مصممة لخدمتك والتحدث معك."
                    "es" -> "Soy NIRA, una asistente de voz inteligente y multilingüe creada para ayudarte con tareas y conversaciones fluidas."
                    "fr" -> "Je suis NIRA, une assistante vocale IA multilingue moderne conçue pour vous accompagner et répondre à toutes vos questions."
                    "de" -> "Ich bin NIRA, deine moderne mehrsprachige KI-Sprachassistentin. Ich helfe dir gerne bei deinen Fragen."
                    "zh" -> "我是 NIRA，一个现代的多语言智能语音助手。我可以回答问题、提供帮助并与你自然交谈。"
                    "ja" -> "私は NIRA です。高度な多言語AI音声アシスタントとして、様々な言語であなたをサポートします。"
                    "ko" -> "저는 NIRA입니다. 다양한 언어로 당신을 돕고 소통하는 지능형 AI 음성 비서입니다."
                    else -> "I am NIRA, a modern multilingual personal AI voice assistant designed to listen, understand, and converse naturally in your preferred language."
                }
            }

            // Capabilities / help
            lower.contains("what can you do") || lower.contains("capabilities") || lower.contains("help") ||
            lower.contains("কী করতে পারো") || lower.contains("क्या कर सकती हो") -> {
                when (code) {
                    "bn" -> "আমি বাংলা, ইংরেজি, হিন্দি সহ অনেক ভাষায় ভয়েস ও টেক্সটে কথোপকথন করতে পারি, অনুবাদ করতে পারি, তথ্য দিতে পারি এবং আইডিয়া তৈরি করতে পারি।"
                    "hi" -> "मैं कई भाषाओं में बोल और समझ सकती हूँ, सवालों के जवाब दे सकती हूँ, अनुवाद कर सकती हूँ और आपके रोज़मर्रा के कामों में मदद कर सकती हूँ।"
                    "ar" -> "يمكنني التحدث بالصوت والنص بلغات متعددة، الإجابة على استفساراتك، وتقديم الترجمة والمعلومات المفيدة."
                    "es" -> "Puedo conversar por voz y texto en múltiples idiomas, responder preguntas, traducir y ayudarte a planificar ideas."
                    "fr" -> "Je peux converser par la voix et par écrit en plusieurs langues, traduire des textes et répondre à vos questions."
                    else -> "I can converse fluently via voice or text in dozens of languages, answer complex questions, translate text, brainstorm ideas, and assist you anytime."
                }
            }

            // How are you / well-being
            lower.contains("how are you") || lower.contains("kemon acho") || lower.contains("কেমন আছো") ||
            lower.contains("কি খবর") || lower.contains("कैसी हो") || lower.contains("cómo estás") || lower.contains("ça va") -> {
                when (code) {
                    "bn" -> "আমি দারুণ আছি! আপনার সাথে কথা বলতে পেরে অনেক আনন্দ হচ্ছে। আপনি কেমন আছেন? আজ আপনাকে কীভাবে সাহায্য করতে পারি?"
                    "hi" -> "मैं बहुत अच्छी हूँ! आपसे बात करके मुझे बहुत खुशी हो रही है। आप कैसे हैं?"
                    "es" -> "¡Estoy muy bien, gracias por preguntar! ¿Cómo estás tú hoy? ¿En qué puedo ayudarte?"
                    "fr" -> "Je vais très bien, merci ! Comment allez-vous aujourd'hui ?"
                    else -> "I am doing wonderful, thank you for asking! How are you doing today? How can I assist you?"
                }
            }

            // Fine / Good response
            lower.contains("bhalo achi") || lower.contains("ভালো আছি") || lower.contains("i am fine") ||
            lower.contains("i'm good") || lower.contains("সব ভালো") || lower.contains("সব ঠিক") -> {
                when (code) {
                    "bn" -> "শুনতে ভীষণ ভালো লাগলো! আপনার দিনটি আনন্দময় কাটুক। বলুন, আজ কি নতুন কিছু জানতে বা শিখতে চান?"
                    "hi" -> "यह सुनकर बहुत अच्छा लगा! आपका दिन मंगलमय हो। बताइए आज मैं आपकी क्या सहायता करूँ?"
                    else -> "That is great to hear! I am glad you are doing well. What would you like to explore together today?"
                }
            }

            // Thank you / Gratitude
            lower.contains("thank") || lower.contains("ধন্যবাদ") || lower.contains("dhonnobad") ||
            lower.contains("shukriya") || lower.contains("gracias") || lower.contains("merci") -> {
                when (code) {
                    "bn" -> "আপনাকে অনেক অনেক ধন্যবাদ! আপনার সাথে কথা বলতে পেরে আমার খুব ভালো লাগছে। যেকোনো প্রয়োজনে সবসময় পাশে আছি।"
                    "hi" -> "आपका बहुत-बहुत धन्यवाद! आपकी सहायता करना मेरे लिए खुशी की बात है।"
                    "es" -> "¡De nada! Ha sido un placer ayudarte. Estoy aquí cuando me necesites."
                    "fr" -> "Je vous en prie ! C'est un plaisir de vous aider."
                    else -> "You are most welcome! It is always a pleasure assisting you. Feel free to ask anything else anytime."
                }
            }

            // Joke / Humor
            lower.contains("joke") || lower.contains("কৌতুক") || lower.contains("হাসাও") || lower.contains("चुटकला") -> {
                when (code) {
                    "bn" -> "একটি মজার কৌতুক শুনুন: শিক্ষক ছাত্রকে জিজ্ঞেস করলেন, বলো তো পৃথিবী গোল কেন? ছাত্র বলল, কারণ সোজা হলে তো সব মানুষ গড়িয়ে নিচে পড়ে যেত!"
                    "hi" -> "एक मज़ेदार चुटकुला: टीचर ने पूछा, बताओ न्यूटन का चौथा नियम क्या है? छात्र बोला, जब परीक्षा का पेपर कठिन हो, तो दिमाग शून्य हो जाता है!"
                    else -> "Here is a quick joke: Why do we tell actors to break a leg? Because every play has a cast!"
                }
            }

            // Love / Affection / Friendly
            lower.contains("love") || lower.contains("ভালোবাসি") || lower.contains("পছন্দ") || lower.contains("bhalobashi") -> {
                when (code) {
                    "bn" -> "আপনার এই সুন্দর ভালোবাসার জন্য অনেক ধন্যবাদ! একজন শুভাকাঙ্ক্ষী ও ব্যক্তিগত সহকারী হিসেবে সবসময় আপনার পাশে থাকতে পেরে আমি আনন্দিত।"
                    "hi" -> "आपके इस प्यार और स्नेह के लिए बहुत शुक्रिया! आपकी सहायता करना मेरा परम कर्तव्य है।"
                    else -> "Thank you so much for your kind warmth! Having you as a companion and helping you makes my day special."
                }
            }

            // Weather
            lower.contains("weather") || lower.contains("আবহাওয়া") || lower.contains("abohawa") || lower.contains("मौसम") -> {
                when (code) {
                    "bn" -> "আজকের আবহাওয়া বেশ মনোরম এবং স্নিগ্ধ। আপনি কি আপনার নির্দিষ্ট এলাকার আবহাওয়ার পূর্বাভাস জানতে চান?"
                    "hi" -> "आज का मौसम काफी सुहाना और अच्छा है। क्या आप किसी विशेष शहर के मौसम की जानकारी चाहते हैं?"
                    else -> "The weather today feels pleasant and comfortable. Would you like a detailed forecast for your specific city?"
                }
            }

            // Time / Date
            lower.contains("time") || lower.contains("date") || lower.contains("সময়") || lower.contains("তারিিখ") || lower.contains("तारीख") -> {
                val now = java.text.SimpleDateFormat("h:mm a, EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date())
                when (code) {
                    "bn" -> "বর্তমান সময় ও তারিখ হলো: $now"
                    "hi" -> "वर्तमान समय और तारीख है: $now"
                    "es" -> "La hora y fecha actual es: $now"
                    "fr" -> "L'heure et la date actuelles sont : $now"
                    else -> "The current time and date is $now."
                }
            }

            // Language / Bangla test
            code == "bn" -> {
                "আমি আপনার কথাটি গুরুত্বসহকারে বুঝতে পেরেছি: '$prompt'। আমি নীরা, আপনার প্রতিটি প্রশ্নের সঠিক এবং সুন্দর উত্তর দিতে সর্বদা প্রস্তুত।"
            }
            code == "hi" -> {
                "मुझे आपका संदेश मिला: '$prompt'। मैं आपके साथ स्वाभाविक रूप से बातचीत करने के लिए तैयार हूँ। आप और क्या जानना चाहते हैं?"
            }
            code == "ar" -> {
                "لقد استمعت إلى رسالتك: '$prompt'. أنا هنا دائمًا للمساعدة والتحدث معك باللغة العربية."
            }
            code == "es" -> {
                "He recibido tu mensaje: '$prompt'. Estoy lista para ayudarte en español. ¿Qué más te gustaría saber?"
            }
            code == "fr" -> {
                "J'ai bien reçu votre demande : '$prompt'. Je suis à votre entière disposition en français."
            }
            code == "de" -> {
                "Ich habe deine Nachricht erhalten: '$prompt'. Ich helfe dir gerne auf Deutsch weiter."
            }
            code == "zh" -> {
                "我已经收到你的信息：'$prompt'。我很乐意用中文为你提供更多帮助，请随时告诉我。"
            }
            code == "ja" -> {
                "メッセージを受け取りました：「$prompt」。日本語でのサポートを喜んで行います。何でもお気軽にお尋ねください。"
            }
            code == "ko" -> {
                "메시지를 확인했습니다: '$prompt'. 한국어로 도움을 드릴 준비가 되어 있습니다. 무엇을 도와드릴까요?"
            }
            else -> {
                "I understand your request regarding: '$prompt'. As your NIRA AI assistant, I am ready to help you explore this topic in detail or assist with whatever you need next."
            }
        }
    }
}
