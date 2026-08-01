package com.kamsiob.healthtrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kamsiob.healthtrail.ui.AppRoot
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme
import com.kamsiob.healthtrail.ui.theme.ThemeSetting
import com.kamsiob.healthtrail.ui.theme.isDark

/**
 * The single activity. Every screen in the app is a composable inside it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val setting = remember(context) { ThemeSetting(context) }

            // **Held above the theme, which is the whole reason it works.**
            // State read inside `HealthTrailTheme` could not change the theme
            // that is reading it, so choosing dark would need a restart, and a
            // setting that needs a restart reads as one that did not take.
            //
            // Read from disk here rather than kept across process death, so
            // there is exactly one durable copy of the value and this is the
            // live one.
            var choice by remember(setting) { mutableStateOf(setting.read()) }

            HealthTrailTheme(darkTheme = choice.isDark()) {
                AppRoot(
                    themeChoice = choice,
                    onThemeChoice = { chosen ->
                        // Recomposes immediately, then persists.
                        choice = chosen
                        setting.write(chosen)
                    },
                )
            }
        }
    }
}
