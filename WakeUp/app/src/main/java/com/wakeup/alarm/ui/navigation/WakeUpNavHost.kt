package com.wakeup.alarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wakeup.alarm.ui.edit.EditAlarmScreen
import com.wakeup.alarm.ui.home.HomeScreen
import com.wakeup.alarm.ui.settings.AboutScreen
import com.wakeup.alarm.ui.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val ARG_ALARM_ID = "alarmId"
    const val EDIT = "edit/{$ARG_ALARM_ID}"

    /** Id 0 means "create a new alarm". */
    fun edit(alarmId: Long) = "edit/$alarmId"
}

@Composable
fun WakeUpNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddAlarm = { navController.navigate(Routes.edit(0L)) },
                onEditAlarm = { id -> navController.navigate(Routes.edit(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument(Routes.ARG_ALARM_ID) { type = NavType.LongType }),
        ) { entry ->
            val alarmId = entry.arguments?.getLong(Routes.ARG_ALARM_ID) ?: 0L
            EditAlarmScreen(alarmId = alarmId, onClose = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
