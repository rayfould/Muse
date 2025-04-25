package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.UserInfo // Assuming UserInfo is needed
import com.example.creativecommunity.models.Post // Import the Post composable
import com.example.creativecommunity.pages.MyPostData // Reusing MyPostData from MyPostsPage
import com.example.creativecommunity.pages.MyPostEntity // Reusing MyPostEntity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable // Assuming needed for reused data classes
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun UserPostsPage(navController: NavController, userId: String) {
    var userPosts by remember { mutableStateOf<List<MyPostData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        var targetUserIdInt: Int? = null
        Log.d("UserPostsPage", "Received auth_id: $userId")

        if (userId.isNullOrEmpty()) {
            error = "User ID (auth_id) cannot be empty."
            isLoading = false
            Log.e("UserPostsPage", "Received empty or null auth_id string")
            return@LaunchedEffect
        }

        try {
            val client = SupabaseClient.client

            try {
                val userResponse = client.postgrest["users"]
                    .select { filter { eq("auth_id", userId) } }
                val userData = userResponse.decodeSingleOrNull<UserInfo>()

                if (userData == null) {
                    error = "User not found for the provided ID."
                    isLoading = false
                    Log.e("UserPostsPage", "No user found for auth_id: $userId")
                    return@LaunchedEffect
                }

                targetUserIdInt = userData.id
                username = userData.username
                Log.d("UserPostsPage", "Fetched UserInfo: id=$targetUserIdInt, username=$username for auth_id: $userId")

            } catch (e: Exception) {
                 error = "Failed to fetch user data: ${e.message}"
                 isLoading = false
                 Log.e("UserPostsPage", "Failed to fetch user data for auth_id $userId", e)
                 return@LaunchedEffect
            }

            Log.d("UserPostsPage", "Fetching posts for integer user_id: $targetUserIdInt")

            val postsResponse = client.postgrest["posts"]
                .select {
                    filter { eq("user_id", targetUserIdInt!!) }
                    order("created_at", Order.DESCENDING)
                }
            val posts = postsResponse.decodeList<MyPostEntity>()
            Log.d("UserPostsPage", "Found ${posts.size} posts for user_id $targetUserIdInt")

             val formattedPosts = posts.map { post ->
                 val likeCountResponse = client.postgrest["likes"]
                     .select() { 
                         filter { eq("post_id", post.id) }
                     }
                 val likeCount = likeCountResponse.decodeList<LikeData>().size 

                 val commentCountResponse = client.postgrest["comments"]
                    .select() { 
                         filter { eq("post_id", post.id) }
                     }
                 val commentCount = commentCountResponse.decodeList<CommentData>().size 

                 val authorInfo = client.postgrest["users"]
                    .select() { filter { eq("id", post.user_id) } }
                    .decodeSingleOrNull<UserInfo>()


                MyPostData(
                     id = post.id,
                     image_url = post.image_url,
                     caption = post.caption ?: post.content ?: "",
                     username = authorInfo?.username ?: "Unknown",
                     profile_image = authorInfo?.profile_image ?: "",
                     author_id = authorInfo?.auth_id ?: userId,
                     like_count = likeCount,
                     comment_count = commentCount
                 )
             }


            userPosts = formattedPosts
            Log.d("UserPostsPage", "Loaded ${formattedPosts.size} formatted posts for user_id $targetUserIdInt")

        } catch (e: Exception) {
            error = "Failed to load posts: ${e.message}"
            Log.e("UserPostsPage", "Error loading posts for user_id $targetUserIdInt: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }


    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
         Text(
             text = username?.let { "$it's Posts" } ?: "Posts for User",
             style = MaterialTheme.typography.headlineMedium,
             modifier = Modifier.padding(bottom = 16.dp)
         )

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            userPosts.isEmpty() -> {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This user hasn't posted anything yet.")
                 }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(userPosts, key = { it.id }) { postData ->
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
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
} 