package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
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
import com.example.creativecommunity.models.UserData
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
import kotlin.random.Random
import com.example.creativecommunity.models.RecommendationEngine
import com.example.creativecommunity.models.DiscoveryPost
import com.example.creativecommunity.models.DiscoveryPostWithCounts
import com.example.creativecommunity.models.PostMetrics

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

    // Fetch posts
    LaunchedEffect(category) {
        try {
            val fetchedPosts = withContext(Dispatchers.IO) {
                val result = SupabaseClient.client.postgrest.from("posts")
                    .select(Columns.raw("id, image_url, content, user_id, users!inner(profile_image, username)")) {
                        filter {
                            eq("category", category)
                        }
                        order("created_at", Order.DESCENDING)
                        limit(10)
                    }
                val postsList = result.decodeList<FeedPost>()
                Log.d("SupabaseTest", "Fetched posts: $postsList")
                postsList
            }
            posts = fetchedPosts
            displayedPosts = fetchedPosts // Initialize displayed posts with all posts
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
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header item
            item {
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
                
                // Sort button and dropdown - moved below the prompt card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showSortDropdown = true },
                        enabled = !isLoading
                    ) {
                        Text("Sort by: $selectedSortOption")
                    }
                    
                    DropdownMenu(
                        expanded = showSortDropdown,
                        onDismissRequest = { showSortDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recent") },
                            onClick = {
                                selectedSortOption = "Recent"
                                showSortDropdown = false
                                isLoading = true
                                coroutineScope.launch {
                                    delay(300) // Small delay for animation
                                    displayedPosts = posts
                                    isLoading = false
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Most Liked") },
                            onClick = {
                                selectedSortOption = "Most Liked"
                                showSortDropdown = false
                                isLoading = true
                                coroutineScope.launch {
                                    // Fetch like counts for all posts and sort
                                    fetchAndSortPostsByLikes(posts, likeManager) { sortedPosts ->
                                        displayedPosts = sortedPosts
                                        isLoading = false
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recommended") },
                            onClick = {
                                selectedSortOption = "Recommended"
                                showSortDropdown = false
                                isLoading = true
                                coroutineScope.launch {
                                    // Use our recommendation algorithm
                                    fetchAndScorePosts(posts, likeManager) { recommendedPosts ->
                                        displayedPosts = recommendedPosts
                                        isLoading = false
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Random") },
                            onClick = {
                                selectedSortOption = "Random"
                                showSortDropdown = false
                                isLoading = true
                                coroutineScope.launch {
                                    delay(300) // Small delay for animation
                                    displayedPosts = posts.shuffled(Random(System.currentTimeMillis()))
                                    isLoading = false
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Loading indicator
            item {
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(48.dp)
                    )
                }
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
                items(
                    items = displayedPosts,
                    key = { it.id } // Use id as key for better stability
                ) { post ->
                    val defaultPfp = remember { post.user.profile_image ?: defaultProfileImages.random() }
                    AnimatedVisibility(
                        visible = !isLoading,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
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

// Function to fetch like counts and sort posts by likes (copied from DiscoveryPage)
private suspend fun fetchAndSortPostsByLikes(
    posts: List<FeedPost>,
    likeManager: LikeManager,
    onComplete: (List<FeedPost>) -> Unit
) {
    val postsWithLikes = mutableListOf<FeedPostWithLikes>()
    
    for (post in posts) {
        val likeCount = likeManager.getLikeCount(post.id)
        postsWithLikes.add(FeedPostWithLikes(post, likeCount))
    }
    
    // Sort by like count (descending)
    val sortedPosts = postsWithLikes.sortedByDescending { it.likeCount }.map { it.post }
    
    onComplete(sortedPosts)
}

// Function to fetch metrics, score and sort posts by the recommendation algorithm (copied from DiscoveryPage)
private suspend fun fetchAndScorePosts(
    posts: List<FeedPost>,
    likeManager: LikeManager,
    onComplete: (List<FeedPost>) -> Unit
) {
    // First, collect like and comment counts for all posts
    val postsWithCounts = mutableListOf<FeedPostWithCounts>()
    
    for (post in posts) {
        val likeCount = likeManager.getLikeCount(post.id)
        // For simplicity, we'll generate random comment counts
        // In a real app, you would fetch this from your database
        val commentCount = (0..5).random() // Simplified for demo
        
        postsWithCounts.add(FeedPostWithCounts(post, likeCount, commentCount))
    }
    
    // Convert FeedPostWithCounts to DiscoveryPostWithCounts for compatibility with RecommendationEngine
    val discoveryPostsWithCounts = postsWithCounts.map { feedPostWithCounts ->
        // Create a DiscoveryPost from FeedPost
        val discoveryPost = DiscoveryPost(
            id = feedPostWithCounts.post.id,
            image_url = feedPostWithCounts.post.image_url,
            content = feedPostWithCounts.post.content,
            category = "", // Not used for recommendation algorithm
            user = feedPostWithCounts.post.user,
            created_at = null
        )
        
        // Create a DiscoveryPostWithCounts with the converted post and the same counts
        DiscoveryPostWithCounts(
            post = discoveryPost,
            likeCount = feedPostWithCounts.likeCount,
            commentCount = feedPostWithCounts.commentCount
        )
    }
    
    // Compute author engagement metrics using converted posts
    val authorEngagement = RecommendationEngine.computeAuthorEngagement(discoveryPostsWithCounts)
    
    // Create metrics for each post and score them
    val scoredPosts = postsWithCounts.map { postWithCounts ->
        val username = postWithCounts.post.user.username ?: ""
        val engagement = authorEngagement[username] ?: 0f
        
        // Convert timestamp string to long if available, or use current time
        val createdAt = System.currentTimeMillis() // Default to current time
        
        val metrics = PostMetrics(
            postId = postWithCounts.post.id,
            likeCount = postWithCounts.likeCount,
            commentCount = postWithCounts.commentCount,
            authorEngagement = engagement,
            createdAt = createdAt
        )
        
        val score = RecommendationEngine.score(metrics)
        ScoredPost(postWithCounts.post, metrics, score)
    }
    
    // Sort by score (descending)
    val recommendedPosts = scoredPosts.sortedByDescending { it.score }.map { it.post }
    
    // Log the scores for debugging
    scoredPosts.sortedByDescending { it.score }.take(5).forEach { scoredPost ->
        Log.d("Recommendation", "Post ${scoredPost.post.id} by ${scoredPost.post.user.username}: " +
                "Score=${scoredPost.score}, Likes=${scoredPost.metrics.likeCount}, " +
                "Comments=${scoredPost.metrics.commentCount}, " +
                "AuthorEngagement=${scoredPost.metrics.authorEngagement}")
    }
    
    onComplete(recommendedPosts)
}

// Data classes needed for recommendation algorithm (copied from DiscoveryPage)
data class FeedPostWithCounts(
    val post: FeedPost, 
    val likeCount: Int,
    val commentCount: Int
)

data class ScoredPost(
    val post: FeedPost,
    val metrics: PostMetrics,
    val score: Float
)