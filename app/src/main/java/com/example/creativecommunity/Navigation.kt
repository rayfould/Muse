package com.example.creativecommunity

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.creativecommunity.pages.LoginPage
import com.example.creativecommunity.pages.NewPostPage
import com.example.creativecommunity.pages.MainPage

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginPage(navController) }
        composable("main") { MainPage(navController) } // added this since rearranging project for passing arguments into routes
        composable("new_post/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
            NewPostPage(navController, category)
        }
//        composable("new_post") { NewPostPage(navController) }
    }
}