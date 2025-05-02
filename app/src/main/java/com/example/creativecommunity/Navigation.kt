package com.example.creativecommunity

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isWideScreen = screenWidth > 600

    // Define common transitions
    val fadeInSpec = tween<Float>(300)
    val fadeOutSpec = tween<Float>(300)
    // Define scale specs
    val scaleInSpec = tween<Float>(300)
    val scaleOutSpec = tween<Float>(300)
    // Remove unused slide specs
    // val slideInSpec = tween<androidx.compose.ui.unit.IntOffset>(300)
    // val slideOutSpec = tween<androidx.compose.ui.unit.IntOffset>(300)

    NavHost(navController = navController, startDestination = "login") {
        composable(
            "login",
            // Combine scale and fade (Shared Axis Z)
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }, // Exit slightly enlarges before fading
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) }, // Pop enter scales down from slight enlarge
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { 
            LoginPage(navController) 
        }
        
        composable(
            "main",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { 
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        MainPage(navController)
                    }
                }
            } else {
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
        }
        
        composable(
            "discovery",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { 
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        DiscoveryPage(navController)
                    }
                }
            } else {
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
        }
        
        composable(
            "profile",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { 
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        ProfilePage(navController)
                    }
                }
            } else {
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
        }
        
        composable(
            "saved_posts",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { 
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        SavedPostsPage(navController)
                    }
                }
            } else {
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
        }
        
        composable(
            "new_post/{category}",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        NewPostPage(navController, category)
                    }
                }
            } else {
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
        }
        
        composable(
            "category_feed/{category}",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Unknown"
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        CategoryFeed(navController, category)
                    }
                }
            } else {
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
        }
        
        composable(
            "individual_post/{postId}",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination

            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        IndividualPostPage(navController = navController, postId = postId)
                    }
                }
            } else {
                Scaffold(
                    bottomBar = {
                        if (currentDestination?.route?.startsWith("individual_post/") != true) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        IndividualPostPage(navController = navController, postId = postId)
                    }
                }
            }
        }
        
        composable(
            "my_posts",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { 
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        MyPostsPage(navController)
                    }
                }
            } else {
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
        }
        
        composable(
            "view_profile/{userId}",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        ViewProfilePage(navController = navController, userId = userId)
                    }
                }
            } else {
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
        }

        composable(
            "user_posts/{userId}",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            if (isWideScreen) {
                Row(modifier = Modifier.fillMaxSize()) {
                    BottomNavigationBar(navController = navController)
                    Box(modifier = Modifier.weight(1f)) {
                        UserPostsPage(navController = navController, userId = userId)
                    }
                }
            } else {
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
        }
        
        composable(
            "about_us",
            enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            exitTransition = { scaleOut(targetScale = 1.1f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) },
            popEnterTransition = { scaleIn(initialScale = 1.1f, animationSpec = scaleInSpec) + fadeIn(animationSpec = fadeInSpec) },
            popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = scaleOutSpec) + fadeOut(animationSpec = fadeOutSpec) }
        ) {
            AboutUsPage(navController)
        }
    }
}