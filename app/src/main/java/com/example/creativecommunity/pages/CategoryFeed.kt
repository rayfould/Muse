package com.example.creativecommunity.pages

import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.Post
import com.example.creativecommunity.models.UserInfo
import com.example.creativecommunity.utils.LikeManager
import com.example.creativecommunity.utils.PromptRotation
import com.example.creativecommunity.utils.PromptWithDates
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.w3c.dom.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class Prompt(
    val title: String
)

@Serializable
data class FeedPost(
    val id: Int,
    @SerialName("image_url") val image_url: String,
    @SerialName("content") val content: String,
    @SerialName("users") val user: UserInfo,
    @SerialName("created_at") val created_at: String? = null
)

// Data class to hold post with its like count for sorting (same as in DiscoveryPage)
data class FeedPostWithLikes(
    val post: FeedPost,
    val likeCount: Int
)

@Composable
fun CategoryFeed(navController: NavController, category: String) {
    // State variables
    var promptData by remember { mutableStateOf<PromptWithDates?>(null) }
    var promptLoading by remember { mutableStateOf(true) }
    var promptError by remember { mutableStateOf<String?>(null) }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    
    // Sort functionality state variables
    var showSortDropdown by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf("Recent") }
    var isLoading by remember { mutableStateOf(false) }
    var displayedPosts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    
    // Profile picture expansion
    var showPfpDialog by remember { mutableStateOf(false) }
    var selectedPfpUrl by remember { mutableStateOf<String?>(null) }
    
    // Post image expansion
    var showPostImageDialog by remember { mutableStateOf(false) }
    var selectedPostImageUrl by remember { mutableStateOf<String?>(null) }

    // For coroutines and state
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val likeManager = remember { LikeManager.getInstance(context) }

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

    // Fetch posts (select created_at)
    LaunchedEffect(category) {
        isLoading = true 
        fetchError = null
        try {
            val fetchedPosts = withContext(Dispatchers.IO) {
                val result = SupabaseClient.client.postgrest.from("posts")
                    .select(Columns.raw("id, image_url, content, created_at, user_id, users!inner(id, username, email, profile_image, auth_id)")) {
                        filter { eq("category", category) }
                        order("created_at", Order.DESCENDING)
                    }
                val postsList = result.decodeList<FeedPost>()
                Log.d("CategoryFeed", "Fetched ${postsList.size} posts for category: $category")
                postsList
            }
            posts = fetchedPosts
            displayedPosts = fetchedPosts 
            selectedSortOption = "Recent"
        } catch (e: Exception) {
            fetchError = "Failed to load posts: ${e.message}"
            Log.e("CategoryFeed", "Failed to load posts", e)
        } finally {
            isLoading = false
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

    // Sorting logic (Fix Recent sort, use direct logic)
    val sortPosts: (String) -> Unit = { sortOption ->
        coroutineScope.launch {
            isLoading = true
            try {
                displayedPosts = when (sortOption) {
                    "Recent" -> posts.sortedByDescending { it.created_at }
                    "Popular" -> {
                        val postsWithLikes = posts.map { post ->
                            val likeCount = likeManager.getLikeCount(post.id)
                            FeedPostWithLikes(post, likeCount)
                        }
                        postsWithLikes.sortedByDescending { it.likeCount }.map { it.post }
                    }
                    "Trending" -> {
                        posts.sortedByDescending { it.created_at }
                    }
                    else -> posts
                }
            } catch (e: Exception) {
                fetchError = "Failed to sort posts: ${e.message}"
                Log.e("CategoryFeed", "Sort error", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Back Button
            TextButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("← Back")
            }
            // Prompt Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "This Week's Prompt",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    when {
                        promptLoading -> Text("Loading prompt...", style = MaterialTheme.typography.bodyMedium)
                        promptError != null -> Text(promptError!!, color = MaterialTheme.colorScheme.error)
                        promptData == null -> Text("No active prompt for $category", style = MaterialTheme.typography.bodyMedium)
                        else -> {
                            Text(promptData!!.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            if (!promptData!!.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(promptData!!.description!!, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Posts List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (fetchError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(fetchError!!, color = MaterialTheme.colorScheme.error)
                }
            } else if (displayedPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts yet in this category.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(displayedPosts) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Post(
                                navController = navController,
                                authorId = post.user.auth_id,
                                postId = post.id,
                                profileImage = post.user.profile_image ?: defaultProfileImages.random(),
                                username = post.user.username,
                                postImage = post.image_url,
                                caption = post.content,
                                likeCount = 0, // Let Post composable fetch its own counts
                                commentCount = 0, // Let Post composable fetch its own counts
                                onCommentClicked = { navController.navigate("individual_post/${post.id}") },
                                onImageClick = { showPostImageDialog = true; selectedPostImageUrl = post.image_url }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp)) // For FAB space
                    }
                }
            }
        }
        // Floating Action Button for New Post
        FloatingActionButton(
            onClick = { navController.navigate("new_post/$category") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(28.dp)
        ) {
            Text("+")
        }
    }
}