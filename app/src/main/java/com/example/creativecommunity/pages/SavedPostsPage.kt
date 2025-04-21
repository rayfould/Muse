package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.Post
import com.example.creativecommunity.models.UserInfo
import com.example.creativecommunity.models.SavedPost
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.DisposableEffect
import kotlinx.serialization.Serializable

@Serializable
data class PostEntity(
    val id: Int,
    val user_id: Int,
    val prompt_id: Int? = null,
    val image_url: String,
    val caption: String? = null,
    val content: String? = null,
    val created_at: String? = null,
    val category: String? = null
)

@Serializable
data class UserEntity(
    val id: Int,
    val username: String,
    val profile_image: String? = null
)

@Serializable
data class LikeEntity(
    val id: Int,
    val user_id: Int,
    val post_id: Int
)

@Serializable
data class CommentEntity(
    val id: Int,
    val user_id: Int,
    val post_id: Int,
    val content: String
)

data class PostData(
    val id: Int,
    val user_id: Int,
    val prompt_id: Int?,
    val image_url: String,
    val caption: String,
    val created_at: String,
    val username: String = "",
    val profile_image: String = "",
    var like_count: Int = 0,
    var comment_count: Int = 0
)

@Composable
fun SavedPostsPage(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedPosts by remember { mutableStateOf<List<PostData>>(emptyList()) }
    
    // Add refresh counter that increments when screen is focused
    val refreshCounter = remember { mutableStateOf(0) }
    
    // Use DisposableEffect to detect when screen is focused/unfocused
    DisposableEffect(Unit) {
        // This could be replaced with actual navigation listener in a real app
        // For now we'll just refresh once when the screen loads
        refreshCounter.value++
        
        onDispose { /* cleanup if needed */ }
    }
    
    // Helper function to get the integer user ID from auth ID
    suspend fun getUserIdFromAuth(authId: String): Int? {
        return try {
            Log.d("SavedPosts", "Looking up user ID for auth ID: $authId")
            val client = SupabaseClient.client
            val response = client.postgrest["users"]
                .select {
                    filter {
                        eq("auth_id", authId)
                    }
                }
            
            val users = response.decodeList<UserInfo>()
            Log.d("SavedPosts", "Found ${users.size} users for auth ID: $authId")
            
            if (users.isNotEmpty()) {
                val userId = users.first().id
                Log.d("SavedPosts", "User ID for auth ID $authId is: $userId")
                userId
            } else {
                Log.e("SavedPosts", "No user found for auth ID: $authId")
                null
            }
        } catch (e: Exception) {
            Log.e("SavedPosts", "Error getting user ID for auth ID $authId: ${e.message}", e)
            null
        }
    }
    
    // Helper function to load saved posts
    suspend fun loadSavedPosts(callback: (List<PostData>, String?) -> Unit) {
        try {
            val client = SupabaseClient.client
            val authId = client.auth.currentUserOrNull()?.id ?: ""
            
            if (authId.isNotEmpty()) {
                val userId = getUserIdFromAuth(authId)
                
                if (userId != null) {
                    Log.d("SavedPosts", "Found numeric user ID: $userId for auth ID: $authId")
                    
                    // Get saved post IDs
                    val savedPostsResponse = client.postgrest["saved_posts"]
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }
                    
                    val savedPostsEntries = savedPostsResponse.decodeList<SavedPost>()
                    Log.d("SavedPosts", "Found ${savedPostsEntries.size} saved posts")
                    
                    if (savedPostsEntries.isEmpty()) {
                        callback(emptyList(), null)
                        return
                    }
                    
                    val postIds = savedPostsEntries.map { it.post_id }
                    Log.d("SavedPosts", "Post IDs: $postIds")
                    
                    // Get the actual posts
                    val postsResponse = client.postgrest["posts"]
                        .select {
                            filter {
                                if (postIds.isNotEmpty()) {
                                    eq("id", postIds.first())
                                    
                                    // Add OR conditions for additional post IDs
                                    for (postId in postIds.drop(1)) {
                                        or {
                                            eq("id", postId)
                                        }
                                    }
                                }
                            }
                        }
                    
                    val posts = postsResponse.decodeList<PostEntity>()
                    Log.d("SavedPosts", "Found ${posts.size} posts")
                    
                    // Debug post structure
                    posts.forEachIndexed { index, post ->
                        Log.d("SavedPosts", "Post $index structure: id=${post.id}, user_id=${post.user_id}, " +
                                "caption=${post.caption}, content=${post.content}, image_url=${post.image_url}")
                    }
                    
                    // Process each post to get user data and counts
                    val postsWithUserData = posts.map { post ->
                        val postId = post.id
                        val postUserId = post.user_id
                        
                        // Get user info
                        val userResponse = client.postgrest["users"]
                            .select {
                                filter {
                                    eq("id", postUserId)
                                }
                            }
                        
                        val userData = userResponse.decodeList<UserEntity>().firstOrNull()
                        
                        // Get like count
                        val likeCountResponse = client.postgrest["likes"]
                            .select {
                                filter {
                                    eq("post_id", postId)
                                }
                            }
                        val likeList = likeCountResponse.decodeList<LikeEntity>()
                        val likeCount = likeList.size
                        
                        // Get comment count
                        val commentCountResponse = client.postgrest["comments"]
                            .select {
                                filter {
                                    eq("post_id", postId)
                                }
                            }
                        val commentList = commentCountResponse.decodeList<CommentEntity>()
                        val commentCount = commentList.size
                        
                        // Use caption or content as the text to display
                        val postText = post.caption ?: post.content ?: ""
                        
                        PostData(
                            id = postId,
                            user_id = postUserId,
                            prompt_id = post.prompt_id,
                            image_url = post.image_url,
                            caption = postText,
                            created_at = post.created_at ?: "",
                            username = userData?.username ?: "Unknown User",
                            profile_image = userData?.profile_image ?: "",
                            like_count = likeCount,
                            comment_count = commentCount
                        )
                    }
                    
                    callback(postsWithUserData, null)
                } else {
                    Log.e("SavedPosts", "Could not find user ID for auth ID: $authId")
                    callback(emptyList(), "Could not find your user account.")
                }
            } else {
                Log.e("SavedPosts", "No auth ID available")
                callback(emptyList(), "Please sign in to view saved posts.")
            }
        } catch (e: Exception) {
            Log.e("SavedPosts", "Error loading saved posts: ${e.message}", e)
            callback(emptyList(), "Failed to load saved posts: ${e.message}")
        }
    }
    
    // Load saved posts whenever refresh counter changes
    LaunchedEffect(refreshCounter.value) {
        isLoading = true
        error = null
        
        loadSavedPosts { posts, errorMessage ->
            savedPosts = posts
            error = errorMessage
            isLoading = false
            
            Log.d("SavedPosts", "Loaded ${posts.size} saved posts")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            savedPosts.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "Saved Posts",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    Text(
                        text = "You haven't saved any posts yet.",
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
            else -> {
                LazyColumn {
                    item {
                        Text(
                            text = "Saved Posts",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    
                    items(savedPosts) { post ->
                        Post(
                            postId = post.id,
                            profileImage = post.profile_image,
                            username = post.username,
                            postImage = post.image_url,
                            caption = post.caption,
                            likeCount = post.like_count,
                            commentCount = post.comment_count,
                            onCommentClicked = {
                                navController.navigate("individual_post/${post.id}")
                            },
                            onProfileClick = {
                                // TODO: Navigate to user profile
                            }
                        )
                    }
                }
            }
        }
    }
} 