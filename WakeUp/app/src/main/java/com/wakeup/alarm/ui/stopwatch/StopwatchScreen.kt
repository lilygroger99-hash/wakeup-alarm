package com.wakeup.alarm.ui.stopwatch

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.alarm.R
import com.wakeup.alarm.container
import com.wakeup.alarm.domain.StopwatchFormat
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    val controller = LocalContext.current.container.stopwatch
    val state by controller.state.collectAsStateWithLifecycle()
    val running = state.isRunning

    // Redraw about 30 times a second, only while running.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) {
        now = SystemClock.elapsedRealtime()
        while (running) {
            delay(30)
            now = SystemClock.elapsedRealtime()
        }
    }

    // Keep the screen on while timing, so the stopwatch can be watched.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stopwatch_title)) },
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = StopwatchFormat.format(state.elapsed(now)),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val buttonModifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                when {
                    running -> FilledTonalButton(onClick = controller::lap, modifier = buttonModifier) {
                        Text(stringResource(R.string.stopwatch_lap))
                    }
                    state.hasTime -> OutlinedButton(onClick = controller::reset, modifier = buttonModifier) {
                        Text(stringResource(R.string.stopwatch_reset))
                    }
                    else -> FilledTonalButton(onClick = {}, enabled = false, modifier = buttonModifier) {
                        Text(stringResource(R.string.stopwatch_lap))
                    }
                }
                Button(
                    onClick = if (running) controller::pause else controller::start,
                    modifier = buttonModifier,
                ) {
                    Text(
                        stringResource(
                            when {
                                running -> R.string.stopwatch_pause
                                state.hasTime -> R.string.stopwatch_resume
                                else -> R.string.stopwatch_start
                            },
                        ),
                    )
                }
            }

            if (state.laps.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                LapHeader()
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Newest lap on top.
                    items(items = state.laps.indices.reversed().toList(), key = { it }) { index ->
                        LapRow(
                            number = index + 1,
                            lapTime = StopwatchFormat.format(state.lapDuration(index)),
                            total = StopwatchFormat.format(state.laps[index]),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LapHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { heading() },
    ) {
        Text(
            text = stringResource(R.string.stopwatch_lap),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.8f),
        )
        Text(
            text = stringResource(R.string.stopwatch_lap_time),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.stopwatch_total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LapRow(number: Int, lapTime: String, total: String) {
    val mono = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.stopwatch_lap_number, number),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.8f),
        )
        Text(text = lapTime, style = mono, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(
            text = total,
            style = mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
