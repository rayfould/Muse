package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.Post
import com.example.creativecommunity.models.UserData
import com.example.creativecommunity.utils.PromptRotation
import com.example.creativecommunity.utils.PromptWithDates
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.compose.ui.layout.ContentScale

@Serializable
data class Prompt(
    val title: String
)

@Serializable
data class FeedPost(
    val id: Int,
    @SerialName("image_url") val image_url: String,
    @SerialName("content") val content: String,
    @SerialName("users") val user: UserData
)

@Composable
fun CategoryFeed(navController: NavController, category: String) {
    // State variables
    var promptData by remember { mutableStateOf<PromptWithDates?>(null) }
    var promptLoading by remember { mutableStateOf(true) }
    var promptError by remember { mutableStateOf<String?>(null) }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    
    // Profile picture expansion
    var showPfpDialog by remember { mutableStateOf(false) }
    var selectedPfpUrl by remember { mutableStateOf<String?>(null) }
    
    // Post image expansion
    var showPostImageDialog by remember { mutableStateOf(false) }
    var selectedPostImageUrl by remember { mutableStateOf<String?>(null) }

    // Default profile images for users without one
    val defaultProfileImages = listOf(
        "https://i.imgur.com/DyFZblf.jpeg", // Gray square
        "https://i.imgur.com/kcbZfpx.png", // Smiley face
        "https://i.imgur.com/WvDsY4x.jpeg", // Simple avatar silhouette
        "https://i.imgur.com/iCy2JU1.jpeg", // Minimalist user icon
        "https://i.imgur.com/7hVHf5f.png"  // Abstract shape
    )

    // Fetch prompt data
    LaunchedEffect(category) {
        try {
            promptLoading = true
            val prompt = PromptRotation.getCurrentPrompt(category)
            promptData = prompt
            promptLoading = false
        } catch (e: Exception) {
            promptError = "Failed to load prompt: ${e.message}"
            promptLoading = false
        }
    }

    // Fetch posts
    LaunchedEffect(category) {
        try {
            val fetchedPosts = withContext(Dispatchers.IO) {
                val result = SupabaseClient.client.postgrest.from("posts")
                    .select(Columns.raw("id, image_url, content, user_id, users!inner(profile_image, username)")) {
                        filter {
                            eq("category", category)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(10)
                    }
                val postsList = result.decodeList<FeedPost>()
                Log.d("SupabaseTest", "Fetched posts: $postsList")
                postsList
            }
            posts = fetchedPosts
        } catch (e: Exception) {
            fetchError = "Failed to load posts: ${e.message}"
        }
    }

    // Profile picture dialog
    if (showPfpDialog && selectedPfpUrl != null) {
        Dialog(onDismissRequest = { showPfpDialog = false }) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = selectedPfpUrl,
                    contentDescription = "Enlarged profile picture",
                    modifier = Modifier
                        .size(500.dp)
                        .clickable { showPfpDialog = false }
                )
            }
        }
    }
    
    // Post image dialog
    if (showPostImageDialog && selectedPostImageUrl != null) {
        Dialog(onDismissRequest = { showPostImageDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedPostImageUrl,
                    contentDescription = "Enlarged post image",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)  // Take up 90% of screen width
                        .clickable { showPostImageDialog = false },
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        // Main content with LazyColumn that includes the header and prompt
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header item
            item {
                Text(
                    text = "$category Community!",
                    modifier = Modifier.padding(top = 30.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Prompt Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "THIS WEEK'S PROMPT",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (promptLoading) {
                            Text(
                                text = "Loading prompt...",
                                textAlign = TextAlign.Center
                            )
                        } else if (promptError != null) {
                            Text(
                                text = promptError!!,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        } else if (promptData == null) {
                            Text(
                                text = "No active prompt for $category",
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = promptData!!.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Display description if available
                            promptData!!.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Show error or empty state
            if (fetchError != null) {
                item {
                    Text(text = fetchError!!)
                }
            } else if (posts.isEmpty()) {
                item {
                    Text(text = "No posts yet for this category.")
                }
            } else {
                // Posts list
                items(posts) { post ->
                    val defaultPfp = remember { post.user.profile_image ?: defaultProfileImages.random() }
                    Post(
                        postId = post.id,
                        profileImage = defaultPfp,
                        username = post.user.username ?: "Unknown User",
                        postImage = post.image_url,
                        caption = post.content,
                        likeCount = 0,
                        commentCount = 0,
                        onCommentClicked = {
                            navController.navigate("individual_post/${post.id}")
                        },
                        onProfileClick = {
                            selectedPfpUrl = defaultPfp
                            showPfpDialog = true
                        },
                        onImageClick = {
                            selectedPostImageUrl = post.image_url
                            showPostImageDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Bottom navigation buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("All Communities")
            }
            Button(
                onClick = {
                    navController.navigate("new_post/${category}")
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text("+")
            }
        }
    }
}