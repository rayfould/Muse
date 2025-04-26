package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.Comment
import com.example.creativecommunity.models.Post
import com.example.creativecommunity.models.UserInfo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Data needed for an individual post
@Serializable
data class IndividualPostDetails(
    val id: Int,
    val image_url: String,
    val content: String? = null, 
    val users: UserInfo // Use UserInfo to get auth_id
)

// Data for comments (can be enhanced)
@Serializable
data class PostComment(
    val id: Int,
    val content: String,
    val users: UserInfo // Who made the comment
)

// Data for comments - need to serialize data --> supabase
@Serializable
data class NewComment(
    val user_id: Int,
    val post_id: Int,
    val content: String
)

@Composable
fun IndividualPostPage(navController: NavController, postId: String?) {
    var postDetails by remember { mutableStateOf<IndividualPostDetails?>(null) }
    var comments by remember { mutableStateOf<List<PostComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Posts
    val coroutineScope = rememberCoroutineScope()
    var commentInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val currentUser = remember { SupabaseClient.client.auth.currentUserOrNull() }
    val currentAuthId = currentUser?.id

    val postIdInt = postId?.toIntOrNull()

    LaunchedEffect(postIdInt) {
        if (postIdInt == null) {
            error = "Invalid Post ID"
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        try {
            Log.d("IndividualPost", "Fetching post details for ID: $postIdInt")
            // Fetch Post Details
            val postResult = withContext(Dispatchers.IO) {
                 SupabaseClient.client.postgrest.from("posts")
                    .select(Columns.raw("id, image_url, content, users!inner(id, username, email, profile_image, bio, auth_id)")) { // Fetch user info including auth_id
                        filter { eq("id", postIdInt) }
                    }
                    .decodeSingle<IndividualPostDetails>()
            }
            postDetails = postResult
            Log.d("IndividualPost", "Fetched post details: ${postResult.users.username}")

            // Fetch Comments
             Log.d("IndividualPost", "Fetching comments for post ID: $postIdInt")
             val commentsResult = withContext(Dispatchers.IO) {
                 SupabaseClient.client.postgrest.from("comments")
                     .select(Columns.raw("id, content, users!inner(id, username, email, profile_image, bio, auth_id)")) { // Fetch comment user info too
                         filter { eq("post_id", postIdInt) }
                         order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                     }
                     .decodeList<PostComment>()
             }
             comments = commentsResult
             Log.d("IndividualPost", "Fetched ${commentsResult.size} comments")

        } catch (e: Exception) {
            error = "Failed to load post details: ${e.message}"
            Log.e("IndividualPost", "Error loading post $postIdInt", e)
            postDetails = null
            comments = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back Button
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                 Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                 }
            }
            postDetails != null -> {
                Column(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
                        // Display the Post itself
                        item {
                            Post(
                                navController = navController, // Pass NavController
                                authorId = postDetails!!.users.auth_id, // Pass author's auth_id
                                postId = postDetails!!.id,
                                profileImage = postDetails!!.users.profile_image ?: "https://via.placeholder.com/40",
                                username = postDetails!!.users.username,
                                postImage = postDetails!!.image_url,
                                caption = postDetails!!.content ?: "",
                                // Like/Comment counts fetched within Post composable now
                                likeCount = 0, // Placeholder - Post fetches its own counts
                                commentCount = comments.size, // We have comment count here
                                onCommentClicked = { /* Already on the page, maybe scroll? */ },
                                onImageClick = { /* Already on the page, maybe zoom? */ }
                            )
                            Divider()
                        }

                        // Display Comments Header
                        item {
                            Text(
                                text = "Comments (${comments.size})", 
                                style = MaterialTheme.typography.headlineSmall, 
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        // Display Comments
                        if (comments.isEmpty()) {
                            item { 
                                 Text("No comments yet.", modifier = Modifier.padding(16.dp)) 
                            }
                        } else {
                            items(comments, key = { it.id }) { comment ->
                                Comment(
                                    // Pass data for the comment composable
                                    // Assuming Comment composable takes these parameters
                                    // We might need to adjust Comment.kt if not
                                    profileImage = comment.users.profile_image ?: "https://via.placeholder.com/40",
                                    username = comment.users.username,
                                    commentText = comment.content
                                    // TODO: Make comment username clickable? navController.navigate("user/${comment.users.auth_id}")
                                )
                                Divider()
                            }
                        }
                    }
                }
                // Comment input field and submit button
                if (currentAuthId != null && postIdInt != null) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            label = { Text("Add a comment...") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (commentInput.isNotBlank()) {
                                    isSubmitting = true
                                    submitError = null
                                    coroutineScope.launch {
                                        try {
                                            // Look up numeric user_id from auth_id
                                            val userId = withContext(Dispatchers.IO) {
                                                val response = SupabaseClient.client.postgrest["users"]
                                                    .select { filter { eq("auth_id", currentAuthId) } }
                                                val users = response.decodeList<com.example.creativecommunity.models.UserInfo>()
                                                users.firstOrNull()?.id
                                            }
                                            if (userId != null) {
                                                // Insert comment
                                                withContext(Dispatchers.IO) {
                                                    SupabaseClient.client.postgrest["comments"].insert(
                                                        NewComment(user_id = userId, post_id = postIdInt, content = commentInput)
                                                    )
                                                }
                                                // Refresh comments
                                                val commentsResult = withContext(Dispatchers.IO) {
                                                    SupabaseClient.client.postgrest.from("comments")
                                                        .select(Columns.raw("id, content, users!inner(id, username, email, profile_image, bio, auth_id)")) {
                                                            filter { eq("post_id", postIdInt) }
                                                            order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                                                        }
                                                        .decodeList<PostComment>()
                                                }
                                                comments = commentsResult
                                                commentInput = ""
                                            } else {
                                                submitError = "Could not find your user account."
                                            }
                                        } catch (e: Exception) {
                                            submitError = "Failed to submit comment: ${e.message}"
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting && commentInput.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(if (isSubmitting) "Posting..." else "Post")
                        }
                        if (submitError != null) {
                            Text(submitError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            else -> {
                // Should not happen if not loading and no error, but handle just in case
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Post not found.")
                }
            }
        }
    }
}