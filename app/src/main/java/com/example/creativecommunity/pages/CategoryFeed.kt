package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.CommentIdOnly
import com.example.creativecommunity.models.Post
import com.example.creativecommunity.models.PostMetrics
import com.example.creativecommunity.models.RecommendationEngine
import com.example.creativecommunity.models.UserInfo
import com.example.creativecommunity.ui.theme.HighlightRed
import com.example.creativecommunity.ui.theme.OnPrimaryWhite
import com.example.creativecommunity.ui.theme.PrimaryBlue
import com.example.creativecommunity.ui.theme.DeepAquaContainer
import com.example.creativecommunity.utils.LikeManager
import com.example.creativecommunity.utils.PromptRotation
import com.example.creativecommunity.utils.PromptWithDates
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException
import com.example.creativecommunity.models.IPostData
import com.example.creativecommunity.models.IPostWithEngagementData

@Serializable
data class Prompt(
    val title: String
)

@Serializable
data class FeedPost(
    val id: Int,
    @SerialName("image_url") val image_url: String,
    @SerialName("content") val content: String,
    @SerialName("users") override val user: UserInfo,
    @SerialName("created_at") val created_at: String? = null
) : IPostData

// Data class to hold post with its like count for sorting (same as in DiscoveryPage)
data class FeedPostWithLikes(
    val post: FeedPost,
    val likeCount: Int
)

data class FeedPostWithCounts(
    override val post: FeedPost,
    override val likeCount: Int,
    override val commentCount: Int
) : IPostWithEngagementData

