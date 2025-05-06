package dev.riss.muse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// Import for SplashScreen API
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.riss.muse.ui.theme.MuseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call installSplashScreen() *before* super.onCreate()
        installSplashScreen() 
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme {
                AppNavigation()
            }
        }
    }
}