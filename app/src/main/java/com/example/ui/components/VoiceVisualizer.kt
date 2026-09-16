package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NiraCyan
import com.example.ui.theme.NiraEmerald
import com.example.ui.theme.NiraRose
import com.example.ui.theme.NiraViolet
import com.example.voice.VoiceState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual presentation style for the real-time voice waveform.
 */
enum class WaveformStyle(val title: String, val icon: ImageVector, val tag: String) {
    FLUID_WAVE("Fluid Wave", Icons.Default.Waves, "waveform_style_fluid"),
    STUDIO_BARS("Studio Bars", Icons.Default.GraphicEq, "waveform_style_bars"),
    PULSE_SPECTRUM("Pulse Spectrum", Icons.AutoMirrored.Filled.ShowChart, "waveform_style_spectrum")
}

/**
 * High-performance, real-time waveform visualizer canvas reacting dynamically
 * to audio amplitude and voice state.
 */
@Composable
fun RealtimeWaveformVisualizer(
    voiceState: VoiceState,
    rmsLevel: Float = 0f,
    style: WaveformStyle = WaveformStyle.FLUID_WAVE,
    primaryColor: Color = NiraEmerald,
    secondaryColor: Color = NiraCyan,
    accentColor: Color = NiraViolet,
    modifier: Modifier = Modifier
) {
    // Dynamic physics-based animated amplitude reacting instantaneously to voice input
    val targetAmplitude = when (voiceState) {
        VoiceState.LISTENING -> (rmsLevel * 1.25f).coerceIn(0.04f, 1f)
        VoiceState.SPEAKING -> 0.72f
        VoiceState.THINKING -> 0.32f
        VoiceState.IDLE -> 0.04f
    }

    val animatedAmplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "animated_amplitude"
    )

    // Smooth continuous phase progression for organic wave motion
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_phase_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_phase"
    )

    // Secondary modulation rhythm
    val harmonicPhase by infiniteTransition.animateFloat(
        initialValue = (2f * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "harmonic_phase"
    )

    // Remember peak bars for Studio Bars style to simulate physical gravity decay
    val peakBars = remember { FloatArray(36) { 0f } }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("realtime_waveform_canvas")
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        if (width <= 0f || height <= 0f) return@Canvas

        when (style) {
            WaveformStyle.FLUID_WAVE -> {
                val maxWaveAmp = (height * 0.44f) * (0.12f + animatedAmplitude * 0.88f)

                // Background Ambient Aura Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = (0.25f * animatedAmplitude).coerceIn(0.05f, 0.45f)),
                            Color.Transparent
                        ),
                        center = Offset(width / 2f, centerY),
                        radius = (width * 0.45f) * (0.8f + animatedAmplitude * 0.4f)
                    ),
                    radius = (width * 0.45f) * (0.8f + animatedAmplitude * 0.4f),
                    center = Offset(width / 2f, centerY)
                )

                // Layer 1: Background Harmonic Violet Wave with Gradient Fill
                val path1 = Path().apply {
                    moveTo(0f, centerY)
                    val steps = 50
                    for (step in 0..steps) {
                        val x = (step.toFloat() / steps) * width
                        val normX = (x / width).coerceIn(0f, 1f)
                        val envelope = sin(normX * PI.toFloat()).let { it * it }
                        val y = centerY + sin(normX * 2.8f * 2f * PI.toFloat() - phase * 1.1f) * maxWaveAmp * 0.65f * envelope
                        lineTo(x, y)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = path1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = (0.22f + animatedAmplitude * 0.28f).coerceIn(0.08f, 0.6f)),
                            Color.Transparent
                        ),
                        startY = centerY - maxWaveAmp,
                        endY = height
                    )
                )

                // Layer 2: Middle Cyan Wave with Vertical Gradient Fill
                val path2 = Path().apply {
                    moveTo(0f, centerY)
                    val steps = 50
                    for (step in 0..steps) {
                        val x = (step.toFloat() / steps) * width
                        val normX = (x / width).coerceIn(0f, 1f)
                        val envelope = sin(normX * PI.toFloat()).let { it * it }
                        val waveOffset = sin(normX * 2.1f * 2f * PI.toFloat() + harmonicPhase) * 0.72f +
                                cos(normX * 4.4f * 2f * PI.toFloat() - phase * 0.8f) * 0.28f
                        val y = centerY + waveOffset * maxWaveAmp * 0.82f * envelope
                        lineTo(x, y)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(
                    path = path2,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = (0.28f + animatedAmplitude * 0.32f).coerceIn(0.1f, 0.7f)),
                            Color.Transparent
                        ),
                        startY = centerY - maxWaveAmp,
                        endY = height
                    )
                )

                // Layer 3: Foreground Primary Glowing Wave Crest
                val path3 = Path().apply {
                    moveTo(0f, centerY)
                    val steps = 64
                    for (step in 0..steps) {
                        val x = (step.toFloat() / steps) * width
                        val normX = (x / width).coerceIn(0f, 1f)
                        val envelope = sin(normX * PI.toFloat()).let { it * it }
                        val waveOffset = sin(normX * 2f * 2f * PI.toFloat() - phase * 1.5f) * 0.85f +
                                sin(normX * 5.2f * 2f * PI.toFloat() + phase * 0.9f) * 0.22f
                        val y = centerY + waveOffset * maxWaveAmp * envelope
                        lineTo(x, y)
                    }
                }

                // Glowing Stroke for the Crest Wave
                drawPath(
                    path = path3,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.35f),
                            primaryColor,
                            accentColor,
                            primaryColor,
                            secondaryColor.copy(alpha = 0.35f)
                        )
                    ),
                    style = Stroke(
                        width = (2.5f + (animatedAmplitude * 3f)).dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Baseline Ambient Center Line
                drawLine(
                    color = primaryColor.copy(alpha = 0.2f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            WaveformStyle.STUDIO_BARS -> {
                val barCount = 28
                val spacing = 3.dp.toPx()
                val totalSpacing = spacing * (barCount - 1)
                val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())
                val maxBarHeight = height * 0.44f

                for (i in 0 until barCount) {
                    val x = i * (barWidth + spacing)
                    val distFromCenter = abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f)
                    val bell = (1f - distFromCenter * 0.58f).coerceIn(0.22f, 1f)

                    // Acoustic frequency modulation derived from phase and position
                    val harmonic = sin(phase * 2.2f + i * 0.55f) * 0.16f +
                            cos(harmonicPhase * 1.4f - i * 0.38f) * 0.12f
                    val barEnergy = (animatedAmplitude * 0.82f + harmonic * animatedAmplitude + 0.08f).coerceIn(0.06f, 1f)
                    val currentBarH = (maxBarHeight * barEnergy * bell).coerceAtLeast(3.dp.toPx())

                    // Physics gravity decay for floating peak indicators
                    if (currentBarH > peakBars[i]) {
                        peakBars[i] = currentBarH
                    } else {
                        peakBars[i] = (peakBars[i] - 1.2f).coerceAtLeast(currentBarH)
                    }

                    val top = centerY - currentBarH
                    val bottom = centerY + currentBarH

                    // Mirrored rounded frequency bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor, primaryColor),
                            startY = top,
                            endY = bottom
                        ),
                        topLeft = Offset(x, top),
                        size = Size(barWidth, bottom - top),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Peak drop indicator dot floating above the bar
                    val peakTop = centerY - peakBars[i] - 3.dp.toPx()
                    drawCircle(
                        color = accentColor.copy(alpha = 0.9f),
                        radius = (barWidth / 2f).coerceIn(1.5f, 3.5f),
                        center = Offset(x + barWidth / 2f, peakTop)
                    )
                }

                // Center dividing line
                drawLine(
                    color = primaryColor.copy(alpha = 0.25f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            WaveformStyle.PULSE_SPECTRUM -> {
                val nodeCount = 26
                val stepX = width / (nodeCount - 1)
                val maxPulse = height * 0.43f
                val points = mutableListOf<Offset>()

                for (i in 0 until nodeCount) {
                    val x = i * stepX
                    val normX = (i.toFloat() / (nodeCount - 1)).coerceIn(0f, 1f)
                    val envelope = sin(normX * PI.toFloat()).let { it * it }
                    val harmonic = sin(normX * 10f + phase * 2.4f) * 0.55f +
                            cos(normX * 6f - harmonicPhase * 1.8f) * 0.45f
                    val yOffset = maxPulse * (animatedAmplitude * 0.88f + 0.08f) * envelope * harmonic
                    points.add(Offset(x, centerY + yOffset))
                }

                // Connected polyline
                val polyline = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (p in points.drop(1)) {
                        lineTo(p.x, p.y)
                    }
                }

                // Shadow reflection beneath
                val shadowPolyline = Path().apply {
                    moveTo(points.first().x, centerY - (points.first().y - centerY) * 0.6f)
                    for (p in points.drop(1)) {
                        lineTo(p.x, centerY - (p.y - centerY) * 0.6f)
                    }
                }
                drawPath(
                    path = shadowPolyline,
                    brush = Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.25f), secondaryColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.25f))
                    ),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Main Spectrum Stroke
                drawPath(
                    path = polyline,
                    brush = Brush.horizontalGradient(
                        listOf(accentColor, primaryColor, secondaryColor, primaryColor, accentColor)
                    ),
                    style = Stroke(
                        width = (2.5f + animatedAmplitude * 2.5f).dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Glowing Node Vertices
                for (p in points) {
                    drawCircle(
                        color = Color.White,
                        radius = (1.8f + animatedAmplitude * 1.8f).dp.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.45f),
                        radius = (4f + animatedAmplitude * 3f).dp.toPx(),
                        center = p
                    )
                }

                // Center baseline glow
                drawLine(
                    color = primaryColor.copy(alpha = 0.3f + animatedAmplitude * 0.4f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

/**
 * Premium glassmorphic card container featuring:
 * 1. Real-time audio telemetry (live microphone activity, amplitude percentage, decibel meter).
 * 2. Interactive waveform style selector (Fluid Wave, Studio Bars, Pulse Spectrum).
 * 3. Dynamic RealtimeWaveformVisualizer canvas.
 * 4. Responsive bottom amplitude meter strip with peak detection.
 */
@Composable
fun RealtimeWaveformCard(
    voiceState: VoiceState,
    rmsLevel: Float,
    rawDb: Float = -2f,
    currentStyle: WaveformStyle = WaveformStyle.FLUID_WAVE,
    onStyleSelected: (WaveformStyle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Dynamic Accent Colors based on voice state
    val primaryColor = when (voiceState) {
        VoiceState.IDLE -> NiraCyan.copy(alpha = 0.6f)
        VoiceState.LISTENING -> NiraEmerald
        VoiceState.THINKING -> NiraViolet
        VoiceState.SPEAKING -> NiraCyan
    }

    val secondaryColor = when (voiceState) {
        VoiceState.IDLE -> NiraViolet.copy(alpha = 0.4f)
        VoiceState.LISTENING -> NiraCyan
        VoiceState.THINKING -> NiraEmerald
        VoiceState.SPEAKING -> NiraViolet
    }

    val accentColor = when (voiceState) {
        VoiceState.IDLE -> NiraCyan.copy(alpha = 0.3f)
        VoiceState.LISTENING -> if (rmsLevel > 0.65f) NiraRose else NiraViolet
        VoiceState.THINKING -> NiraCyan
        VoiceState.SPEAKING -> NiraRose
    }

    // Dynamic amplitude status text
    val amplitudePercent = (rmsLevel * 100).toInt().coerceIn(0, 100)
    val statusLabel = when (voiceState) {
        VoiceState.LISTENING -> {
            if (rmsLevel > 0.25f) "Speaking • $amplitudePercent%" else "Listening • Speak now"
        }
        VoiceState.THINKING -> "Processing audio..."
        VoiceState.SPEAKING -> "NIRA Speaking • Live Output"
        VoiceState.IDLE -> "Mic Standby • Tap to speak"
    }

    // Decibel readout formatting
    val dbText = when {
        voiceState == VoiceState.IDLE -> "Muted"
        voiceState == VoiceState.THINKING -> "AI Active"
        voiceState == VoiceState.SPEAKING -> "Output: High"
        rawDb > -20f -> "${if (rawDb > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", rawDb)} dB"
        else -> "-∞ dB"
    }

    // Animated meter level
    val meterProgress by animateFloatAsState(
        targetValue = when (voiceState) {
            VoiceState.LISTENING -> rmsLevel.coerceIn(0.04f, 1f)
            VoiceState.SPEAKING -> 0.75f
            VoiceState.THINKING -> 0.4f
            VoiceState.IDLE -> 0.04f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "meter_progress"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.2f),
                        primaryColor.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("realtime_waveform_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Top Bar: Audio Telemetry & Style Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Live Audio Telemetry Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.testTag("live_audio_telemetry_badge")
                ) {
                    // Pulsing beacon dot
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == VoiceState.LISTENING && rmsLevel > 0.2f) NiraEmerald
                                else if (voiceState == VoiceState.LISTENING) NiraEmerald.copy(alpha = 0.6f)
                                else if (voiceState == VoiceState.SPEAKING) NiraCyan
                                else if (voiceState == VoiceState.THINKING) NiraViolet
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )

                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Decibel chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = dbText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Right: Style Selector Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WaveformStyle.values().forEach { style ->
                        val isSelected = style == currentStyle
                        val buttonColor by animateColorAsState(
                            targetValue = if (isSelected) primaryColor.copy(alpha = 0.22f) else Color.Transparent,
                            label = "style_btn_color"
                        )
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            label = "style_icon_color"
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(buttonColor)
                                .then(
                                    if (isSelected) Modifier.border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, radius = 16.dp),
                                    role = Role.Button,
                                    onClick = { onStyleSelected(style) }
                                )
                                .testTag(style.tag),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = style.icon,
                                contentDescription = "Switch to ${style.title}",
                                tint = iconColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center: The Real-Time Waveform Visualizer Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
            ) {
                RealtimeWaveformVisualizer(
                    voiceState = voiceState,
                    rmsLevel = rmsLevel,
                    style = currentStyle,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Bar: Dynamic Real-time Audio Level Progress Track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(meterProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NiraEmerald,
                                    NiraCyan,
                                    if (meterProgress > 0.7f) NiraRose else NiraViolet
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Standard VoiceVisualizer maintaining backward-compatible function signature
 * for use anywhere in the application (including TextChatScreen), upgraded with
 * dynamic amplitude reactivity.
 */
@Composable
fun VoiceVisualizer(
    voiceState: VoiceState,
    rmsLevel: Float = 0f,
    barCount: Int = 18,
    maxHeight: Dp = 48.dp,
    modifier: Modifier = Modifier,
    style: WaveformStyle = WaveformStyle.STUDIO_BARS
) {
    RealtimeWaveformVisualizer(
        voiceState = voiceState,
        rmsLevel = rmsLevel,
        style = style,
        modifier = modifier
            .height(maxHeight)
            .width((barCount * 8).dp)
    )
}
