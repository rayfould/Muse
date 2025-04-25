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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Item 1: Header and Prompt Card
            item(key = "header_prompt") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$category Community!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))

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

            // Item 2: Sort dropdown (Correctly placed as its own item)
            item(key = "sort_dropdown") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box {
                        Button(onClick = { showSortDropdown = true }, enabled = !isLoading) {
                            Text("Sort by: $selectedSortOption")
                        }
                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            listOf("Recent", "Popular", "Trending").forEach { option ->
                                DropdownMenuItem(text = {Text(option)}, onClick = { 
                                    selectedSortOption = option
                                    showSortDropdown = false
                                    sortPosts(option)
                                 })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Item 3: Loading/Error/Empty state 
            item(key = "status_indicator") {
                 Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    when {
                         isLoading -> CircularProgressIndicator() 
                         fetchError != null -> Text(fetchError!!, color = MaterialTheme.colorScheme.error)
                         posts.isEmpty() && !isLoading -> Text("No posts yet in this category.")
                    }
                 }
            }

            // Items 4...N: Display Posts list
            items(displayedPosts, key = { it.id }) { post ->
                var likeCount by remember { mutableStateOf(0) }
                var commentCount by remember { mutableStateOf(0) }
                
                LaunchedEffect(post.id) {
                    likeCount = likeManager.getLikeCount(post.id)
                    try {
                        // Get all comments and count them, just like we do with likes
                        val comments = SupabaseClient.client.postgrest
                            .from("comments")
                            .select {
                                filter { eq("post_id", post.id) }
                            }
                            .decodeList<Comment>()
                        commentCount = comments.size
                        Log.d("CategoryFeed", "Post ${post.id} comment count: $commentCount")
                    } catch (e: Exception) {
                        Log.e("CategoryFeed", "Failed to get comment count for post ${post.id}", e)
                    }
                }

                Post(
                    navController = navController,
                    authorId = post.user.auth_id,
                    postId = post.id,
                    profileImage = post.user.profile_image ?: defaultProfileImages.random(),
                    username = post.user.username,
                    postImage = post.image_url,
                    caption = post.content, 
                    likeCount = likeCount, 
                    commentCount = commentCount, 
                    onCommentClicked = { navController.navigate("individual_post/${post.id}") },
                    onImageClick = { 
                        selectedPostImageUrl = post.image_url
                        showPostImageDialog = true
                    }
                )
                 Spacer(modifier = Modifier.height(8.dp))
            }

            // Footer item 
            item(key = "footer_spacer") {
                Spacer(modifier = Modifier.height(100.dp))
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