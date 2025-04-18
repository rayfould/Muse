package com.example.creativecommunity.models

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.utils.LikeManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// Individual post composable:
// Write it here to reuse both in the feed and for individual post page
@Composable
fun Post(
    postId: Int = 0, // Change to Int with default value 0
    profileImage: String,
    username: String,
    postImage: String,
    caption: String,
    likeCount: Int, // Initial like count (only used if we can't fetch from DB)
    commentCount: Int,
    onCommentClicked: () -> Unit = {}, // --> create a comment....? create this action later
    onProfileClick: () -> Unit = {} // Add this parameter
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Get the LikeManager singleton instance
    val likeManager = remember { LikeManager.getInstance(context) }
    
    // Get current user
    val currentUser = remember { SupabaseClient.client.auth.currentUserOrNull() }
    val userId = remember { currentUser?.id ?: "" }
    
    // State for likes
    var isLiked by remember { mutableStateOf(false) }
    var currentLikeCount by remember { mutableIntStateOf(0) } // Start at 0 and load from DB
    var hasLoadedInitialState by remember { mutableStateOf(false) }

    // Initial data loading
    LaunchedEffect(userId, postId) {
        if (userId.isNotEmpty() && postId > 0) {
            // Get actual like count first
            val count = likeManager.getLikeCount(postId)
            currentLikeCount = count
            
            // Then check if user has liked this post
            isLiked = likeManager.isPostLikedByUser(userId, postId)
            
            hasLoadedInitialState = true
        }
    }
    
    // Listen for real-time like updates
    if (postId > 0) {
        val likeUpdates by likeManager.likeUpdates.collectAsState(initial = null)
        
        LaunchedEffect(likeUpdates) {
            // Only process updates after we've loaded the initial state
            if (hasLoadedInitialState) {
                likeUpdates?.let { update ->
                    if (update.postId == postId) {
                        // Only adjust count for updates from other users
                        // Our own updates are handled optimistically
                        val isOwnUpdate = update.ownUpdate
                        if (!isOwnUpdate) {
                            // Adjust like count based on real-time updates
                            currentLikeCount += if (update.isLiked) 1 else -1
                        }
                    }
                }
            }
        }
    }
    
    // Handle like button press
    val toggleLike = {
        if (userId.isNotEmpty() && postId > 0) {
            val newLikedState = !isLiked
            isLiked = newLikedState
            
            // Update like count optimistically
            currentLikeCount += if (newLikedState) 1 else -1
            
            // Queue the like/unlike for batch processing
            coroutineScope.launch {
                likeManager.queueLike(userId, postId, newLikedState)
            }
        }
    }
    
    // Flush pending likes when the component is disposed
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                likeManager.flush()
            }
        }
    }

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
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
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
            Button(onClick = toggleLike) {
                if (isLiked) {
                    Text("♥️ $currentLikeCount")
                } else {
                    Text("🤍 $currentLikeCount")
                }
            }

            // Comment button
            Button(onClick = onCommentClicked) {
                Text("💬 $commentCount")
            }
        }
    }
}