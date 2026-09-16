package com.example.data.model

data class VoiceSettings(
    val voiceGender: VoiceGender = VoiceGender.FEMALE,
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.15f,
    val preferredLanguageCode: String = "en",
    val autoDetectLanguage: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val autoSpeakResponses: Boolean = true
)

enum class VoiceGender(val label: String) {
    FEMALE("Female Voice"),
    MALE("Male Voice")
}

enum class ThemeMode(val label: String) {
    DARK("Dark Cyber"),
    LIGHT("Clean Light"),
    SYSTEM("System Default")
}
