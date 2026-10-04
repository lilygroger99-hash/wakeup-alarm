package com.wakeup.alarm.ui.common

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wakeup.alarm.container
import com.wakeup.alarm.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches work in the ViewModel scope and logs (instead of crashing on) unexpected storage errors.
 * Cancellation is rethrown so coroutines still cancel correctly.
 */
fun ViewModel.launchSafely(block: suspend CoroutineScope.() -> Unit): Job = viewModelScope.launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("WakeUp", "Operation failed", e)
    }
}

/**
 * Creates (or returns the existing) ViewModel for the current navigation entry, building it from the app's
 * [AppContainer]. This is the app's tiny replacement for a DI framework.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline build: (AppContainer) -> VM,
): VM {
    val appContainer = LocalContext.current.container
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = build(appContainer) as T
        },
    )
}
