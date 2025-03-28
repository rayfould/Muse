package com.example.creativecommunity

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.example.creativecommunity.pages.LoginPage
import com.example.creativecommunity.pages.NewPostPage

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginPage(navController) }
        composable("new_post") { NewPostPage(navController) }
    }
}