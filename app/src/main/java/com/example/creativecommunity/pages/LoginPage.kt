package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginPage(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        // Placeholder text and button for navigation testing
        Text("Login Page")
        Button(onClick = {
            // Adds "New_Post" to the "back stack" - pages act as a stack, starts off as just Login Page
            // Then new post page is added to the stack once this button is pressed
            navController.navigate("new_post")
        }) {
            Text("New Post Page")
        }
    }
}
