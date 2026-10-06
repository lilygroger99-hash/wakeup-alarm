package com.wakeup.alarm.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.alarm.R
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.ui.common.Formatters
import com.wakeup.alarm.ui.common.PermissionBanner
import com.wakeup.alarm.ui.common.containerViewModel
import com.wakeup.alarm.ui.common.rememberPermissionStatus
import com.wakeup.alarm.ui.common.rememberResources
import com.wakeup.alarm.ui.theme.AlarmTimeTextStyle
import com.wakeup.alarm.util.PermissionUtils
import com.wakeup.alarm.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStopwatch: () -> Unit,
) {
    val viewModel = containerViewModel("home") { HomeViewModel(it.alarmRepository, it.settingsRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissions by rememberPermissionStatus()

    // Ask for the notification permission once, the first time the home screen opens (Android 13+ only).
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedForNotifications &&
            PermissionUtils.notificationsNeedRuntimePermission &&
            !PermissionUtils.notificationsAllowed(context)
        ) {
            askedForNotifications = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val is24Hour = TimeFormatter.is24Hour(context, state.settings.timeFormat)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAlarm,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.home_add_alarm)) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { Header(onOpenSettings = onOpenSettings, onOpenStopwatch = onOpenStopwatch) }

            item(key = "next") {
                NextAlarmCard(next = state.next, nowMillis = state.nowMillis, is24Hour = is24Hour)
            }

            // Only the most important missing permission is shown, so the screen stays calm.
            val banner = when {
                !permissions.notifications -> Triple(
                    R.string.perm_notifications_title,
                    R.string.perm_notifications_message,
                    R.string.perm_notifications_action,
                ) to { PermissionUtils.openNotificationSettings(context) }
                !permissions.exactAlarms -> Triple(
                    R.string.perm_exact_title,
                    R.string.perm_exact_message,
                    R.string.perm_exact_action,
                ) to { PermissionUtils.openExactAlarmSettings(context) }
                !permissions.fullScreen -> Triple(
                    R.string.perm_fullscreen_title,
                    R.string.perm_fullscreen_message,
                    R.string.perm_fullscreen_action,
                ) to { PermissionUtils.openFullScreenIntentSettings(context) }
                else -> null
            }
            if (banner != null) {
                val (texts, action) = banner
                item(key = "permission-banner") {
                    PermissionBanner(
                        title = stringResource(texts.first),
                        message = stringResource(texts.second),
                        actionLabel = stringResource(texts.third),
                        onAction = action,
                    )
                }
            }

            if (!state.loading) {
                if (state.alarms.isEmpty()) {
                    item(key = "empty") { EmptyState() }
                } else {
                    items(state.alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            is24Hour = is24Hour,
                            nowMillis = state.nowMillis,
                            onClick = { onEditAlarm(alarm.id) },
                            onToggle = { enabled -> viewModel.setEnabled(alarm.id, enabled) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit, onOpenStopwatch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        IconButton(onClick = onOpenStopwatch, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_stopwatch), contentDescription = stringResource(R.string.home_stopwatch))
        }
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.home_settings))
        }
    }
}

@Composable
private fun NextAlarmCard(next: NextAlarm?, nowMillis: Long, is24Hour: Boolean) {
    val resources = rememberResources()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.home_next_alarm_label),
                style = MaterialTheme.typography.labelLarge,
            )
            if (next == null) {
                Text(
                    text = stringResource(R.string.home_no_next),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.home_no_next_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val remaining = Formatters.duration(resources, next.triggerMillis - nowMillis)
                val day = Formatters.dayLabel(resources, next.triggerMillis, nowMillis)
                val time = TimeFormatter.timeTextFromMillis(next.triggerMillis, is24Hour)
                Text(
                    text = stringResource(R.string.home_next_alarm_in, remaining),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.home_next_alarm_when, day, time),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    is24Hour: Boolean,
    nowMillis: Long,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = rememberResources()
    val colors = MaterialTheme.colorScheme
    val (timeText, marker) = remember(alarm.hour, alarm.minute, is24Hour) {
        TimeFormatter.timeParts(alarm.hour, alarm.minute, is24Hour)
    }
    val fullTime = remember(alarm.hour, alarm.minute, is24Hour) {
        TimeFormatter.timeText(alarm.hour, alarm.minute, is24Hour)
    }
    val containerColor by animateColorAsState(
        targetValue = if (alarm.enabled) colors.surfaceContainerHigh else colors.surfaceContainerLow,
        label = "alarmCardColor",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (alarm.enabled) 1f else 0.62f,
        label = "alarmCardAlpha",
    )
    val repeatText = Formatters.repeatSummary(resources, alarm.repeatDays)
    val snoozed = alarm.enabled && alarm.isSnoozedAt(nowMillis)
    val switchDescription = stringResource(R.string.alarm_switch_description, fullTime)

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier
                .weight(1f)
                .alpha(contentAlpha)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeText,
                        style = AlarmTimeTextStyle,
                        maxLines = 1,
                        softWrap = false,
                    )
                    if (marker.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = marker,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 9.dp),
                        )
                    }
                }
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics {
                            contentDescription = resources.getString(R.string.alarm_card_label_description, alarm.label)
                        },
                    )
                }
                Text(
                    text = repeatText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (snoozed) {
                    Text(
                        text = stringResource(
                            R.string.alarm_snoozed_until,
                            TimeFormatter.timeTextFromMillis(alarm.snoozedUntilMillis, is24Hour),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.tertiary,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = switchDescription },
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(112.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.home_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
