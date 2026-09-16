package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.model.SupportedLanguage
import com.example.ui.components.LanguageSelectionBottomSheet
import com.example.ui.components.VoiceVisualizer
import com.example.ui.theme.NiraCyan
import com.example.ui.theme.NiraEmerald
import com.example.ui.theme.NiraViolet
import com.example.ui.viewmodel.NiraViewModel
import com.example.voice.VoiceState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextChatScreen(
    viewModel: NiraViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVoiceChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.currentMessages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val voiceState by viewModel.speechManager.voiceState.collectAsState()
    val rmsLevel by viewModel.speechManager.rmsLevel.collectAsState()
    val selectedLang by viewModel.selectedLanguage.collectAsState()
    val detectedLang by viewModel.detectedLanguage.collectAsState()
    val partialTranscript by viewModel.partialTranscript.collectAsState()

    var showLanguageSheet by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Sync speech transcription to input preview if speaking
    LaunchedEffect(partialTranscript, voiceState) {
        if (voiceState == VoiceState.LISTENING && partialTranscript.isNotBlank()) {
            textInput = partialTranscript
        }
    }

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

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Mini NIRA Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, NiraCyan, CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.nira_orb),
                                contentDescription = "NIRA Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NIRA",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(NiraEmerald)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showLanguageSheet = true }
                                    .testTag("chat_language_selector_chip")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${selectedLang.flag} ${selectedLang.name}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = "Switch Language",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Language Selector Button
                    IconButton(
                        onClick = { showLanguageSheet = true },
                        modifier = Modifier.testTag("chat_language_selector_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language Selection",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // New Chat Button
                    IconButton(
                        onClick = { viewModel.createNewConversation() },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Direct Jump to Voice Chat Mode
                    IconButton(
                        onClick = onNavigateToVoiceChat,
                        modifier = Modifier.testTag("open_voice_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Full Voice Chat",
                            tint = NiraCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Audio visualizer and live speech-to-text banner when mic listening in chat
                AnimatedVisibility(
                    visible = voiceState == VoiceState.LISTENING,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(NiraEmerald.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, NiraEmerald.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = NiraEmerald,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 4.dp)
                            )
                            Text(
                                text = if (partialTranscript.isNotBlank()) "Listening: \"$partialTranscript\"" else "Listening in ${selectedLang.name}... Speak now",
                                style = MaterialTheme.typography.labelSmall,
                                color = NiraEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        VoiceVisualizer(
                            voiceState = voiceState,
                            rmsLevel = rmsLevel,
                            barCount = 12,
                            maxHeight = 18.dp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        placeholder = {
                            Text(
                                text = "Ask NIRA anything in any language...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput, isVoiceInput = false, speakOutput = true)
                                    textInput = ""
                                }
                            }
                        ),
                        maxLines = 4
                    )

                    // Microphone Dictation Button
                    IconButton(
                        onClick = {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                return@IconButton
                            }
                            if (voiceState == VoiceState.LISTENING) {
                                viewModel.stopListening()
                            } else {
                                viewModel.startListening()
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == VoiceState.LISTENING) NiraEmerald else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("chat_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (voiceState == VoiceState.LISTENING) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput, isVoiceInput = false, speakOutput = true)
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (textInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            if (messages.isEmpty() && !isThinking) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(2.dp, NiraCyan, CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.nira_orb),
                                contentDescription = "NIRA Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "NIRA AI Assistant",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Speak or type in Bangla, English, Hindi, Arabic or any language.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(messages) { message ->
                ChatMessageItem(
                    message = message,
                    onSpeak = { text -> viewModel.speakText(text, message.languageDetected) },
                    onRegenerate = { viewModel.regenerateLastMessage() },
                    onCopy = { text ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("NIRA Message", text)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    onShare = { text ->
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, text)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share with"))
                    }
                )
            }

            // Typing / Thinking Animation Bubble
            if (isThinking) {
                item {
                    NiraThinkingBubble()
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

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onSpeak: (String) -> Unit,
    onRegenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    val isUser = message.sender == "user"
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_${message.id}"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // NIRA Small Icon
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.dp, NiraCyan, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nira_orb),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            // Bubble Content
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (message.isVoiceMessage && isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice Message",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Message Meta & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                if (!isUser) {
                    // Speak aloud
                    IconButton(
                        onClick = { onSpeak(message.content) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Speak Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Copy
                    IconButton(
                        onClick = { onCopy(message.content) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Message",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Regenerate
                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Share
                    IconButton(
                        onClick = { onShare(message.content) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NiraThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_dots")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(1.dp, NiraCyan, CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.nira_orb),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "NIRA is thinking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NiraCyan.copy(alpha = dot1Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NiraCyan.copy(alpha = dot2Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NiraCyan.copy(alpha = dot3Alpha))
                )
            }
        }
    }
}
