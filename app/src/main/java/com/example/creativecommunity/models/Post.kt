package com.example.creativecommunity.models

import android.util.Log
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.utils.LikeManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import androidx.navigation.NavController
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape

@Serializable
data class UserInfo(
    val id: Int,
    val username: String,
    val email: String,
    val profile_image: String? = null,
    val bio: String? = null,
    val auth_id: String
)

@Serializable
data class SavedPost(
    val id: Int? = null,
    val auth_id: String,
    @SerialName("user_id") val userId: Int,
    val post_id: Int,
    val created_at: String? = null
)

// Helper function to get the integer user ID from auth ID
private suspend fun getUserIdFromAuth(authId: String): Int? {
    return try {
        Log.d("SavePost", "Looking up user ID for auth ID: $authId")
        val client = SupabaseClient.client
        val response = client.postgrest["users"]
            .select {
                filter {
                    eq("auth_id", authId)
                }
            }
        
        try {
            val users = response.decodeList<UserInfo>()
            Log.d("SavePost", "Found ${users.size} users for auth ID: $authId")
            
            if (users.isNotEmpty()) {
                val userId = users.first().id
                Log.d("SavePost", "User ID for auth ID $authId is: $userId")
                userId
            } else {
                Log.e("SavePost", "No user found for auth ID: $authId")
                null
            }
        } catch (e: Exception) {
            // If we can't decode as UserInfo, just log the error
            Log.e("SavePost", "Error decoding as UserInfo: ${e.message}", e)
            null
        }
    } catch (e: Exception) {
        Log.e("SavePost", "Error getting user ID for auth ID $authId: ${e.message}", e)
        null
    }
}

