package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalConfiguration
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
    val users: UserInfo, // Who made the comment
    val parent_id: Int? = null // Add parent_id for threading
)

// Data for comments - need to serialize data --> supabase
@Serializable
data class NewComment(
    val user_id: Int,
    val post_id: Int,
    val content: String,
    val parent_id: Int? = null // Add parent_id for threading
)

// FOR COMMENT REPLYING - using parent attribute in comment table to store "parent" - comments are linked together as a linked list
// The original comments to the post will have no parent, with any replies therein having the comment id as the parent id
@Composable
fun ThreadedComments(
    comments: List<PostComment>,
    parentId: Int? = null,
    onReply: (PostComment) -> Unit,
    indentLevel: Int = 0
) {
    val children = comments.filter { it.parent_id == parentId }
    children.forEach { comment ->
        Comment(
            profileImage = comment.users.profile_image ?: "https://via.placeholder.com/40",
            username = comment.users.username,
            commentText = comment.content,
            onReplyClicked = { onReply(comment) },
            indentLevel = indentLevel
        )
        androidx.compose.material3.Divider()
        ThreadedComments(
            comments = comments,
            parentId = comment.id,
            onReply = onReply,
            indentLevel = indentLevel + 1
        )
    }
}

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

    var replyingToComment by remember { mutableStateOf<PostComment?>(null) }
    var showCommentBox by remember { mutableStateOf(true) }

    // New state variables for edit/delete functionality
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedContent by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isWideScreen = screenWidth > 900
    val maxContentWidth = if (isWideScreen) 600.dp else screenWidth.dp

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
                     .select(Columns.raw("id, content, parent_id, users!inner(id, username, email, profile_image, bio, auth_id)")) { // Fetch comment user info too
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Opacity background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        )

        // Main content
        Column(modifier = Modifier.fillMaxSize()) {
            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth).align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                
                // Show menu button only if current user is the post owner
                if (currentAuthId == postDetails?.users?.auth_id) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Post") },
                                onClick = {
                                    showMenu = false
                                    isEditing = true
                                    editedContent = postDetails?.content ?: ""
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = maxContentWidth)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display the Post itself
                            item {
                                if (isEditing) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        OutlinedTextField(
                                            value = editedContent,
                                            onValueChange = { editedContent = it },
                                            label = { Text("Edit caption") },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !isUpdating
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { isEditing = false },
                                                enabled = !isUpdating
                                            ) {
                                                Text("Cancel")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isUpdating = true
                                                        try {
                                                            val result = withContext(Dispatchers.IO) {
                                                                SupabaseClient.client.postgrest["posts"]
                                                                    .update({
                                                                        set("content", editedContent)
                                                                    }) {
                                                                        filter { eq("id", postIdInt!!) }
                                                                    }
                                                            }
                                                            postDetails = postDetails?.copy(content = editedContent)
                                                            isEditing = false
                                                        } catch (e: Exception) {
                                                            error = "Failed to update post: ${e.message}"
                                                        } finally {
                                                            isUpdating = false
                                                        }
                                                    }
                                                },
                                                enabled = !isUpdating && editedContent.isNotEmpty()
                                            ) {
                                                Text("Save")
                                            }
                                        }
                                    }
                                } else {
                                    Post(
                                        navController = navController,
                                        authorId = postDetails!!.users.auth_id,
                                        postId = postDetails!!.id,
                                        profileImage = postDetails!!.users.profile_image ?: "https://via.placeholder.com/40",
                                        username = postDetails!!.users.username,
                                        postImage = postDetails!!.image_url,
                                        caption = postDetails!!.content ?: "",
                                        likeCount = 0,
                                        commentCount = comments.size,
                                        onImageClick = { }
                                    )
                                }
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
                                item {
                                    ThreadedComments(
                                        comments = comments,
                                        onReply = { comment ->
                                            replyingToComment = comment
                                            showCommentBox = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (currentAuthId != null && postIdInt != null && showCommentBox) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp) // Use horizontal padding for consistency
                                .widthIn(max = maxContentWidth) // Constrain width like the LazyColumn content
                                .align(Alignment.CenterHorizontally), // Center the input area horizontally within the parent Column
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (replyingToComment != null) {
                                Text("Replying to @${replyingToComment!!.users.username}", color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            OutlinedTextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                label = { Text("Add a comment...") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSubmitting
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Button(
                                    onClick = {
                                        // Only cancel the reply, don't hide the box
                                        // showCommentBox = false 
                                        submitError = null
                                        replyingToComment = null
                                    },
                                    // Only enable when replying and not submitting
                                    enabled = replyingToComment != null && !isSubmitting 
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (commentInput.isNotBlank()) {
                                            isSubmitting = true
                                            submitError = null
                                            coroutineScope.launch {
                                                try {
                                                    val userId = withContext(Dispatchers.IO) {
                                                        val response = SupabaseClient.client.postgrest["users"]
                                                            .select { filter { eq("auth_id", currentAuthId) } }
                                                        val users = response.decodeList<com.example.creativecommunity.models.UserInfo>()
                                                        users.firstOrNull()?.id
                                                    }
                                                    if (userId != null) {
                                                        withContext(Dispatchers.IO) {
                                                            SupabaseClient.client.postgrest["comments"].insert(
                                                                NewComment(
                                                                    user_id = userId,
                                                                    post_id = postIdInt,
                                                                    content = commentInput,
                                                                    parent_id = replyingToComment?.id
                                                                )
                                                            )
                                                        }
                                                        val commentsResult = withContext(Dispatchers.IO) {
                                                            SupabaseClient.client.postgrest.from("comments")
                                                                .select(Columns.raw("id, content, parent_id, users!inner(id, username, email, profile_image, bio, auth_id)")) {
                                                                    filter { eq("post_id", postIdInt) }
                                                                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                                                                }
                                                                .decodeList<PostComment>()
                                                        }
                                                        comments = commentsResult
                                                        commentInput = ""
                                                        replyingToComment = null
                                                        showCommentBox = false // Optionally hide after successful post
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
                                ) {
                                    Text(if (isSubmitting) "Posting..." else "Post")
                                }
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

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Post") },
                text = { Text("Are you sure you want to delete this post? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isDeleting = true
                                try {
                                    withContext(Dispatchers.IO) {
                                        // First, delete all likes for this post
                                        SupabaseClient.client.postgrest["likes"]
                                            .delete {
                                                filter { eq("post_id", postIdInt!!) }
                                            }
                                        // Then, delete all comments for this post
                                        SupabaseClient.client.postgrest["comments"]
                                            .delete {
                                                filter { eq("post_id", postIdInt!!) }
                                            }
                                        // Then, delete the post itself
                                        SupabaseClient.client.postgrest["posts"]
                                            .delete {
                                                filter { eq("id", postIdInt!!) }
                                            }
                                    }
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    error = "Failed to delete post: ${e.message}"
                                } finally {
                                    isDeleting = false
                                    showDeleteDialog = false
                                }
                            }
                        },
                        enabled = !isDeleting
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}