data class ScoredFeedPost(
    val post: FeedPost,
    val metrics: PostMetrics,
    val score: Float
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isPhoneMode = screenWidth < 600

    // Default profile images for users without one
    val defaultProfileImages = listOf(
        "https://i.imgur.com/DyFZblf.jpeg", // Gray square
        "https://i.imgur.com/kcbZfpx.png", // Smiley face
        "https://i.imgur.com/WvDsY4x.jpeg", // Simple avatar silhouette
        "https://i.imgur.com/iCy2JU1.jpeg", // Minimalist user icon
        "https://i.imgur.com/7hVHf5f.png"  // Abstract shape
    )

    var commentCounts by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

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
            // Fetch comment counts for each post
            val postsWithCommentCounts = fetchedPosts.map { post ->
                val commentCount = withContext(Dispatchers.IO) {
                    SupabaseClient.client.postgrest["comments"]
                        .select(Columns.raw("id")) { filter { eq("post_id", post.id) } }
                        .decodeList<CommentIdOnly>().size
                }
                post to commentCount
            }
            posts = fetchedPosts
            displayedPosts = fetchedPosts 
            selectedSortOption = "Recent"
            // Store comment counts in a map for display
            commentCounts = postsWithCommentCounts.associate { it.first.id to it.second }
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

    // Sorting logic - FIXED
    val sortPosts: (String) -> Unit = { sortOption ->
        coroutineScope.launch { 
            isLoading = true // Ensure isLoading is true before starting any sort logic
            try {
                val sortedResult = withContext(Dispatchers.Default) {
                    when (sortOption) {
                        "Recent" -> {
                            delay(50) // Small delay for visual consistency
                            posts.sortedByDescending { it.created_at }
                        }
                        "Random" -> {
                            delay(50) // Small delay for visual consistency
                            posts.shuffled()
                        }
                        "Most Liked" -> {
                            val postsWithLikes = posts.map { post: FeedPost ->
                                val likeCount = likeManager.getLikeCount(post.id) // This might be slow
                                FeedPostWithLikes(post, likeCount)
                            }
                            postsWithLikes.sortedByDescending { it.likeCount }.map { it.post }
                        }
                        "Recommended" -> {
                            // 1. Map (Like counts might be slow)
                            val postsWithCounts = posts.map { post: FeedPost ->
                                FeedPostWithCounts(
                                    post = post,
                                    likeCount = likeManager.getLikeCount(post.id),
                                    commentCount = commentCounts[post.id] ?: 0
                                )
                            }
                            // 2. Compute Engagement (Might be CPU intensive)
                            val authorEngagement = RecommendationEngine.computeAuthorEngagement(postsWithCounts)
                            // 3. Calculate Scores (Might be CPU intensive)
                            val scoredPosts = postsWithCounts.map { postWithCounts: FeedPostWithCounts ->
                                val username = postWithCounts.post.user.username ?: ""
                                val engagement = authorEngagement[username] ?: 0f
                                val createdAtEpochMillis = try {
                                    postWithCounts.post.created_at?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis()
                                } catch (e: DateTimeParseException) {
                                    Log.w("Recommendation", "Could not parse timestamp: ${postWithCounts.post.created_at}, using current time.")
                                    System.currentTimeMillis()
                                }
                                val metrics = PostMetrics(
                                    postId = postWithCounts.post.id,
                                    likeCount = postWithCounts.likeCount,
                                    commentCount = postWithCounts.commentCount,
                                    authorEngagement = engagement,
                                    createdAt = createdAtEpochMillis
                                )
                                val score = RecommendationEngine.score(metrics)
                                ScoredFeedPost(postWithCounts.post, metrics, score)
                            }
                            // 4. Sort by Score
                            scoredPosts.sortedByDescending { it.score }.map { it.post }
                        }
                        else -> {
                            delay(50) // Small delay for visual consistency
                            posts
                        }
                    }
                }
                displayedPosts = sortedResult
            } catch (e: Exception) {
                fetchError = "Failed to sort posts: ${e.message}"
                Log.e("CategoryFeed", "Sort error", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Wrap Column and FAB in a Box for alignment
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Column for Header + Scrolling Content
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Fixed Header Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp), // Adjust padding as needed
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back Button
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Category Title (takes up middle space)
                Text(
                    text = formatCategoryTitle(category),
                    style = MaterialTheme.typography.titleLarge, // Use a prominent style
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center, // Center text
                    modifier = Modifier.weight(1f) // Allow text to take available space
                )

                // Sorting Button Box
                Box { 
                    IconButton(onClick = { showSortDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Posts",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showSortDropdown,
                        onDismissRequest = { showSortDropdown = false }
                    ) {
                         val options = listOf("Recent", "Random", "Most Liked", "Recommended")
                         options.forEach { option ->
                             DropdownMenuItem(
                                 text = { Text(option) },
                                 onClick = {
                                     if (selectedSortOption != option) {
                                         selectedSortOption = option
                                         sortPosts(option)
                                     }
                                     showSortDropdown = false
                                 }
                             )
                         }
                    }
                }
            }
            // --- End Header Row ---

            // --- Scrollable Content ---
            // Posts List Section (includes LazyColumn)
            if (isLoading) {
                 Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { // Added weight
                    CircularProgressIndicator()
                }
            } else if (fetchError != null) {
                 Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { // Added weight
                    Text(fetchError!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                 // Use LazyColumn for both prompt and posts, or empty message
                 LazyColumn(
                    modifier = Modifier.weight(1f), // Make LazyColumn take remaining space
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Prompt Card item (existing code)
                    item {
                        promptData?.let {
                            // Define HORIZONTAL gradient using Theme Primary Blue -> Highlight Red
                            val gradientBrush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer, // Start Muted Blue
                                    DeepAquaContainer                           // End Muted Teal
                                ),
                                start = Offset.Zero, // Top-Left
                                end = Offset.Infinite // Bottom-Right (represents diagonal)
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .background(gradientBrush) // Apply new Blue->Red gradient
                                        .padding(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lightbulb,
                                            contentDescription = "Prompt Icon",
                                            tint = OnPrimaryWhite, // Use WHITE for contrast
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "This Week's Prompt",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = OnPrimaryWhite // Use WHITE for contrast
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    when {
                                        promptLoading -> Text(
                                            "Loading prompt...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnPrimaryWhite.copy(alpha = 0.7f) // White, less emphasis
                                        )
                                        promptError != null -> Text(
                                            promptError!!,
                                            color = MaterialTheme.colorScheme.error, // Keep error distinct
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        promptData == null -> Text(
                                            "No active prompt for $category",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = OnPrimaryWhite.copy(alpha = 0.7f)
                                        )
                                        else -> {
                                            Text(
                                                text = promptData!!.title,
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Medium,
                                                color = OnPrimaryWhite // Use WHITE for contrast
                                            )
                                            if (!promptData!!.description.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = promptData!!.description!!,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = OnPrimaryWhite // Use WHITE for contrast
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Conditionally display posts or "No posts" message
                    if (displayedPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxSize() // Fill available space in LazyColumn
                                    .padding(top = if (promptData != null) 16.dp else 120.dp), // Add more top padding if no prompt
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No posts in this category yet.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                         items(
                            items = displayedPosts, 
                            key = { post: FeedPost -> post.id }
                        ) { post: FeedPost ->
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Post(
                                    navController = navController,
                                    authorId = post.user.auth_id,
                                    postId = post.id,
                                    profileImage = post.user.profile_image ?: defaultProfileImages.random(),
                                    username = post.user.username ?: "User",
                                    postImage = post.image_url,
                                    caption = post.content,
                                    likeCount = 0,
                                    commentCount = commentCounts[post.id] ?: 0,
                                    onCommentClicked = { navController.navigate("individual_post/${post.id}") },
                                    onImageClick = { showPostImageDialog = true; selectedPostImageUrl = post.image_url }
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp)) // For FAB space
                    }
                }
            }
        }
        // Floating Action Button remains outside the Column, anchored to the parent Box
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

// --- ADD Helper Function to Format Title ---
private fun formatCategoryTitle(rawCategory: String): String {
    return rawCategory
        .split('_') // Split by underscore
        .joinToString(" ") { word -> // Join with space
            word.lowercase() // Convert word to lowercase
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } // Capitalize first letter
        }
}