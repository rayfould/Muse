package com.example.creativecommunity

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.creativecommunity.components.BottomNavigationBar
import com.example.creativecommunity.pages.AboutUsPage
import com.example.creativecommunity.pages.CategoryFeed
import com.example.creativecommunity.pages.DiscoveryPage
import com.example.creativecommunity.pages.IndividualPostPage
import com.example.creativecommunity.pages.LoginPage
import com.example.creativecommunity.pages.MainPage
import com.example.creativecommunity.pages.MyPostsPage
import com.example.creativecommunity.pages.NewPostPage
import com.example.creativecommunity.pages.ProfilePage
import com.example.creativecommunity.pages.SavedPostsPage
import com.example.creativecommunity.pages.UserPostsPage
import com.example.creativecommunity.pages.ViewProfilePage

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { 
            LoginPage(navController) 
        }
        
        composable("main") { 
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    MainPage(navController)
                }
            }
        }
        
        composable("discovery") { 
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    DiscoveryPage(navController)
                }
            }
        }
        
        composable("profile") { 
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    ProfilePage(navController)
                }
            }
        }
        
        composable("saved_posts") { 
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    SavedPostsPage(navController)
                }
            }
        }
        
        composable("new_post/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    NewPostPage(navController, category)
                }
            }
        }
        
        composable("category_feed/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    CategoryFeed(navController, category)
                }
            }
        }
        
        composable("individual_post/{postId}") { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    IndividualPostPage(navController = navController, postId = postId)
                }
            }
        }
        
        composable("my_posts") { 
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    MyPostsPage(navController)
                }
            }
        }
        
        // New route for viewing other users' profiles
        composable("view_profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    ViewProfilePage(navController = navController, userId = userId)
                }
            }
        }

        // New route for viewing posts by a specific user
        composable("user_posts/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    UserPostsPage(navController = navController, userId = userId)
                }
            }
        }
        
        // Add the route for AboutUsPage
        composable("about_us") {
            // About Us doesn't need the bottom nav bar, so no Scaffold here typically
            AboutUsPage(navController)
        }
    }
}