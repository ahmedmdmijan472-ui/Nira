package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.NiraDatabase
import com.example.data.model.SupportedLanguage
import com.example.data.model.ThemeMode
import com.example.data.model.VoiceGender
import com.example.data.model.VoiceSettings
import com.example.data.remote.GeminiService
import com.example.voice.NiraSpeechManager
import com.example.voice.VoiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NiraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NiraDatabase.getInstance(application)
    private val dao = db.niraDao()
    private val geminiService = GeminiService()
    val speechManager = NiraSpeechManager(application)
    val rawDbLevel: StateFlow<Float> = speechManager.rawDb
    val amplitudeHistory: StateFlow<List<Float>> = speechManager.amplitudeHistory
    val partialTranscript: StateFlow<String> = speechManager.partialTranscript

    private val prefs = application.getSharedPreferences("nira_prefs", Context.MODE_PRIVATE)

    // Voice & Settings State
    private val _voiceSettings = MutableStateFlow(loadVoiceSettings())
    val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        SupportedLanguage.findByCode(_voiceSettings.value.preferredLanguageCode)
    )
    val selectedLanguage: StateFlow<SupportedLanguage> = _selectedLanguage.asStateFlow()

    private val _detectedLanguage = MutableStateFlow(SupportedLanguage.findByCode("en"))
    val detectedLanguage: StateFlow<SupportedLanguage> = _detectedLanguage.asStateFlow()

    // Conversations Flow
    val allConversations: StateFlow<List<ConversationEntity>> = dao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversation = MutableStateFlow<ConversationEntity?>(null)
    val currentConversation: StateFlow<ConversationEntity?> = _currentConversation.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentMessages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _activeTranscript = MutableStateFlow("")
    val activeTranscript: StateFlow<String> = _activeTranscript.asStateFlow()

    private val _lastNiraReply = MutableStateFlow("")
    val lastNiraReply: StateFlow<String> = _lastNiraReply.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        speechManager.onPartialSpeechResultCallback = { partialText ->
            _activeTranscript.value = partialText
        }
        speechManager.onSpeechResultCallback = { recognizedText ->
            handleSpokenText(recognizedText)
        }
        speechManager.onSpeechErrorCallback = { errorMsg ->
            _errorMessage.value = errorMsg
        }
    }

    private fun loadVoiceSettings(): VoiceSettings {
        val genderStr = prefs.getString("voice_gender", VoiceGender.FEMALE.name) ?: VoiceGender.FEMALE.name
        val speed = prefs.getFloat("speech_speed", 1.0f)
        val pitch = prefs.getFloat("speech_pitch", 1.15f)
        val langCode = prefs.getString("lang_code", "en") ?: "en"
        val autoDetect = prefs.getBoolean("auto_detect", true)
        val themeStr = prefs.getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val notifs = prefs.getBoolean("notifs", true)
        val haptics = prefs.getBoolean("haptics", true)
        val autoSpeak = prefs.getBoolean("auto_speak", true)

        return VoiceSettings(
            voiceGender = try { VoiceGender.valueOf(genderStr) } catch (e: Exception) { VoiceGender.FEMALE },
            speechSpeed = speed,
            speechPitch = pitch,
            preferredLanguageCode = langCode,
            autoDetectLanguage = autoDetect,
            themeMode = try { ThemeMode.valueOf(themeStr) } catch (e: Exception) { ThemeMode.DARK },
            notificationsEnabled = notifs,
            hapticsEnabled = haptics,
            autoSpeakResponses = autoSpeak
        )
    }

    fun updateVoiceSettings(newSettings: VoiceSettings) {
        _voiceSettings.value = newSettings
        prefs.edit()
            .putString("voice_gender", newSettings.voiceGender.name)
            .putFloat("speech_speed", newSettings.speechSpeed)
            .putFloat("speech_pitch", newSettings.speechPitch)
            .putString("lang_code", newSettings.preferredLanguageCode)
            .putBoolean("auto_detect", newSettings.autoDetectLanguage)
            .putString("theme_mode", newSettings.themeMode.name)
            .putBoolean("notifs", newSettings.notificationsEnabled)
            .putBoolean("haptics", newSettings.hapticsEnabled)
            .putBoolean("auto_speak", newSettings.autoSpeakResponses)
            .apply()
    }

    fun setPreferredLanguage(lang: SupportedLanguage) {
        _selectedLanguage.value = lang
        _detectedLanguage.value = lang
        updateVoiceSettings(_voiceSettings.value.copy(preferredLanguageCode = lang.code))
    }

    fun setAutoDetectLanguage(enabled: Boolean) {
        updateVoiceSettings(_voiceSettings.value.copy(autoDetectLanguage = enabled))
    }

    fun previewLanguage(lang: SupportedLanguage) {
        speakText(lang.sampleGreeting, lang.code)
    }

    fun setVoiceGender(gender: VoiceGender) {
        updateVoiceSettings(_voiceSettings.value.copy(voiceGender = gender))
    }

    fun setVoiceSpeed(speed: Float) {
        updateVoiceSettings(_voiceSettings.value.copy(speechSpeed = speed))
    }

    fun setThemeMode(mode: ThemeMode) {
        updateVoiceSettings(_voiceSettings.value.copy(themeMode = mode))
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        updateVoiceSettings(_voiceSettings.value.copy(notificationsEnabled = enabled))
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadConversation(conversationId: Long) {
        viewModelScope.launch {
            val conv = dao.getConversationById(conversationId)
            _currentConversation.value = conv
            if (conv != null) {
                _selectedLanguage.value = SupportedLanguage.findByCode(conv.languageCode)
                dao.getMessagesForConversation(conv.id).collect { msgs ->
                    _currentMessages.value = msgs
                }
            }
        }
    }

    fun createNewConversation(initialTitle: String? = null, language: SupportedLanguage? = null) {
        val lang = language ?: _selectedLanguage.value
        val title = initialTitle ?: "Chat ${System.currentTimeMillis() % 10000}"
        viewModelScope.launch(Dispatchers.IO) {
            val entity = ConversationEntity(
                title = title,
                languageCode = lang.code,
                lastMessage = "",
                updatedAt = System.currentTimeMillis()
            )
            val newId = dao.insertConversation(entity)
            val created = entity.copy(id = newId)
            _currentConversation.value = created
            _currentMessages.value = emptyList()
        }
    }

    private fun ensureActiveConversation(firstMessage: String, lang: SupportedLanguage): Long {
        val current = _currentConversation.value
        if (current != null) return current.id

        val title = if (firstMessage.length > 25) firstMessage.take(25) + "..." else firstMessage
        val entity = ConversationEntity(
            title = if (title.isBlank()) "New Conversation" else title,
            languageCode = lang.code,
            lastMessage = firstMessage,
            updatedAt = System.currentTimeMillis()
        )
        val id = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            dao.insertConversation(entity)
        }
        val created = entity.copy(id = id)
        _currentConversation.value = created
        return id
    }

    private fun handleSpokenText(spokenText: String) {
        _activeTranscript.value = spokenText
        sendMessage(text = spokenText, isVoiceInput = true, speakOutput = true)
    }

    fun sendMessage(text: String, isVoiceInput: Boolean = false, speakOutput: Boolean = true) {
        if (text.isBlank()) return

        // Auto-detect language if enabled
        val targetLanguage = if (_voiceSettings.value.autoDetectLanguage) {
            val detected = SupportedLanguage.detectLanguage(text)
            _detectedLanguage.value = detected
            detected
        } else {
            _selectedLanguage.value
        }

        val convId = ensureActiveConversation(text, targetLanguage)

        viewModelScope.launch {
            // 1. Insert User Message
            val userMsg = ChatMessageEntity(
                conversationId = convId,
                sender = "user",
                content = text,
                languageDetected = targetLanguage.code,
                isVoiceMessage = isVoiceInput,
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(userMsg)
            refreshCurrentMessages(convId)

            // Update Conversation last message
            _currentConversation.value?.let { c ->
                val updated = c.copy(
                    lastMessage = text,
                    updatedAt = System.currentTimeMillis(),
                    languageCode = targetLanguage.code
                )
                _currentConversation.value = updated
                dao.updateConversation(updated)
            }

            // 2. Set State to Thinking
            _isThinking.value = true
            speechManager.setVoiceState(VoiceState.THINKING)

            // 3. Prepare History
            val historyTurns = _currentMessages.value.takeLast(6).map { msg ->
                Pair(if (msg.sender == "user") "user" else "model", msg.content)
            }

            // 4. Call Gemini AI
            val result = geminiService.generateResponse(
                prompt = text,
                history = historyTurns,
                detectedLanguage = targetLanguage
            )

            _isThinking.value = false
            val replyText = result.getOrDefault("I am here and ready to help.")
            _lastNiraReply.value = replyText

            // 5. Insert NIRA Reply
            val niraMsg = ChatMessageEntity(
                conversationId = convId,
                sender = "nira",
                content = replyText,
                languageDetected = targetLanguage.code,
                isVoiceMessage = isVoiceInput,
                timestamp = System.currentTimeMillis()
            )
            dao.insertMessage(niraMsg)
            refreshCurrentMessages(convId)

            // Update Conversation last message
            _currentConversation.value?.let { c ->
                val updated = c.copy(
                    lastMessage = replyText,
                    updatedAt = System.currentTimeMillis()
                )
                _currentConversation.value = updated
                dao.updateConversation(updated)
            }

            // 6. Speak response if requested
            if (speakOutput && _voiceSettings.value.autoSpeakResponses) {
                speechManager.speak(
                    text = replyText,
                    languageCode = targetLanguage.code,
                    settings = _voiceSettings.value
                )
            } else {
                speechManager.setVoiceState(VoiceState.IDLE)
            }
        }
    }

    fun regenerateLastMessage() {
        val msgs = _currentMessages.value
        if (msgs.isEmpty()) return
        val lastUserMsg = msgs.findLast { it.sender == "user" } ?: return

        // Delete last nira message if present
        val lastNiraMsg = msgs.findLast { it.sender == "nira" }
        viewModelScope.launch {
            if (lastNiraMsg != null && lastNiraMsg.timestamp > lastUserMsg.timestamp) {
                dao.deleteMessageById(lastNiraMsg.id)
                refreshCurrentMessages(lastUserMsg.conversationId)
            }
            sendMessage(
                text = lastUserMsg.content,
                isVoiceInput = lastUserMsg.isVoiceMessage,
                speakOutput = _voiceSettings.value.autoSpeakResponses
            )
        }
    }

    private suspend fun refreshCurrentMessages(convId: Long) {
        val list = dao.getMessagesList(convId)
        _currentMessages.value = list
    }

    fun startListening() {
        _activeTranscript.value = ""
        val lang = _selectedLanguage.value
        speechManager.startListening(lang.code)
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    fun cancelListening() {
        speechManager.cancelListening()
    }

    fun speakText(text: String, langCode: String? = null) {
        val targetCode = langCode ?: _selectedLanguage.value.code
        speechManager.speak(text, targetCode, _voiceSettings.value)
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    fun toggleMute() {
        val current = speechManager.isMuted.value
        speechManager.setMuted(!current)
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteConversationById(id)
            if (_currentConversation.value?.id == id) {
                _currentConversation.value = null
                _currentMessages.value = emptyList()
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllConversations()
            _currentConversation.value = null
            _currentMessages.value = emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
    }
}
