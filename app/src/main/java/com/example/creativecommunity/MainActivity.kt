package com.example.creativecommunity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
// Import for SplashScreen API
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen 
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.creativecommunity.pages.LoginPage
import com.example.creativecommunity.pages.MainPage
import com.example.creativecommunity.pages.NewPostPage
import com.example.creativecommunity.ui.theme.CreativeCommunityTheme
import com.example.creativecommunity.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Call installSplashScreen() *before* super.onCreate()
        installSplashScreen() 
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreativeCommunityTheme {
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
