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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.Post
import com.example.creativecommunity.models.SavedPost
import com.example.creativecommunity.models.UserInfo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
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
    val profile_image: String? = null,
    val auth_id: String
)

@Serializable
data class LikeEntity(
    val id: String? = null,
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
    val author_id: String = "",
    var like_count: Int = 0,
    var comment_count: Int = 0
)

@Composable
fun SavedPostsPage(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var savedPosts by remember { mutableStateOf<List<PostData>>(emptyList()) }
    
    val refreshCounter = remember { mutableStateOf(0) }
    
    DisposableEffect(Unit) {
        refreshCounter.value++
        
        onDispose { /* cleanup if needed */ }
    }
    
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
    
    suspend fun loadSavedPosts(callback: (List<PostData>, String?) -> Unit) {
        try {
            val client = SupabaseClient.client
            val currentUserAuthId = client.auth.currentUserOrNull()?.id ?: ""
            
            if (currentUserAuthId.isNotEmpty()) {
                val currentUserId = getUserIdFromAuth(currentUserAuthId)
                
                if (currentUserId != null) {
                    val savedPostsResponse = client.postgrest["saved_posts"]
                        .select { filter { eq("user_id", currentUserId) } }
                    val savedPostsEntries = savedPostsResponse.decodeList<SavedPost>()
                    
                    if (savedPostsEntries.isEmpty()) {
                        callback(emptyList(), null)
                        return
                    }
                    
                    val postIds = savedPostsEntries.map { it.post_id }
                    
                    // Fetch posts one by one instead of using in filter
                    val posts = mutableListOf<PostEntity>()
                    for (postId in postIds) {
                        try {
                            val postResponse = client.postgrest["posts"]
                                .select {
                                    filter { eq("id", postId) }
                                }
                            val post = postResponse.decodeSingleOrNull<PostEntity>()
                            if (post != null) {
                                posts.add(post)
                            }
                        } catch (e: Exception) {
                            Log.e("SavedPosts", "Error fetching post $postId", e)
                        }
                    }
                    
                    // Create a map of user data as we process posts
                    val usersDataMap = mutableMapOf<Int, UserEntity>()
                    for (post in posts) {
                        if (!usersDataMap.containsKey(post.user_id)) {
                            try {
                                val userResponse = client.postgrest["users"]
                                    .select(columns = Columns.list("id", "username", "profile_image", "auth_id", "email")) {
                                        filter { eq("id", post.user_id) }
                                    }
                                val user = userResponse.decodeSingleOrNull<UserEntity>()
                                if (user != null) {
                                    usersDataMap[post.user_id] = user
                                }
                            } catch (e: Exception) {
                                Log.e("SavedPosts", "Error fetching user ${post.user_id}", e)
                            }
                        }
                    }
                    
                    val postsWithDetails = posts.map { post ->
                        val userData = usersDataMap[post.user_id]
                        
                        val likeCountResponse = client.postgrest["likes"]
                            .select {
                                filter { eq("post_id", post.id) }
                            }
                        val likeCount = likeCountResponse.decodeList<LikeEntity>().size
                        
                        val commentCountResponse = client.postgrest["comments"]
                            .select {
                                filter { eq("post_id", post.id) }
                            }
                        val commentCount = commentCountResponse.decodeList<CommentEntity>().size
                        
                        PostData(
                            id = post.id,
                            user_id = post.user_id,
                            prompt_id = post.prompt_id,
                            image_url = post.image_url,
                            caption = post.caption ?: post.content ?: "",
                            created_at = post.created_at ?: "",
                            username = userData?.username ?: "Unknown User",
                            profile_image = userData?.profile_image ?: "",
                            author_id = userData?.auth_id ?: "",
                            like_count = likeCount,
                            comment_count = commentCount
                        )
                    }
                    
                    callback(postsWithDetails, null)
                } else {
                    callback(emptyList(), "Could not find your user account.")
                }
            } else {
                callback(emptyList(), "Please sign in to view saved posts.")
            }
        } catch (e: Exception) {
            Log.e("SavedPosts", "Error loading saved posts", e)
            callback(emptyList(), "Error loading saved posts: ${e.message}")
        }
    }

    LaunchedEffect(refreshCounter.value) {
        isLoading = true
        loadSavedPosts { posts, err ->
            savedPosts = posts
            error = err
            isLoading = false
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp)
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
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No saved posts yet.")
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Saved Posts", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(savedPosts, key = { it.id }) { postData ->
                            Post(
                                navController = navController,
                                authorId = postData.author_id,
                                postId = postData.id,
                                profileImage = postData.profile_image,
                                username = postData.username,
                                postImage = postData.image_url,
                                caption = postData.caption,
                                likeCount = postData.like_count,
                                commentCount = postData.comment_count,
                                onCommentClicked = { navController.navigate("individual_post/${postData.id}") },
                                onImageClick = { navController.navigate("individual_post/${postData.id}") }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
} 