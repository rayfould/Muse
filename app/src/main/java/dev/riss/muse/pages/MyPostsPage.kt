package dev.riss.muse.pages

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.riss.muse.SupabaseClient
import dev.riss.muse.models.Post
import dev.riss.muse.models.UserInfo
import kotlinx.serialization.Serializable
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import androidx.compose.material3.Divider
import io.github.jan.supabase.postgrest.query.Columns

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
data class LikeData(
    val id: String? = null,
    val user_id: Int? = null,
    val post_id: Int? = null,
    val created_at: String? = null,
    val auth_id: String? = null
)

@Serializable
data class CommentData(
    val id: Int,
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
    val author_id: String,
    var like_count: Int = 0,
    var comment_count: Int = 0
)

@Composable
fun MyPostsPage(
    navController: NavController,
    userId: Int? = null,
    title: String = "My Posts"
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var myPosts by remember { mutableStateOf<List<MyPostData>>(emptyList()) }
    
    val refreshCounter = remember { mutableStateOf(0) }
    
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
    
    LaunchedEffect(refreshCounter.value) {
        isLoading = true
        error = null
        
        try {
            val client = SupabaseClient.client
            val currentUserAuthId = client.auth.currentUserOrNull()?.id
            val targetUserId = userId ?: if (currentUserAuthId != null) getUserIdFromAuth(currentUserAuthId) else null
            
            if (targetUserId != null) {
                val postsResponse = client.postgrest["posts"]
                    .select {
                        filter { eq("user_id", targetUserId) }
                        order("created_at", Order.DESCENDING)
                    }
                val posts = postsResponse.decodeList<MyPostEntity>()
                Log.d("MyPosts", "Found ${posts.size} posts by user $targetUserId")
                
                val userResponse = client.postgrest["users"]
                    .select(columns = Columns.list("id", "username", "profile_image", "bio", "auth_id", "email")) {
                        filter { eq("id", targetUserId) }
                    }
                
                val userData = userResponse.decodeSingleOrNull<UserInfo>()
                
                if (userData != null) {
                    val formattedPosts = posts.map { post ->
                        val likeCountResponse = client.postgrest["likes"]
                            .select {
                                filter { eq("post_id", post.id) }
                            }
                        val likeCount = likeCountResponse.decodeList<LikeData>().size
                        
                        val commentCountResponse = client.postgrest["comments"]
                            .select {
                                filter { eq("post_id", post.id) }
                            }
                        val commentCount = commentCountResponse.decodeList<CommentData>().size
                        
                        val postText = post.caption ?: post.content ?: ""
                        
                        MyPostData(
                            id = post.id,
                            image_url = post.image_url,
                            caption = postText,
                            username = userData.username,
                            profile_image = userData.profile_image ?: "",
                            author_id = userData.auth_id,
                            like_count = likeCount,
                            comment_count = commentCount
                        )
                    }
                    
                    myPosts = formattedPosts
                    Log.d("MyPosts", "Loaded ${formattedPosts.size} formatted posts")
                } else {
                    error = "Could not find user data for ID: $targetUserId"
                    Log.e("MyPosts", "Could not find user data for ID: $targetUserId")
                }
            } else {
                error = "Could not determine user ID (current user might be null)"
                Log.e("MyPosts", "Could not determine user ID")
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
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    Text(
                        text = "No posts yet.",
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(text = title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(myPosts) { postData ->
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