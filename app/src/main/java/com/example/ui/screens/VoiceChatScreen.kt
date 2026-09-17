package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceGender
import com.example.ui.components.AnimatedMicButton
import com.example.ui.components.LanguageSelectionBottomSheet
import com.example.ui.components.NiraOrbAvatar
import com.example.ui.components.RealtimeWaveformCard
import com.example.ui.components.VoiceVisualizer
import com.example.ui.components.WaveformStyle
import com.example.ui.theme.NiraCyan
import com.example.ui.theme.NiraEmerald
import com.example.ui.theme.NiraRose
import com.example.ui.theme.NiraViolet
import com.example.ui.viewmodel.NiraViewModel
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    viewModel: NiraViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTextChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceState by viewModel.speechManager.voiceState.collectAsState()
    val rmsLevel by viewModel.speechManager.rmsLevel.collectAsState()
    val rawDb by viewModel.rawDbLevel.collectAsState()
    val isMuted by viewModel.speechManager.isMuted.collectAsState()
    var currentWaveformStyle by remember { mutableStateOf(WaveformStyle.FLUID_WAVE) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val detectedLang by viewModel.detectedLanguage.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    val transcript by viewModel.activeTranscript.collectAsState()
    val partialTranscript by viewModel.partialTranscript.collectAsState()
    val lastReply by viewModel.lastNiraReply.collectAsState()
    val currentMessages by viewModel.currentMessages.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    var isHandsFreeActive by remember { mutableStateOf(true) }
    var previousVoiceState by remember { mutableStateOf(voiceState) }

    // When NIRA finishes speaking in hands-free mode, seamlessly listen for next user utterance
    LaunchedEffect(voiceState) {
        if (previousVoiceState == VoiceState.SPEAKING && voiceState == VoiceState.IDLE) {
            if (isHandsFreeActive && hasAudioPermission) {
                kotlinx.coroutines.delay(450)
                viewModel.startListening()
            }
        }
        previousVoiceState = voiceState
    }

    // Auto-listen when screen opens if audio permission is granted
    LaunchedEffect(Unit) {
        if (hasAudioPermission) {
            viewModel.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Cleanup when leaving voice screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
            viewModel.stopSpeaking()
        }
    }

    val activeLanguage = if (voiceSettings.autoDetectLanguage) detectedLang else selectedLang

    val statusText = when (voiceState) {
        VoiceState.IDLE -> if (isHandsFreeActive) "Ready to listen • Speak or tap mic" else "Tap the microphone to speak"
        VoiceState.LISTENING -> "Listening to your voice..."
        VoiceState.THINKING -> "NIRA is thinking..."
        VoiceState.SPEAKING -> "NIRA is speaking"
    }

    val statusColor = when (voiceState) {
        VoiceState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        VoiceState.LISTENING -> NiraEmerald
        VoiceState.THINKING -> NiraViolet
        VoiceState.SPEAKING -> NiraCyan
    }

    val transcriptScrollState = rememberScrollState()
    LaunchedEffect(transcript, lastReply) {
        transcriptScrollState.animateScrollTo(transcriptScrollState.maxValue)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("voice_chat_screen")
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Navigation & Controls Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // End / Close Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("close_voice_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice Chat",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Interactive Persistent Language Pill Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable { showLanguageSheet = true }
                        .testTag("detected_language_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = activeLanguage.flag, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${activeLanguage.name}${if (voiceSettings.autoDetectLanguage) " • Auto" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Switch Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Voice gender, Language selector & Mute action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Language Selection Button
                    IconButton(
                        onClick = { showLanguageSheet = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("open_language_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Select Language",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Voice Gender Toggle
                    IconButton(
                        onClick = {
                            val next = if (voiceSettings.voiceGender == VoiceGender.FEMALE) VoiceGender.MALE else VoiceGender.FEMALE
                            viewModel.setVoiceGender(next)
                            viewModel.speakText(if (next == VoiceGender.FEMALE) "Female voice" else "Male voice")
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("voice_gender_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Switch Voice Gender",
                            tint = if (voiceSettings.voiceGender == VoiceGender.FEMALE) NiraCyan else NiraViolet
                        )
                    }

                    // Continuous Hands-Free Conversation Toggle
                    IconButton(
                        onClick = {
                            isHandsFreeActive = !isHandsFreeActive
                            if (isHandsFreeActive && voiceState == VoiceState.IDLE && hasAudioPermission) {
                                viewModel.startListening()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isHandsFreeActive) NiraEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("hands_free_voice_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isHandsFreeActive) Icons.Default.Hearing else Icons.Default.HearingDisabled,
                            contentDescription = if (isHandsFreeActive) "Hands-Free Listening Active" else "Hands-Free Listening Disabled",
                            tint = if (isHandsFreeActive) NiraEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mute Toggle
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) NiraRose.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("mute_voice_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute NIRA" else "Mute NIRA",
                            tint = if (isMuted) NiraRose else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Center Stage: Animated NIRA Avatar & Visualizer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                NiraOrbAvatar(
                    voiceState = voiceState,
                    rmsLevel = rmsLevel,
                    size = 180.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Voice State Title
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = statusColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Dynamic Real-Time Waveform Visualizer Card
                RealtimeWaveformCard(
                    voiceState = voiceState,
                    rmsLevel = rmsLevel,
                    rawDb = rawDb,
                    currentStyle = currentWaveformStyle,
                    onStyleSelected = { currentWaveformStyle = it },
                    modifier = Modifier.fillMaxWidth(0.96f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Short Live Transcript Box
                val displayUserText = if (voiceState == VoiceState.LISTENING && partialTranscript.isNotBlank()) {
                    partialTranscript
                } else {
                    transcript.ifBlank {
                        currentMessages.findLast { it.sender == "user" }?.content.orEmpty()
                    }
                }
                val displayNiraText = lastReply.ifBlank {
                    currentMessages.findLast { it.sender == "nira" }?.content.orEmpty()
                }

                if (displayUserText.isNotBlank() || displayNiraText.isNotBlank() || voiceState == VoiceState.LISTENING) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(100.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                            .padding(2.dp)
                            .testTag("voice_transcript_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(transcriptScrollState),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (voiceState == VoiceState.LISTENING && displayUserText.isBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = NiraEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Listening... Speak in ${activeLanguage.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NiraEmerald
                                    )
                                }
                            } else if (displayUserText.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (voiceState == VoiceState.LISTENING) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = NiraEmerald,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(top = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "You: $displayUserText",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            if (displayNiraText.isNotBlank()) {
                                Text(
                                    text = "NIRA: $displayNiraText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Conversation Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Switch to Text Chat
                    IconButton(
                        onClick = onNavigateToTextChat,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("switch_to_text_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Switch to Text Chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Main Animated Microphone Button
                    AnimatedMicButton(
                        voiceState = voiceState,
                        rmsLevel = rmsLevel,
                        size = 76.dp,
                        onClick = {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@AnimatedMicButton
                            }

                            when (voiceState) {
                                VoiceState.LISTENING -> viewModel.stopListening()
                                VoiceState.SPEAKING -> viewModel.stopSpeaking()
                                VoiceState.THINKING -> viewModel.stopSpeaking()
                                VoiceState.IDLE -> viewModel.startListening()
                            }
                        }
                    )

                    // End Conversation Button
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(NiraRose.copy(alpha = 0.2f))
                            .border(1.dp, NiraRose.copy(alpha = 0.4f), CircleShape)
                            .testTag("end_conversation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Conversation",
                            tint = NiraRose
                        )
                    }
                }
            }
        }

        if (showLanguageSheet) {
            LanguageSelectionBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { showLanguageSheet = false }
            )
        }
    }
}
