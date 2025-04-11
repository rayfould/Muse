package com.example.creativecommunity.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.navigation.NavController


@Composable
fun CategoryFeed(navController: NavController, category: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "$category Community!",
            modifier = Modifier.padding(top = 30.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "This week's prompt: Paint a park near you!")

        // Post composable
        // Example of a Post on our community feed
        Post(
            profileImage = "https://i.imgur.com/qBPkCnP.jpeg",
            username = "Bob Ross",
            postImage = "https://i.imgur.com/NxjUBgB.jpeg",
            caption = "A Bob Ross Classic",
            likeCount = 15,
            commentCount = 8,

            // Hardcoded this for now to test functionality - Would pass in Post ID for example
            onCommentClicked = {
                navController.navigate("individual_post")
            }
        )
        //

        Row(){
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(vertical = 4.dp)
            ) {
                Text("All Communities")
            }
            Spacer(modifier = Modifier.width(50.dp))
            Button(
                onClick = {
                    navController.navigate("new_post/${category}")
                },
                modifier = Modifier
                    .padding(vertical = 5.dp)
            ) {
                Text("+")
            }
        }
    }
}

