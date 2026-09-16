package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NiraCyan
import com.example.ui.theme.NiraEmerald
import com.example.ui.theme.NiraRose
import com.example.ui.theme.NiraViolet
import com.example.voice.VoiceState

@Composable
fun AnimatedMicButton(
    voiceState: VoiceState,
    rmsLevel: Float = 0f,
    size: Dp = 72.dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState == VoiceState.LISTENING
    val isSpeaking = voiceState == VoiceState.SPEAKING
    val isThinking = voiceState == VoiceState.THINKING

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pa1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )

    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pa2"
    )

    val buttonGradient = when (voiceState) {
        VoiceState.IDLE -> Brush.linearGradient(listOf(NiraCyan, NiraViolet))
        VoiceState.LISTENING -> Brush.linearGradient(listOf(NiraEmerald, NiraCyan))
        VoiceState.THINKING -> Brush.linearGradient(listOf(NiraViolet, NiraRose))
        VoiceState.SPEAKING -> Brush.linearGradient(listOf(NiraCyan, NiraEmerald))
    }

    val glowColor = when (voiceState) {
        VoiceState.IDLE -> NiraCyan
        VoiceState.LISTENING -> NiraEmerald
        VoiceState.THINKING -> NiraViolet
        VoiceState.SPEAKING -> NiraCyan
    }

    val dynamicScale = if (isListening) 1.0f + (rmsLevel * 0.15f) else 1.0f

    Box(
        modifier = modifier
            .size(size * 1.8f)
            .testTag("animated_mic_button_container"),
        contentAlignment = Alignment.Center
    ) {
        // Concentric expanding shockwaves when listening
        if (isListening) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = (size.toPx() / 2f)

                drawCircle(
                    color = glowColor.copy(alpha = pulseAlpha1),
                    radius = baseRadius * pulse1,
                    center = center
                )

                drawCircle(
                    color = glowColor.copy(alpha = pulseAlpha2),
                    radius = baseRadius * pulse2,
                    center = center
                )
            }
        }

        // Active Button
        Box(
            modifier = Modifier
                .size(size)
                .scale(dynamicScale)
                .clip(CircleShape)
                .background(buttonGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onClick
                )
                .testTag("animated_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            val icon = when {
                isListening -> Icons.Default.Stop
                isSpeaking -> Icons.Default.MicOff
                else -> Icons.Default.Mic
            }

            val iconColor = Color.White

            Icon(
                imageVector = icon,
                contentDescription = if (isListening) "Stop Listening" else "Start Voice Input",
                tint = iconColor,
                modifier = Modifier.size(size * 0.46f)
            )
        }
    }
}
