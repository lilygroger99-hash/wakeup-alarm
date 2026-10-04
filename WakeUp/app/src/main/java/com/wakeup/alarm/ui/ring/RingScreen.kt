package com.wakeup.alarm.ui.ring

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeup.alarm.R
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.ui.common.rememberResources
import com.wakeup.alarm.ui.theme.RingTimeTextStyle
import com.wakeup.alarm.util.TimeFormatter

/**
 * Full-screen ringing UI. Two huge buttons at the bottom (reachable with a thumb, easy to hit half asleep):
 * SNOOZE above and STOP below. [alarm] is `null` for a moment while it loads; the buttons already work then.
 */
@Composable
fun RingScreen(
    alarm: Alarm?,
    is24Hour: Boolean,
    onStop: () -> Unit,
    onSnooze: () -> Unit,
) {
    val resources = rememberResources()
    val colors = MaterialTheme.colorScheme
    val snoozeMinutes = alarm?.snoozeMinutes ?: Alarm.DEFAULT_SNOOZE_MINUTES
    val snoozeText = resources.getQuantityString(R.plurals.duration_minutes, snoozeMinutes, snoozeMinutes)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primaryContainer, colors.background)))
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            PulsingBell()
            Spacer(Modifier.height(24.dp))

            if (alarm != null) {
                val (time, marker) = remember(alarm.hour, alarm.minute, is24Hour) {
                    TimeFormatter.timeParts(alarm.hour, alarm.minute, is24Hour)
                }
                val fullTime = TimeFormatter.timeText(alarm.hour, alarm.minute, is24Hour)
                // Announced by screen readers as soon as the screen appears.
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Assertive
                        contentDescription = fullTime
                    },
                ) {
                    Text(text = time, style = RingTimeTextStyle, color = colors.onBackground, maxLines = 1, softWrap = false)
                    if (marker.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = marker,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onBackground,
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                    }
                }
                Text(
                    text = alarm.label.ifBlank { stringResource(R.string.ring_default_title) },
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            val snoozeDescription = stringResource(R.string.ring_snooze_description, snoozeText)
            FilledTonalButton(
                onClick = onSnooze,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .semantics { contentDescription = snoozeDescription },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.action_snooze), style = MaterialTheme.typography.headlineSmall)
                    Text(snoozeText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            val stopDescription = stringResource(R.string.ring_stop_description)
            Button(
                onClick = onStop,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.tertiary,
                    contentColor = colors.onTertiary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .semantics { contentDescription = stopDescription },
            ) {
                Text(stringResource(R.string.action_stop), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun PulsingBell() {
    val transition = rememberInfiniteTransition(label = "bellPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 650), repeatMode = RepeatMode.Reverse),
        label = "bellScale",
    )
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier
            .size(104.dp)
            .scale(scale),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(52.dp))
        }
    }
}
