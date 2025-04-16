package com.example.creativecommunity

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.creativecommunity.pages.LoginPage
import com.example.creativecommunity.pages.NewPostPage
import com.example.creativecommunity.pages.MainPage
import com.example.creativecommunity.pages.CategoryFeed
import com.example.creativecommunity.pages.IndividualPostPage
import com.example.creativecommunity.pages.DiscoveryPage
import com.example.creativecommunity.pages.ProfilePage
import com.example.creativecommunity.components.BottomNavigationBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route

    if (currentRoute == "login") {
        LoginPage(navController)
    } else {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController = navController)
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") { MainPage(navController) }
                    composable("discovery") { DiscoveryPage(navController) }
                    composable("profile") { ProfilePage(navController) }
                    composable("new_post/{category}") { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
                        NewPostPage(navController, category)
                    }
                    composable("category_feed/{category}") { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
                        CategoryFeed(navController, category)
                    }
                    composable("individual_post") { IndividualPostPage(navController = navController) }
                }
            }
        }
    }
}