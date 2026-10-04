package com.wakeup.alarm.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.alarm.R
import com.wakeup.alarm.data.settings.ThemeMode
import com.wakeup.alarm.data.settings.TimeFormatPreference
import com.wakeup.alarm.domain.model.Alarm
import com.wakeup.alarm.ui.common.ChoiceDialog
import com.wakeup.alarm.ui.common.Formatters
import com.wakeup.alarm.ui.common.RowDivider
import com.wakeup.alarm.ui.common.SectionHeader
import com.wakeup.alarm.ui.common.SettingsGroup
import com.wakeup.alarm.ui.common.SettingsRow
import com.wakeup.alarm.ui.common.SwitchRow
import com.wakeup.alarm.ui.common.containerViewModel
import com.wakeup.alarm.ui.common.rememberPermissionStatus
import com.wakeup.alarm.ui.common.rememberResources
import com.wakeup.alarm.ui.common.rememberSoundPicker
import com.wakeup.alarm.util.PermissionUtils

private enum class SettingsDialog { NONE, THEME, TIME_FORMAT, SNOOZE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenAbout: () -> Unit) {
    val viewModel = containerViewModel("settings") { SettingsViewModel(it.settingsRepository, it.appContext) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val soundInfo by viewModel.defaultSoundInfo.collectAsStateWithLifecycle()
    val permissions by rememberPermissionStatus()
    val context = LocalContext.current
    val resources = rememberResources()

    var dialog by rememberSaveable { mutableStateOf(SettingsDialog.NONE) }
    val pickSound = rememberSoundPicker(settings.defaultSoundUri) { viewModel.setDefaultSoundUri(it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            // ---- Appearance ---------------------------------------------------------------------------------
            SectionHeader(stringResource(R.string.settings_section_appearance))
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.settings_theme),
                    summary = stringResource(themeLabel(settings.themeMode)),
                    onClick = { dialog = SettingsDialog.THEME },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RowDivider()
                    SwitchRow(
                        title = stringResource(R.string.settings_dynamic_color),
                        summary = stringResource(R.string.settings_dynamic_color_summary),
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_time_format),
                    summary = stringResource(timeFormatLabel(settings.timeFormat)),
                    onClick = { dialog = SettingsDialog.TIME_FORMAT },
                )
            }

            // ---- Defaults for new alarms --------------------------------------------------------------------
            SectionHeader(stringResource(R.string.settings_section_defaults))
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.settings_default_snooze),
                    summary = resources.getQuantityString(
                        R.plurals.duration_minutes,
                        settings.defaultSnoozeMinutes,
                        settings.defaultSnoozeMinutes,
                    ),
                    onClick = { dialog = SettingsDialog.SNOOZE },
                )
                RowDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_default_vibrate),
                    checked = settings.defaultVibrate,
                    onCheckedChange = viewModel::setDefaultVibrate,
                )
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_default_sound),
                    summary = Formatters.soundSummary(resources, soundInfo),
                    onClick = pickSound,
                )
            }

            // ---- Permissions --------------------------------------------------------------------------------
            SectionHeader(stringResource(R.string.settings_section_permissions))
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.settings_perm_notifications),
                    summary = permissionSummary(permissions.notifications),
                    onClick = { PermissionUtils.openNotificationSettings(context) },
                )
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_perm_exact),
                    summary = permissionSummary(permissions.exactAlarms),
                    onClick = { PermissionUtils.openExactAlarmSettings(context) },
                )
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_perm_fullscreen),
                    summary = permissionSummary(permissions.fullScreen),
                    onClick = { PermissionUtils.openFullScreenIntentSettings(context) },
                )
            }

            // ---- About --------------------------------------------------------------------------------------
            SectionHeader(stringResource(R.string.settings_section_about))
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.settings_about, stringResource(R.string.app_name)),
                    onClick = onOpenAbout,
                )
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_privacy),
                    onClick = { openPrivacyPolicy(context) },
                )
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_version),
                    summary = Formatters.appVersionName(context),
                )
            }
        }
    }

    when (dialog) {
        SettingsDialog.NONE -> Unit
        SettingsDialog.THEME -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.values().map { it to stringResource(themeLabel(it)) },
            selected = settings.themeMode,
            onSelect = {
                viewModel.setThemeMode(it)
                dialog = SettingsDialog.NONE
            },
            onDismiss = { dialog = SettingsDialog.NONE },
        )
        SettingsDialog.TIME_FORMAT -> ChoiceDialog(
            title = stringResource(R.string.settings_time_format),
            options = TimeFormatPreference.values().map { it to stringResource(timeFormatLabel(it)) },
            selected = settings.timeFormat,
            onSelect = {
                viewModel.setTimeFormat(it)
                dialog = SettingsDialog.NONE
            },
            onDismiss = { dialog = SettingsDialog.NONE },
        )
        SettingsDialog.SNOOZE -> ChoiceDialog(
            title = stringResource(R.string.settings_default_snooze),
            options = Alarm.SNOOZE_OPTIONS.map { minutes ->
                minutes to resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
            },
            selected = settings.defaultSnoozeMinutes,
            onSelect = {
                viewModel.setDefaultSnoozeMinutes(it)
                dialog = SettingsDialog.NONE
            },
            onDismiss = { dialog = SettingsDialog.NONE },
        )
    }
}

@Composable
private fun permissionSummary(granted: Boolean): String =
    stringResource(if (granted) R.string.permission_granted else R.string.permission_not_granted)

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun timeFormatLabel(format: TimeFormatPreference): Int = when (format) {
    TimeFormatPreference.SYSTEM -> R.string.time_format_system
    TimeFormatPreference.H12 -> R.string.time_format_12
    TimeFormatPreference.H24 -> R.string.time_format_24
}

/** Opens the privacy policy URL (see `privacy_policy_url` in strings.xml) in the browser. Never crashes. */
internal fun openPrivacyPolicy(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, context.getString(R.string.privacy_policy_url).toUri())
                .addCategory(Intent.CATEGORY_BROWSABLE),
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.settings_open_failed, Toast.LENGTH_SHORT).show()
    }
}
