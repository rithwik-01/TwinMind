package com.twinmind.recorder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.twinmind.recorder.ui.dashboard.DashboardScreen
import com.twinmind.recorder.ui.recording.RecordingScreen
import com.twinmind.recorder.ui.summary.SummaryScreen

/**
 * Navigation destinations for the app.
 * Dashboard is the start destination; recording and summary take a sessionId argument.
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")

    object Recording : Screen("recording/{sessionId}") {
        fun createRoute(sessionId: String) = "recording/$sessionId"
    }

    object Summary : Screen("summary/{sessionId}") {
        fun createRoute(sessionId: String) = "summary/$sessionId"
    }
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController  = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        composable(
            route = Screen.Recording.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            RecordingScreen(
                navController = navController,
                sessionId     = sessionId
            )
        }

        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            SummaryScreen(
                navController = navController,
                sessionId     = sessionId
            )
        }
    }
}