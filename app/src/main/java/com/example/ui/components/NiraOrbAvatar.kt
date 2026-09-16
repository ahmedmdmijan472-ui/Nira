package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.NiraCyan
import com.example.ui.theme.NiraEmerald
import com.example.ui.theme.NiraRose
import com.example.ui.theme.NiraViolet
import com.example.voice.VoiceState
import kotlin.math.sin

@Composable
fun NiraOrbAvatar(
    voiceState: VoiceState,
    rmsLevel: Float = 0f,
    size: Dp = 220.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nira_orb_anim")

    // Slow rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Reverse fast rotation for thinking
    val fastRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fast_rotation"
    )

    // Gentle breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Speaking rhythm pulse
    val speakWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speak_wave"
    )

    // Effective scale based on state and microphone RMS
    val stateScale = when (voiceState) {
        VoiceState.IDLE -> breathScale
        VoiceState.LISTENING -> 1.0f + (rmsLevel * 0.25f)
        VoiceState.THINKING -> breathScale * 1.02f
        VoiceState.SPEAKING -> 0.98f + (speakWave * 0.12f)
    }

    // Glow colors based on state
    val glowColors = when (voiceState) {
        VoiceState.IDLE -> listOf(NiraCyan.copy(alpha = 0.4f), NiraViolet.copy(alpha = 0.25f), Color.Transparent)
        VoiceState.LISTENING -> listOf(NiraEmerald.copy(alpha = 0.6f + (rmsLevel * 0.3f)), NiraCyan.copy(alpha = 0.4f), Color.Transparent)
        VoiceState.THINKING -> listOf(NiraViolet.copy(alpha = 0.65f), NiraCyan.copy(alpha = 0.45f), Color.Transparent)
        VoiceState.SPEAKING -> listOf(NiraCyan.copy(alpha = 0.7f), NiraViolet.copy(alpha = 0.5f), NiraRose.copy(alpha = 0.2f))
    }

    val primaryAccent = when (voiceState) {
        VoiceState.IDLE -> NiraCyan
        VoiceState.LISTENING -> NiraEmerald
        VoiceState.THINKING -> NiraViolet
        VoiceState.SPEAKING -> NiraCyan
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("nira_orb_avatar"),
        contentAlignment = Alignment.Center
    ) {
        // Outer energy aura canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.85f

            // Radial Glow Behind Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = glowColors,
                    center = center,
                    radius = baseRadius * 1.25f
                ),
                radius = baseRadius * 1.2f,
                center = center
            )

            // Dynamic ripples when listening
            if (voiceState == VoiceState.LISTENING) {
                val ripple1 = (baseRadius * 0.95f) + (rmsLevel * 28f)
                val ripple2 = (baseRadius * 1.08f) + (rmsLevel * 45f)

                drawCircle(
                    color = NiraEmerald.copy(alpha = (0.4f + rmsLevel * 0.4f).coerceIn(0f, 0.9f)),
                    radius = ripple1,
                    center = center,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                drawCircle(
                    color = NiraCyan.copy(alpha = (0.25f + rmsLevel * 0.3f).coerceIn(0f, 0.7f)),
                    radius = ripple2,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Orbital wave arcs when thinking
            if (voiceState == VoiceState.THINKING) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(NiraViolet, NiraCyan, Color.Transparent, NiraViolet)
                    ),
                    startAngle = fastRotation,
                    sweepAngle = 260f,
                    useCenter = false,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2.1f, baseRadius * 2.1f),
                    topLeft = Offset(center.x - baseRadius * 1.05f, center.y - baseRadius * 1.05f)
                )
            }

            // Sonic arcs when speaking
            if (voiceState == VoiceState.SPEAKING) {
                val arcSweep = 120f + (speakWave * 90f)
                drawArc(
                    color = NiraCyan.copy(alpha = 0.75f),
                    startAngle = rotation,
                    sweepAngle = arcSweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2.05f, baseRadius * 2.05f),
                    topLeft = Offset(center.x - baseRadius * 1.025f, center.y - baseRadius * 1.025f)
                )
                drawArc(
                    color = NiraViolet.copy(alpha = 0.75f),
                    startAngle = rotation + 180f,
                    sweepAngle = arcSweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2.05f, baseRadius * 2.05f),
                    topLeft = Offset(center.x - baseRadius * 1.025f, center.y - baseRadius * 1.025f)
                )
            }
        }

        // Inner Core Spherical Container
        val innerSize = size * 0.76f
        Box(
            modifier = Modifier
                .size(innerSize)
                .scale(stateScale)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(primaryAccent, NiraViolet, primaryAccent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Orb Image Asset
            Image(
                painter = painterResource(id = R.drawable.nira_orb),
                contentDescription = "NIRA Assistant Orb",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(if (voiceState == VoiceState.THINKING) fastRotation * 0.2f else rotation * 0.1f)
            )

            // Layer 2: Futuristic holographic dynamic overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryAccent.copy(alpha = if (voiceState == VoiceState.LISTENING) 0.35f else 0.18f),
                                Color(0x99000000)
                            )
                        )
                    )
            )

            // Center focal flare
            Box(
                modifier = Modifier
                    .size(innerSize * 0.38f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.75f),
                                primaryAccent.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
