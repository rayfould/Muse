package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue
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
    var postsWithLikes by remember { mutableStateOf<List<PostWithLikes>>(emptyList()) }
    var displayedPosts by remember { mutableStateOf<List<DiscoveryPost>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val likeManager = remember { LikeManager.getInstance(context) }

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
                Text(text = "Discover")
                
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
                try {
                    val fetchedPosts = withContext(Dispatchers.IO) {
                        val result = SupabaseClient.client.postgrest.from("posts")
                            .select(Columns.raw("id, image_url, content, category, created_at, user_id, users!inner(profile_image, username)")) {
                                order("created_at", Order.DESCENDING)
                                limit(20)  // Limit to 20 most recent posts
                            }
                        val postsList = result.decodeList<DiscoveryPost>()
                        Log.d("SupabaseTest", "Fetched posts: $postsList")
                        postsList
                    }
                    posts = fetchedPosts
                    displayedPosts = fetchedPosts
                } catch (e: Exception) {
                    fetchError = "Failed to load posts: ${e.message}"
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
                        key = { it.id } // Use id as key instead of image_url for better stability
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
                                caption = "${post.content}\n\nCategory: ${post.category}",
                                likeCount = 0, // Initial value, will be updated by Post component
                                commentCount = 0,
                                onCommentClicked = {
                                    navController.navigate("individual_post/${post.id}")
                                },
                                onProfileClick = {
                                    selectedPfpUrl = defaultPfp
                                    showPfpDialog = true
                                }
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