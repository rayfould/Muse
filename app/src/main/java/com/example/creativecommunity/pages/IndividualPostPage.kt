package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun IndividualPostPage(navController: NavController) {
    Column {
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
        Post(
            profileImage = "https://i.imgur.com/qBPkCnP.jpeg",
            username = "Bob Ross",
            postImage = "https://i.imgur.com/NxjUBgB.jpeg",
            caption = "A Bob Ross Classic",
            likeCount = 15,
            commentCount = 8
        )
    }
}