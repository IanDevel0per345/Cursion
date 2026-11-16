package com.opencell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opencell.app.ui.DiagnosticsScreen
import com.opencell.app.ui.HistoryScreen
import com.opencell.app.ui.HomeScreen
import com.opencell.app.ui.OnboardingScreen
import com.opencell.app.ui.OpenCellTheme
import com.opencell.app.ui.PermissionsScreen
import com.opencell.app.ui.ProvidersScreen
import com.opencell.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = (application as OpenCellApp).container
        setContent {
            OpenCellTheme(c) {
                val nav = rememberNavController()
                val s by c.settings.collectAsState()
                val start = if (s.onboardingDone) "home" else "onboarding"
                NavHost(nav, startDestination = start) {
                    composable("onboarding") {
                        OnboardingScreen(c, openProviders = { nav.navigate("providers") }, onDone = { nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } })
                    }
                    composable("home") { HomeScreen(c) { nav.navigate(it) } }
                    composable("providers") { ProvidersScreen(c) { nav.popBackStack() } }
                    composable("permissions") { PermissionsScreen(c) { nav.popBackStack() } }
                    composable("diagnostics") { DiagnosticsScreen(c) { nav.popBackStack() } }
                    composable("settings") { SettingsScreen(c) { nav.popBackStack() } }
                    composable("history") { HistoryScreen(c, null, { nav.navigate(it) }) { nav.popBackStack() } }
                    composable("history/{id}", listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                        HistoryScreen(c, entry.arguments?.getString("id"), { nav.navigate(it) }) { nav.popBackStack() }
                    }
                }
            }
        }
    }
}
