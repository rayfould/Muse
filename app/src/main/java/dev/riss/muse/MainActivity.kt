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

// Commented out code below - kept for reference if needed later

// // Since we have to pass in arguments through navigation, we have to redo routes - this is in Navigation.kt
// 
// //Composable to allow us to navigate through the app.
// //@Composable
// //fun AppNavigation() {
// //    val navController = rememberNavController()
// //    NavHost(navController = navController, startDestination = "login") {
// //        composable("login") {
// //            LoginPage(navController = navController)
// //        }
// //        composable("main") {
// //            MainPage(navController = navController)
// //        }
// //        composable("new_post") {
// //            NewPostPage(navController = navController)
// //        }
// //    }
// //}
