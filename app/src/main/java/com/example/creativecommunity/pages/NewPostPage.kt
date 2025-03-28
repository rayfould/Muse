package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewPostPage(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        // Placeholder text and button for navigation testing
        Text("New Post Page")
        Button(onClick = {
            // Acts as a "back" button, pops this page off the stack
            // The login page is defaulted as the starting page in the stack, goes back to the Login page
            navController.popBackStack()
        }) {
            Text("Login Page")
        }
    }
}
