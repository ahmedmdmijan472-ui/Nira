package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class NiraSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _rawDb = MutableStateFlow(-2f)
    val rawDb: StateFlow<Float> = _rawDb.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(List(32) { 0.05f })
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    var onSpeechResultCallback: ((String) -> Unit)? = null
    var onPartialSpeechResultCallback: ((String) -> Unit)? = null
    var onSpeechErrorCallback: ((String) -> Unit)? = null
    var onSpeakingCompleteCallback: (() -> Unit)? = null

    init {
        mainHandler.post {
            initTts()
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("NiraSpeechManager", "Error initializing TTS: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    if (_voiceState.value == VoiceState.SPEAKING) {
                        _voiceState.value = VoiceState.IDLE
                    }
                    mainHandler.post {
                        onSpeakingCompleteCallback?.invoke()
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (_voiceState.value == VoiceState.SPEAKING) {
                        _voiceState.value = VoiceState.IDLE
                    }
                }
            })
        } else {
            isTtsReady = false
            Log.e("NiraSpeechManager", "TTS initialization failed status: $status")
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            stopSpeaking()
        }
    }

    fun setVoiceState(state: VoiceState) {
        _voiceState.value = state
    }

    fun speak(
        text: String,
        languageCode: String,
        settings: VoiceSettings,
        onComplete: (() -> Unit)? = null
    ) {
        if (_isMuted.value) {
            onComplete?.invoke()
            return
        }

        if (!isTtsReady || tts == null) {
            Log.w("NiraSpeechManager", "TTS is not ready yet")
            return
        }

        // Configure Locale
        val locale = getLocaleForCode(languageCode)
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("NiraSpeechManager", "Language $languageCode not fully supported in TTS, trying default")
            tts?.language = Locale.getDefault()
        }

        // Pitch & Speech Rate
        var pitch = settings.speechPitch
        if (settings.voiceGender == VoiceGender.MALE) {
            pitch = (settings.speechPitch * 0.78f).coerceIn(0.7f, 1.1f)
        } else {
            pitch = (settings.speechPitch * 1.12f).coerceIn(0.9f, 1.4f)
        }
        tts?.setPitch(pitch)
        tts?.setSpeechRate(settings.speechSpeed)

        // Try selecting male/female voice from system voices if available
        try {
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val targetNameKeyword = if (settings.voiceGender == VoiceGender.MALE) "male" else "female"
                val matchedVoice: Voice? = availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    nameLower.contains(targetNameKeyword) && (voice.locale.language == locale.language || locale.language.isEmpty())
                } ?: availableVoices.firstOrNull { voice ->
                    val nameLower = voice.name.lowercase()
                    nameLower.contains(targetNameKeyword)
                }

                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            Log.d("NiraSpeechManager", "Selecting system voice: ${e.message}")
        }

        val utteranceId = "nira_utt_${System.currentTimeMillis()}"
        _voiceState.value = VoiceState.SPEAKING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("NiraSpeechManager", "Error stopping TTS: ${e.message}")
        }
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun startListening(languageCode: String) {
        stopSpeaking()

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.e("NiraSpeechManager", "SpeechRecognizer not available on device")
                    onSpeechErrorCallback?.invoke("Speech recognition is not available on this device.")
                    _voiceState.value = VoiceState.IDLE
                    return@post
                }

                _partialTranscript.value = ""

                // Ensure clean recognizer instance to prevent ERROR_RECOGNIZER_BUSY
                try {
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    Log.w("NiraSpeechManager", "Error cleaning prior recognizer: ${e.message}")
                }
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize rmsdB (typically -2 to 10 dB) to 0.0 .. 1.0
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsLevel.value = normalized
                        _rawDb.value = rmsdB
                        val current = _amplitudeHistory.value
                        _amplitudeHistory.value = current.drop(1) + normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _rmsLevel.value = 0f
                        _rawDb.value = -2f
                        _voiceState.value = VoiceState.THINKING
                    }

                    override fun onError(error: Int) {
                        _rmsLevel.value = 0f
                        _rawDb.value = -2f
                        val lastHeard = _partialTranscript.value.trim()
                        _partialTranscript.value = ""

                        // Destroy instance on error to prevent stuck state
                        try {
                            speechRecognizer?.destroy()
                            speechRecognizer = null
                        } catch (e: Exception) {
                            Log.w("NiraSpeechManager", "Error resetting recognizer after error: ${e.message}")
                        }

                        // Seamless recovery: if speech was captured partially before timeout/no_match, process it!
                        if (lastHeard.isNotBlank() && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                            Log.d("NiraSpeechManager", "Recovered speech input from partial transcript on error $error: $lastHeard")
                            _voiceState.value = VoiceState.THINKING
                            onSpeechResultCallback?.invoke(lastHeard)
                            return
                        }

                        _voiceState.value = VoiceState.IDLE

                        // Avoid alarming error messages on natural pauses or timeouts
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            Log.d("NiraSpeechManager", "Natural silence / no input detected ($error)")
                            return
                        }

                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check microphone."
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                            SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network timeout."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again."
                            SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
                            else -> "Speech recognition error ($error)"
                        }
                        Log.d("NiraSpeechManager", "Speech recognition error: $errorMessage")
                        onSpeechErrorCallback?.invoke(errorMessage)
                    }

                    override fun onResults(results: Bundle?) {
                        _rmsLevel.value = 0f
                        _rawDb.value = -2f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull { it.isNotBlank() }?.trim()
                            ?: _partialTranscript.value.trim()
                        _partialTranscript.value = ""

                        if (!text.isNullOrBlank()) {
                            _voiceState.value = VoiceState.THINKING
                            onSpeechResultCallback?.invoke(text)
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull { it.isNotBlank() }?.trim()
                        if (!text.isNullOrBlank()) {
                            _partialTranscript.value = text
                            onPartialSpeechResultCallback?.invoke(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val tag = getLanguageTag(languageCode)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 100L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                    putExtra("android.speech.extra.DICTATION_MODE", true)
                }

                speechRecognizer?.startListening(intent)
                _voiceState.value = VoiceState.LISTENING
            } catch (e: Exception) {
                Log.e("NiraSpeechManager", "Failed to start listening: ${e.message}")
                _voiceState.value = VoiceState.IDLE
                _partialTranscript.value = ""
                onSpeechErrorCallback?.invoke("Could not access speech recognizer: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("NiraSpeechManager", "Error stopping listening: ${e.message}")
            }
            _rmsLevel.value = 0f
            _rawDb.value = -2f
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e("NiraSpeechManager", "Error cancelling listening: ${e.message}")
            }
            _rmsLevel.value = 0f
            _rawDb.value = -2f
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    private fun getLocaleForCode(code: String): Locale {
        return when (code.lowercase()) {
            "bn" -> Locale("bn", "BD")
            "hi" -> Locale("hi", "IN")
            "ar" -> Locale("ar", "SA")
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "zh" -> Locale.CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "pt" -> Locale("pt", "BR")
            "ru" -> Locale("ru", "RU")
            "it" -> Locale.ITALIAN
            "tr" -> Locale("tr", "TR")
            "ur" -> Locale("ur", "PK")
            else -> Locale.US
        }
    }

    private fun getLanguageTag(code: String): String {
        return when (code.lowercase()) {
            "bn" -> "bn-BD"
            "hi" -> "hi-IN"
            "ar" -> "ar-SA"
            "es" -> "es-ES"
            "fr" -> "fr-FR"
            "de" -> "de-DE"
            "zh" -> "zh-CN"
            "ja" -> "ja-JP"
            "ko" -> "ko-KR"
            "pt" -> "pt-BR"
            "ru" -> "ru-RU"
            "it" -> "it-IT"
            "tr" -> "tr-TR"
            "ur" -> "ur-PK"
            else -> "en-US"
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("NiraSpeechManager", "Error releasing TTS: ${e.message}")
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("NiraSpeechManager", "Error destroying SpeechRecognizer: ${e.message}")
        }
    }
}
