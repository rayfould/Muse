package dev.riss.muse.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.riss.muse.SupabaseClient
import dev.riss.muse.models.CommentIdOnly
import dev.riss.muse.models.DiscoveryPost
import dev.riss.muse.models.DiscoveryPostWithCounts
import dev.riss.muse.models.Post
import dev.riss.muse.models.PostMetrics
import dev.riss.muse.models.RecommendationEngine
import dev.riss.muse.models.ScoredPost
import dev.riss.muse.utils.LikeManager
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeParseException

// Data class to hold post with its like count for sorting
data class PostWithLikes(
    val post: DiscoveryPost,
    val likeCount: Int
)

@Composable
fun DiscoveryPage(navController: NavController) {
    // State variables
    var showSortDropdown by remember { mutableStateOf(false) }
    var selectedSortOption by remember { mutableStateOf("Recent") }
    var isLoading by remember { mutableStateOf(false) }
    var posts by remember { mutableStateOf<List<DiscoveryPost>>(emptyList()) }
    var displayedPosts by remember { mutableStateOf<List<DiscoveryPost>>(emptyList()) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var showPostImageDialog by remember { mutableStateOf(false) }
    var selectedPostImageUrl by remember { mutableStateOf<String?>(null) }
    var commentCounts by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val likeManager = remember { LikeManager.getInstance(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isPhoneMode = screenWidth < 600

    // Default profile images list
    val defaultProfileImages = listOf(
        "https://i.imgur.com/DyFZblf.jpeg", // Gray square
        "https://i.imgur.com/kcbZfpx.png", // Smiley face
        "https://i.imgur.com/WvDsY4x.jpeg", // Simple avatar silhouette
        "https://i.imgur.com/iCy2JU1.jpeg", // Minimalist user icon
        "https://i.imgur.com/7hVHf5f.png"  // Abstract shape
    )

    // Post image dialog
    if (showPostImageDialog && selectedPostImageUrl != null) {
        Dialog(onDismissRequest = { showPostImageDialog = false }) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedPostImageUrl,
                    contentDescription = "Enlarged post image",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable { showPostImageDialog = false },
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }

    // Fetch posts on initial load
    LaunchedEffect(Unit) {
        isLoading = true
        fetchError = null
        try {
            val fetchedPosts = withContext(Dispatchers.IO) {
                val result = SupabaseClient.client.postgrest.from("posts")
                    .select(Columns.raw("id, image_url, content, category, created_at, user_id, users!inner(id, username, email, profile_image, bio, auth_id)")) {
                        order("created_at", Order.DESCENDING)
                        limit(50)
                    }
                val postsList = result.decodeList<DiscoveryPost>()
                Log.d("DiscoveryPage", "Fetched ${postsList.size} posts")
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
            Log.e("DiscoveryPage", "Failed to load posts", e)
        } finally {
            isLoading = false
        }
    }

    // Sorting logic moved inside Composable
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
                        "Most Liked" -> {
                            val postsWithLikes = posts.map { post -> // post is DiscoveryPost here
                                val likeCount = likeManager.getLikeCount(post.id) // This might be slow
                                PostWithLikes(post, likeCount)
                            }
                            postsWithLikes.sortedByDescending { it.likeCount }.map { it.post }
                        }
                        "Recommended" -> {
                            // 1. Map (Like counts might be slow)
                            val postsWithCounts = posts.map {
                                DiscoveryPostWithCounts(
                                    post = it,
                                    likeCount = likeManager.getLikeCount(it.id),
                                    commentCount = commentCounts[it.id] ?: 0
                                )
                            }
                            // 2. Compute Engagement (Might be CPU intensive)
                            val authorEngagement = RecommendationEngine.computeAuthorEngagement(postsWithCounts)
                            // 3. Calculate Scores (Might be CPU intensive)
                            val scoredPosts = postsWithCounts.map { postWithCounts ->
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
                                ScoredPost(postWithCounts.post, metrics, score)
                            }
                            // 4. Sort by Score
                            scoredPosts.sortedByDescending { it.score }.map { it.post }
                        }
                        "Random" -> {
                            delay(50) // Small delay for visual consistency
                            posts.shuffled()
                        }
                        else -> {
                            delay(50) // Small delay for visual consistency
                            posts
                        }
                    }
                 }
                displayedPosts = sortedResult
                listState.scrollToItem(0) // Scroll to top after sorting
            } catch (e: Exception) {
                fetchError = "Failed to sort posts: ${e.message}"
                Log.e("DiscoveryPage", "Sort error", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with opacity background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    Text(
                        text = "Posts For You",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )

                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
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
            }
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
                    Text("No posts to discover yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(displayedPosts, key = { post -> post.id }) { post ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Post(
                                navController = navController,
                                authorId = post.user.auth_id,
                                postId = post.id,
                                profileImage = post.user.profile_image ?: defaultProfileImages.random(),
                                username = post.user.username,
                                postImage = post.image_url,
                                caption = post.content,
                                likeCount = 0,
                                commentCount = commentCounts[post.id] ?: 0,
                                onCommentClicked = { navController.navigate("individual_post/${post.id}") },
                                onImageClick = { 
                                    selectedPostImageUrl = post.image_url
                                    showPostImageDialog = true
                                }
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
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