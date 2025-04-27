package com.example.creativecommunity.models

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextButton


@Composable
fun Comment(
    profileImage: String,
    username: String,
    commentText: String,
    onReplyClicked: (() -> Unit)? = null,
    indentLevel: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 24).dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
    ) {
        AsyncImage(
            model = profileImage,
            contentDescription = "Profile picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(text = username)
            Spacer(modifier = Modifier.height(5.dp))
            Text(text = commentText)
            if (onReplyClicked != null) {
                androidx.compose.material3.TextButton(onClick = onReplyClicked) {
                    Text("Reply")
                }
            }
        }
    }
}