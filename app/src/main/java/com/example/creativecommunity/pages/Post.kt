package com.example.creativecommunity.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// Individual post composable:
// Write it here to reuse both in the feed and for individual post page
@Composable
fun Post(
    profileImage: String,
    username: String,
    postImage: String,
    caption: String,
    likeCount: Int,
    commentCount: Int,
    onCommentClicked: () -> Unit = {}, // --> create a comment....? create this action later
    onProfileClick: () -> Unit = {} // Add this parameter
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
                    .clickable { onProfileClick() }
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