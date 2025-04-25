package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.*
import com.example.creativecommunity.utils.LikeManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

// Data class to hold post with its like count for sorting
data class PostWithLikes(
    val post: DiscoveryPost,
    val likeCount: Int
)

@Composable
fun DiscoveryPage(navController: NavController) {
    var showSortDropdown by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf("Recent") }
    var isLoading by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<DiscoveryPost>>(emptyList()) }
    var displayedPosts by remember { mutableStateOf<List<DiscoveryPost>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val likeManager = remember { LikeManager.getInstance(context) }

    // Sorting logic moved inside Composable
    val sortPosts: (String) -> Unit = { sortOption ->
        coroutineScope.launch {
            isLoading = true
            try {
                displayedPosts = when (sortOption) {
                    "Recent" -> posts.sortedByDescending { it.created_at }
                    "Most Liked" -> {
                        val postsWithLikes = posts.map { post ->
                            val likeCount = likeManager.getLikeCount(post.id)
                            PostWithLikes(post, likeCount)
                        }
                        postsWithLikes.sortedByDescending { it.likeCount }.map { it.post }
                    }
                    "Recommended" -> {
                        // Placeholder for recommendation logic
                        posts.sortedByDescending { it.created_at } // Default to recent
                    }
                    "Random" -> posts.shuffled(Random(System.currentTimeMillis()))
                    else -> posts
                }
            } catch (e: Exception) {
                fetchError = "Failed to sort posts: ${e.message}"
                Log.e("DiscoveryPage", "Sort error", e)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Discover", style = MaterialTheme.typography.headlineMedium)
                
                Box {
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
                        listOf("Recent", "Most Liked", "Recommended", "Random").forEach { option ->
                             DropdownMenuItem(text = {Text(option)}, onClick = { 
                                selectedSortOption = option
                                showSortDropdown = false
                                sortPosts(option)
                             })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            var showPfpDialog by remember { mutableStateOf(false) }
            var selectedPfpUrl by remember { mutableStateOf<String?>(null) }

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

            LaunchedEffect(Unit) {
                isLoading = true
                fetchError = null
                try {
                    val fetchedPosts = withContext(Dispatchers.IO) {
                        val result = SupabaseClient.client.postgrest.from("posts")
                            .select(Columns.raw("id, image_url, content, category, created_at, user_id, users!inner(id, username, email, profile_image, bio, auth_id)")) {
                                order("created_at", Order.DESCENDING)
                                limit(50) // Increased limit, consider pagination later
                            }
                        val postsList = result.decodeList<DiscoveryPost>()
                        Log.d("DiscoveryPage", "Fetched ${postsList.size} posts")
                        postsList
                    }
                    posts = fetchedPosts
                    displayedPosts = fetchedPosts
                    selectedSortOption = "Recent"
                } catch (e: Exception) {
                    fetchError = "Failed to load posts: ${e.message}"
                    Log.e("DiscoveryPage", "Failed to load posts", e)
                } finally {
                    isLoading = false
                }
            }

            val defaultProfileImages = listOf(
                "https://i.imgur.com/DyFZblf.jpeg", // Gray square
                "https://i.imgur.com/kcbZfpx.png", // Smiley face
                "https://i.imgur.com/WvDsY4x.jpeg", // Simple avatar silhouette
                "https://i.imgur.com/iCy2JU1.jpeg", // Minimalist user icon
                "https://i.imgur.com/7hVHf5f.png"  // Abstract shape
            )

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

            if (fetchError != null) {
                Text(text = fetchError!!)
            } else if (posts.isEmpty()) {
                Text(text = "No posts yet.")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.animateContentSize()
                ) {
                    items(
                        items = displayedPosts,
                        key = { it.id }
                    ) { post ->
                        val defaultPfp = remember { post.user.profile_image ?: defaultProfileImages.random() }
                        AnimatedVisibility(
                            visible = !isLoading,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Post(
                                navController = navController,
                                authorId = post.user.auth_id,
                                postId = post.id,
                                profileImage = defaultPfp,
                                username = post.user.username ?: "Unknown User",
                                postImage = post.image_url,
                                caption = "${post.content}\n\nCategory: ${post.category}",
                                likeCount = 0,
                                commentCount = 0,
                                onCommentClicked = { navController.navigate("individual_post/${post.id}") },
                                onImageClick = { navController.navigate("individual_post/${post.id}") }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// Function to fetch like counts and sort posts by likes
private suspend fun fetchAndSortPostsByLikes(
    posts: List<DiscoveryPost>,
    likeManager: LikeManager,
    onComplete: (List<DiscoveryPost>) -> Unit
) {
    val postsWithLikes = mutableListOf<PostWithLikes>()
    
    for (post in posts) {
        val likeCount = likeManager.getLikeCount(post.id)
        postsWithLikes.add(PostWithLikes(post, likeCount))
    }
    
    // Sort by like count (descending)
    val sortedPosts = postsWithLikes.sortedByDescending { it.likeCount }.map { it.post }
    
    onComplete(sortedPosts)
}

// Function to fetch metrics, score and sort posts by the recommendation algorithm
private suspend fun fetchAndScorePosts(
    posts: List<DiscoveryPost>,
    likeManager: LikeManager,
    onComplete: (List<DiscoveryPost>) -> Unit
) {
    // First, collect like and comment counts for all posts
    val postsWithCounts = mutableListOf<DiscoveryPostWithCounts>()
    
    for (post in posts) {
        val likeCount = likeManager.getLikeCount(post.id)
        // For simplicity, we'll generate random comment counts
        // In a real app, you would fetch this from your database
        val commentCount = (0..5).random() // Simplified for demo
        
        postsWithCounts.add(DiscoveryPostWithCounts(post, likeCount, commentCount))
    }
    
    // Compute author engagement metrics
    val authorEngagement = RecommendationEngine.computeAuthorEngagement(postsWithCounts)
    
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

// Placeholder/Removed fetchAndScorePosts - RecommendationEngine needs integration
/*
suspend fun fetchAndScorePosts(
    posts: List<DiscoveryPost>,
    likeManager: LikeManager,
    onResult: (List<DiscoveryPost>) -> Unit
) = withContext(Dispatchers.IO) {
    // ... Implementation needed ...
    onResult(posts) // Placeholder
}
*/ 