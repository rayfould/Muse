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
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class MyPostEntity(
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
data class PostUserData(
    val profile_image: String? = null,
    val username: String
)

@Serializable
data class LikeData(
    val id: String? = null,
    val user_id: Int? = null,
    val post_id: Int? = null,
    val created_at: String? = null,
    val auth_id: String? = null
)

@Serializable
data class CommentData(
    val id: String? = null,
    val user_id: Int? = null,
    val post_id: Int? = null,
    val content: String? = null,
    val created_at: String? = null,
    val auth_id: String? = null
)

data class MyPostData(
    val id: Int,
    val image_url: String,
    val caption: String,
    val username: String,
    val profile_image: String,
    var like_count: Int = 0,
    var comment_count: Int = 0
)

@Composable
fun MyPostsPage(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var myPosts by remember { mutableStateOf<List<MyPostData>>(emptyList()) }
    
    // Refresh counter for handling screen refreshes
    val refreshCounter = remember { mutableStateOf(0) }
    
    // Helper function to get the integer user ID from auth ID
    suspend fun getUserIdFromAuth(authId: String): Int? {
        return try {
            Log.d("MyPosts", "Looking up user ID for auth ID: $authId")
            val client = SupabaseClient.client
            val response = client.postgrest["users"]
                .select {
                    filter {
                        eq("auth_id", authId)
                    }
                }
            
            val users = response.decodeList<UserInfo>()
            Log.d("MyPosts", "Found ${users.size} users for auth ID: $authId")
            
            if (users.isNotEmpty()) {
                val userId = users.first().id
                Log.d("MyPosts", "User ID for auth ID $authId is: $userId")
                userId
            } else {
                Log.e("MyPosts", "No user found for auth ID: $authId")
                null
            }
        } catch (e: Exception) {
            Log.e("MyPosts", "Error getting user ID for auth ID $authId: ${e.message}", e)
            null
        }
    }
    
    // Load my posts
    LaunchedEffect(refreshCounter.value) {
        isLoading = true
        error = null
        
        try {
            val client = SupabaseClient.client
            val currentUser = client.auth.currentUserOrNull()
            
            if (currentUser != null) {
                val userId = getUserIdFromAuth(currentUser.id)
                
                if (userId != null) {
                    // Get all posts by this user
                    val postsResponse = client.postgrest["posts"]
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                            order("created_at", Order.DESCENDING)
                        }
                    
                    val posts = postsResponse.decodeList<MyPostEntity>()
                    Log.d("MyPosts", "Found ${posts.size} posts by user $userId")
                    
                    // Debug post structure
                    posts.forEachIndexed { index, post ->
                        Log.d("MyPosts", "Post $index structure: id=${post.id}, user_id=${post.user_id}, " +
                                "caption=${post.caption}, content=${post.content}, image_url=${post.image_url}")
                    }
                    
                    // Get user data (though we already know the current user)
                    val userResponse = client.postgrest["users"]
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                    
                    val userData = userResponse.decodeList<PostUserData>().firstOrNull()
                    
                    if (userData != null) {
                        // Transform posts to display format
                        val formattedPosts = posts.map { post ->
                            // Get like count for each post
                            val likeCountResponse = client.postgrest["likes"]
                                .select {
                                    filter {
                                        eq("post_id", post.id)
                                    }
                                }
                            val likeList = likeCountResponse.decodeList<LikeData>()
                            val likeCount = likeList.size
                            
                            // Get comment count for each post
                            val commentCountResponse = client.postgrest["comments"]
                                .select {
                                    filter {
                                        eq("post_id", post.id)
                                    }
                                }
                            val commentList = commentCountResponse.decodeList<CommentData>()
                            val commentCount = commentList.size
                            
                            // Use caption or content as the text to display
                            val postText = post.caption ?: post.content ?: ""
                            
                            MyPostData(
                                id = post.id,
                                image_url = post.image_url,
                                caption = postText,
                                username = userData.username,
                                profile_image = userData.profile_image ?: "",
                                like_count = likeCount,
                                comment_count = commentCount
                            )
                        }
                        
                        myPosts = formattedPosts
                        Log.d("MyPosts", "Loaded ${formattedPosts.size} formatted posts")
                    } else {
                        error = "Could not find user data"
                    }
                } else {
                    error = "Could not determine your user ID"
                }
            } else {
                error = "You need to be logged in to view your posts"
            }
        } catch (e: Exception) {
            error = "Error loading posts: ${e.message}"
            Log.e("MyPosts", "Error loading posts", e)
        } finally {
            isLoading = false
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
            myPosts.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "My Posts",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    Text(
                        text = "You haven't created any posts yet.",
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
            else -> {
                LazyColumn {
                    item {
                        Text(
                            text = "My Posts",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    
                    items(myPosts) { post ->
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
                                // Already on our own profile
                            }
                        )
                    }
                }
            }
        }
    }
} 