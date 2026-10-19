package com.unravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.unravel.app.navigation.UnravelNavHost
import com.unravel.core.designsystem.theme.UnravelTheme

import android.content.Intent
import com.unravel.app.navigation.RaceModeRoute
import com.unravel.app.navigation.SandboxRoute
import com.unravel.app.navigation.SkillMapRoute
import com.unravel.app.navigation.UnravelRoute
import androidx.compose.runtime.remember

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private val currentIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Android Core Splashscreen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. Enable modern edge-to-edge with transparent system bars
        enableEdgeToEdge()

        currentIntentState.value = intent

        // 3. Set content wrapped in dark-mode obsidian UnravelTheme
        setContent {
            UnravelTheme {
                val activeIntent by currentIntentState
                val initialRoute = remember(activeIntent) {
                    parseInitialRoute(activeIntent)
                }
                UnravelNavHost(initialRoute = initialRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
    }

    private fun parseInitialRoute(intent: Intent?): UnravelRoute {
        val algoId = intent?.getStringExtra("algorithmId")
        val resumeSession = intent?.getBooleanExtra("resumeSession", false) ?: false
        val routeType = intent?.getStringExtra("route")

        return when {
            algoId != null -> SandboxRoute(algorithmId = algoId, resumeSession = resumeSession)
            routeType == "race_mode" -> RaceModeRoute()
            else -> SkillMapRoute
        }
    }
}
