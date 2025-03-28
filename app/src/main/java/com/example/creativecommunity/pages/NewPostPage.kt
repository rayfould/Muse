package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun NewPostPage(navController: NavController) {
    var postCaption by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("New Post Page")
        Text("This week's challenge: Paint a park near you!")
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Take a photo button
            Button(
                onClick = { /* take a photo */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Take a photo")
            }
            Text("or")
            Button(
                onClick = { /* upload a photo */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Upload a photo")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Caption input text field
        TextField(
            value = postCaption,
            onValueChange = { postCaption = it },
            label = { Text("Write your caption here...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        // Post button
        Button(
            onClick = { /* Submit the post --> integrate with Supabase */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Post to the Community!")
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = {
            // Acts as a "back" button, pops this page off the stack
            // The login page is defaulted as the starting page in the stack, goes back to the Login page
            navController.popBackStack()
        }) {
            Text("Login Page")
        }
    }
}
