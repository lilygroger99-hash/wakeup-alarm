package com.wakeup.alarm.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakeup.alarm.R
import com.wakeup.alarm.ui.common.Formatters
import com.wakeup.alarm.ui.common.RowDivider
import com.wakeup.alarm.ui.common.SettingsGroup
import com.wakeup.alarm.ui.common.SettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The launcher artwork on its brand-coloured background.
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.about_icon_description, appName),
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF1E2A5A)),
            )
            Text(text = appName, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.settings_version) + " " + Formatters.appVersionName(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.about_description, appName),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.about_developer_label),
                    summary = stringResource(R.string.about_developer_value),
                )
                RowDivider()
                SettingsRow(
                    title = stringResource(R.string.settings_version),
                    summary = Formatters.appVersionName(context),
                )
            }
            OutlinedButton(
                onClick = { openPrivacyPolicy(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.settings_privacy))
            }
        }
    }
}
