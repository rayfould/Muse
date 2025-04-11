package com.example.creativecommunity.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
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

        // Post composable
        // Example of a Post on our community feed
        Post(
            profileImage = "https://i.imgur.com/qBPkCnP.jpeg",
            username = "Bob Ross",
            postImage = "https://i.imgur.com/NxjUBgB.jpeg",
            caption = "A Bob Ross Classic",
            likeCount = 15,
            commentCount = 8,
            onCommentClicked = {
                // Navigate to comment screen or open a bottom sheet
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

// Individual post composable:
@Composable
fun Post(
    profileImage: String,
    username: String,

    postImage: String,
    caption: String,

    likeCount: Int,
    commentCount: Int,
    onCommentClicked: () -> Unit = {} // --> create a comment....? create this action later
) {
    var liked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(likeCount) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        // Profile image and username
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            AsyncImage(
                model = profileImage,
                contentDescription = "Profile photo",
                modifier = Modifier
                    .height(40.dp)
                    .width(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = username)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Post Image
        AsyncImage(
            model = postImage,
            contentDescription = "Post image",
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Caption
        Text(text = caption, modifier = Modifier.padding(horizontal = 10.dp))
        Spacer(modifier = Modifier.height(10.dp))

        // Like, Comment Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = {
                liked = !liked
                if (liked){
                    likeCount++
                } else {
                    likeCount--
                }
            }) {
                if (liked){
                    Text("♥️ $likeCount")
                } else {
                    Text("🤍 $likeCount")
                }
            }

            // Comment button
            Button(onClick = onCommentClicked) {
                Text("💬 $commentCount")
            }
        }
    }
}