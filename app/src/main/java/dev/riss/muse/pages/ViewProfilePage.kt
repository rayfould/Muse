package dev.riss.muse.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.riss.muse.SupabaseClient
import dev.riss.muse.models.UserProfile
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.clickable
import dev.riss.muse.components.AchievementTiersDisplay
import java.time.Instant

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewProfilePage(navController: NavController, userId: String) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPfpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        userProfile = null

        try {
            Log.d("ViewProfilePage", "Fetching profile stats for userId: $userId")
            val profileFromView = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest
                    .from("user_stats")
                    .select() {
                        filter { eq("auth_id", userId) }
                    }
                    .decodeSingleOrNull<UserProfile>()
            }
            userProfile = profileFromView
            Log.d("ViewProfilePage", "Successfully fetched profile stats: ${profileFromView?.username}")

            if (profileFromView == null) {
                error = "User profile not found."
            }

        } catch (e: Exception) {
            Log.e("ViewProfilePage", "Failed to load profile stats for userId: $userId", e)
            error = "Failed to load profile stats: ${e.message}"
            userProfile = null
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
        when {
            isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error != null -> {
                Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            }
            userProfile != null -> {
                // Parse createdAt safely, assuming UTC if no timezone
                val parsedCreatedAt = remember(userProfile?.createdAt) { 
                    userProfile?.createdAt?.let { 
                        // Simplify: Replace space with 'T' and always append 'Z'
                        val timestampString = it.replace(' ', 'T') + "Z"
                        Log.d("TimestampParseView", "Attempting to parse (Simplified): $timestampString") 
                        runCatching { 
                            Instant.parse(timestampString) 
                        }.onFailure { e -> 
                            Log.e("TimestampParseView", "Failed to parse timestamp: $timestampString", e)
                        }.getOrNull() 
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    AsyncImage(
                        model = userProfile!!.profileImage ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile photo for ${userProfile!!.username}",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { showPfpDialog = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = userProfile!!.username, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    AchievementTiersDisplay(
                        postCount = userProfile!!.postCount,
                        commentCount = userProfile!!.commentCount,
                        likesReceived = userProfile!!.likesReceivedCount,
                        savesReceived = userProfile!!.savesReceivedCount,
                        accountCreatedAt = parsedCreatedAt
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = userProfile!!.bio ?: "No bio yet.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(onClick = { 
                        navController.navigate("user_posts/$userId") 
                    }) {
                        Text(text = "View Posts")
                    }
                }
            }
            else -> {
                // Error message is already shown via the error state, but we could add 
                // a specific "User not found" message here if needed.
            }
        }
    }

    if (showPfpDialog) {
        Dialog(onDismissRequest = { showPfpDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
            ) {
                val imageUrlToShow = userProfile?.profileImage ?: "https://i.imgur.com/DyFZblf.jpeg" // Fallback

                AsyncImage(
                    model = imageUrlToShow,
                    contentDescription = "Enlarged Profile Picture",
                    modifier = Modifier
                        .sizeIn(maxHeight = 500.dp, maxWidth = 500.dp) 
                        .clip(RoundedCornerShape(16.dp)) 
                        .clickable { showPfpDialog = false }, 
                    contentScale = ContentScale.Fit 
                )
            }
        }
    }
} 