// Individual post composable:
// Write it here to reuse both in the feed and for individual post page
@Composable
fun Post(
    navController: NavController,
    authorId: String,
    postId: Int = 0,
    profileImage: String,
    username: String,
    postImage: String,
    caption: String,
    likeCount: Int,
    commentCount: Int,
    onCommentClicked: () -> Unit = {},
    onImageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Get the LikeManager singleton instance
    val likeManager = remember { LikeManager.getInstance(context) }
    
    // Get current user
    val currentUser = remember { SupabaseClient.client.auth.currentUserOrNull() }
    val currentUserId = remember { currentUser?.id ?: "" }
    
    // State for likes
    var isLiked by remember { mutableStateOf(false) }
    var currentLikeCount by remember { mutableIntStateOf(0) } // Start at 0 and load from DB
    var hasLoadedInitialState by remember { mutableStateOf(false) }
    
    // State for saves
    var isSaved by remember { mutableStateOf(false) }
    
    // Track if we need to refresh save status
    val refreshKey = remember { mutableStateOf(0) }
    
    // Initial data loading with refreshKey
    LaunchedEffect(currentUserId, postId, refreshKey.value) {
        if (currentUserId.isNotEmpty() && postId > 0) {
            // Get actual like count first
            val likeCountValue = likeManager.getLikeCount(postId)
            currentLikeCount = likeCountValue
            
            // Then check if user has liked this post
            isLiked = likeManager.isPostLikedByUser(currentUserId, postId)
            
            // Check if post is saved
            try {
                val client = SupabaseClient.client
                
                // Get numeric user ID first
                val numericUserId = getUserIdFromAuth(currentUserId)
                if (numericUserId != null) {
                    Log.d("SavePost", "Checking save status for post $postId with user_id $numericUserId")
                    
                    // Use user_id column
                    val response = client.postgrest["saved_posts"]
                        .select {
                            filter {
                                eq("user_id", numericUserId)
                                eq("post_id", postId)
                            }
                        }
                    
                    val savedPostsList = response.decodeList<SavedPost>()
                    
                    // Set the state based on query results
                    val wasSavedBefore = isSaved
                    isSaved = savedPostsList.isNotEmpty()
                    
                    Log.d("SavePost", "Initial check - Post $postId is ${if (isSaved) "saved" else "not saved"} for user_id $numericUserId (found ${savedPostsList.size} entries)")
                    
                    if (wasSavedBefore != isSaved) {
                        Log.d("SavePost", "Save status changed from $wasSavedBefore to $isSaved")
                    }
                } else {
                    Log.e("SavePost", "Could not find numeric user ID for auth_id $currentUserId")
                }
            } catch (e: Exception) {
                Log.e("SavePost", "Error checking if post is saved: ${e.message}", e)
            }
            
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
        if (currentUserId.isNotEmpty() && postId > 0) {
            val newLikedState = !isLiked
            isLiked = newLikedState
            
            // Update like count optimistically
            currentLikeCount += if (newLikedState) 1 else -1
            
            // Queue the like/unlike for batch processing
            coroutineScope.launch {
                likeManager.queueLike(currentUserId, postId, newLikedState)
            }
        }
    }
    
    // Handle save button press
    val toggleSave = {
        if (currentUserId.isNotEmpty() && postId > 0) {
            val newSavedState = !isSaved
            isSaved = newSavedState
            
            Log.d("SavePost", "Attempting to ${if (newSavedState) "save" else "unsave"} post $postId for auth_id $currentUserId")
            
            coroutineScope.launch {
                try {
                    val client = SupabaseClient.client
                    
                    // Get numeric user ID first
                    val numericUserId = getUserIdFromAuth(currentUserId)
                    if (numericUserId == null) {
                        Log.e("SavePost", "Could not find numeric user ID for auth_id $currentUserId")
                        isSaved = !newSavedState // Revert UI state
                        return@launch
                    }
                    
                    if (newSavedState) {
                        // Save the post - first check if it already exists
                        Log.d("SavePost", "Checking if post $postId is already saved by user $numericUserId")
                        
                        val existingResponse = client.postgrest["saved_posts"]
                            .select {
                                filter {
                                    eq("user_id", numericUserId)
                                    eq("post_id", postId)
                                }
                            }
                        
                        val existingSaves = existingResponse.decodeList<SavedPost>()
                        
                        if (existingSaves.isNotEmpty()) {
                            Log.d("SavePost", "Post $postId is already saved by user $numericUserId, skipping insert")
                        } else {
                            // Post is not already saved, proceed with insert
                            Log.d("SavePost", "Inserting into saved_posts: auth_id=$currentUserId, user_id=$numericUserId, post_id=$postId")
                            
                            val savedPost = SavedPost(
                                auth_id = currentUserId,
                                userId = numericUserId,
                                post_id = postId
                            )
                            
                            val result = client.postgrest["saved_posts"].insert(savedPost)
                            Log.d("SavePost", "Insert result: $result")
                        }
                    } else {
                        // Unsave the post - use either auth_id or user_id depending on your database constraints
                        Log.d("SavePost", "Deleting from saved_posts: auth_id=$currentUserId, post_id=$postId")
                        
                        val result = client.postgrest["saved_posts"].delete {
                            filter {
                                eq("user_id", numericUserId)
                                eq("post_id", postId)
                            }
                        }
                        Log.d("SavePost", "Delete result: $result")
                    }
                    
                    // Increment the refresh key to force LaunchedEffect to run again
                    refreshKey.value++
                    
                } catch (e: Exception) {
                    // Handle error - revert UI state
                    Log.e("SavePost", "Error saving/unsaving post: ${e.message}", e)
                    isSaved = !newSavedState
                }
            }
        } else {
            Log.w("SavePost", "Cannot save: currentUserId empty or postId <= 0. currentUserId=$currentUserId, postId=$postId")
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

    // Click handler for profile navigation
    val navigateToProfile = {
        // Prevent navigation if authorId is missing (should not happen ideally)
        if (authorId.isNotEmpty()) {
            navController.navigate("user/$authorId")
        } else {
            Log.w("PostComposable", "Attempted to navigate to profile but authorId is empty.")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 0.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
            // Profile image and username
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clickable { navigateToProfile() }
            ) {
                AsyncImage(
                    model = profileImage,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .height(40.dp)
                        .width(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Post Image with shadow
            AsyncImage(
                model = postImage,
                contentDescription = "Post image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .shadow(4.dp, shape = RoundedCornerShape(14.dp))
                    .clickable { onImageClick() },
                contentScale = ContentScale.Crop
            )

            // Caption
            if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 8.dp))

            // Like, Comment, Save Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { toggleLike() }) {
                        if (isLiked) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Liked", tint = Color.Red)
                        } else {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = Color.Gray)
                        }
                    }
                    Text(
                        text = "$currentLikeCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onCommentClicked() }) {
                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Comments", tint = Color.Gray)
                    }
                    Text(
                        text = "$commentCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { toggleSave() }) {
                    if (isSaved) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Saved", tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Save", tint = Color.Gray)
                    }
                }
            }
        }
    }
}