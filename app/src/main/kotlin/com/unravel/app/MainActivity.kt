package com.unravel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.unravel.app.navigation.UnravelNavHost
import com.unravel.core.designsystem.theme.UnravelTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Install Android Core Splashscreen before super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 2. Enable modern edge-to-edge with transparent system bars
        enableEdgeToEdge()

        // 3. Set content wrapped in dark-mode obsidian UnravelTheme
        setContent {
            UnravelTheme {
                UnravelNavHost()
            }
        }
    }
}
