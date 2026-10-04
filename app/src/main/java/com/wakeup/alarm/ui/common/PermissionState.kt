package com.wakeup.alarm.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wakeup.alarm.util.PermissionStatus
import com.wakeup.alarm.util.PermissionUtils

/** Permission status that refreshes every time the screen comes back to the foreground (e.g. from system settings). */
@Composable
fun rememberPermissionStatus(): State<PermissionStatus> {
    val context = LocalContext.current
    val status = remember { mutableStateOf(PermissionUtils.status(context)) }
    LifecycleResumeEffect(Unit) {
        status.value = PermissionUtils.status(context)
        onPauseOrDispose { }
    }
    return status
}

/** Unwraps ContextWrappers until an Activity is found. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
