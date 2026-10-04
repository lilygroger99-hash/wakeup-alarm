package com.wakeup.alarm

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakeup.alarm.data.settings.AppSettings
import com.wakeup.alarm.ui.navigation.WakeUpNavHost
import com.wakeup.alarm.ui.theme.WakeUpTheme
import com.wakeup.alarm.ui.theme.isDark

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            val dark = settings.themeMode.isDark()

            // Keep status/navigation bar icon colours readable when the in-app theme differs from the system theme.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }

            WakeUpTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WakeUpNavHost()
                }
            }
        }
    }
}
