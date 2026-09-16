package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SupportedLanguage
import com.example.data.model.ThemeMode
import com.example.data.model.VoiceGender
import com.example.ui.components.LanguageSelectionBottomSheet
import com.example.ui.components.LanguageSettingsSection
import com.example.ui.theme.NiraCyan
import com.example.ui.theme.NiraEmerald
import com.example.ui.theme.NiraRose
import com.example.ui.viewmodel.NiraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NiraViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Language Selection Section
            item {
                SectionHeader(title = "LANGUAGE & LOCALIZATION", icon = Icons.Default.Language)

                LanguageSettingsSection(
                    viewModel = viewModel,
                    onOpenFullSelector = { showLanguageSheet = true }
                )
            }

            // Voice Assistant Configuration
            item {
                SectionHeader(title = "VOICE ASSISTANT", icon = Icons.Default.RecordVoiceOver)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Voice Gender",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VoiceGender.values().forEach { gender ->
                                val isSelected = voiceSettings.voiceGender == gender
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.setVoiceGender(gender)
                                        }
                                        .testTag("voice_gender_${gender.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (gender == VoiceGender.FEMALE) "♀ Female Voice" else "♂ Male Voice",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Voice Speed Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Speech Speed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.2f", voiceSettings.speechSpeed)}x",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = voiceSettings.speechSpeed,
                            onValueChange = { viewModel.setVoiceSpeed(it) },
                            valueRange = 0.7f..1.5f,
                            steps = 7,
                            modifier = Modifier.testTag("voice_speed_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Test Voice Button
                        Button(
                            onClick = {
                                val testPhrase = when (selectedLang.code) {
                                    "bn" -> "হ্যালো! এটি নীরা ভয়েস টেস্ট।"
                                    "hi" -> "नमस्ते! यह नीरा वॉइस टेस्ट है।"
                                    "es" -> "¡Hola! Esta es una prueba de voz de NIRA."
                                    "fr" -> "Bonjour ! Ceci est un test vocal de NIRA."
                                    else -> "Hello! This is a voice test from NIRA."
                                }
                                viewModel.speakText(testPhrase)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("test_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Preview Voice Sample")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto-speak responses switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Speak Answers",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically read AI responses aloud",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = voiceSettings.autoSpeakResponses,
                                onCheckedChange = {
                                    viewModel.updateVoiceSettings(voiceSettings.copy(autoSpeakResponses = it))
                                },
                                modifier = Modifier.testTag("auto_speak_switch")
                            )
                        }
                    }
                }
            }

            // Appearance & Theme
            item {
                SectionHeader(title = "THEME & DISPLAY", icon = Icons.Default.Brightness4)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Color Theme",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.values().forEach { mode ->
                                val isSelected = voiceSettings.themeMode == mode
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .testTag("theme_option_${mode.name}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Notifications & Data History
            item {
                SectionHeader(title = "DATA & PRIVACY", icon = Icons.Default.Security)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Notifications Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Receive proactive reminders and updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = voiceSettings.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                modifier = Modifier.testTag("notifications_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Clear Chat History
                        OutlinedButton(
                            onClick = { showClearHistoryDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NiraRose),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Clear Chat History")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Privacy Policy Info
                        TextButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("privacy_policy_button")
                        ) {
                            Text(text = "Privacy Commitment & Data Protection")
                        }
                    }
                }
            }

            // About NIRA
            item {
                SectionHeader(title = "ABOUT NIRA", icon = Icons.Default.Info)

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true }
                        .padding(bottom = 32.dp)
                        .testTag("about_nira_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, NiraCyan, CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.nira_orb),
                                contentDescription = "NIRA Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "NIRA Assistant v1.0",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Premium AI Voice Intelligence • Multilingual",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(text = "Clear All Conversations?") },
            text = { Text(text = "This will permanently erase all chat transcripts and voice sessions from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearHistoryDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history")
                ) {
                    Text(text = "Clear All", color = NiraRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(text = "Privacy & Security in NIRA") },
            text = {
                Text(
                    text = "Your privacy is paramount. Voice recognition and speech audio are processed with strict security. All your chat conversations and preferences are stored locally on your device in an encrypted Room database. NIRA never sells your personal voice or transcript data."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(text = "Understood")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = "About NIRA AI Assistant") },
            text = {
                Text(
                    text = "NIRA is an original premium AI voice assistant crafted for seamless, multilingual conversations. Supporting Bangla, English, Hindi, Arabic, Spanish, French, German, Chinese, Japanese, and more, NIRA pairs advanced natural language understanding with realistic voice synthesis and a futuristic 3D dynamic interface."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = "Close")
                }
            }
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionBottomSheet(
            viewModel = viewModel,
            onDismissRequest = { showLanguageSheet = false }
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
