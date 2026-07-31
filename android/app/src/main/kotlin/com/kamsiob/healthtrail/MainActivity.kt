package com.kamsiob.healthtrail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kamsiob.healthtrail.ui.AppRoot
import com.kamsiob.healthtrail.ui.theme.HealthTrailTheme

/**
 * The single activity. Every screen in the app is a composable inside it.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HealthTrailTheme {
                AppRoot()
            }
        }
    }
}
