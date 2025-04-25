package com.example.creativecommunity.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.UserProfile
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ViewProfilePage(navController: NavController, userId: String) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            Log.d("ViewProfilePage", "Fetching profile for userId: $userId")
            val profile = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest.from("users")
                    .select(Columns.raw("username, profile_image, bio")) { // Use UserProfile fields
                        filter { eq("auth_id", userId) }
                    }
                    .decodeSingle<UserProfile>() // Decode into UserProfile
            }
            userProfile = profile
            Log.d("ViewProfilePage", "Successfully fetched profile: ${profile.username}")
        } catch (e: Exception) {
            Log.e("ViewProfilePage", "Failed to load profile for userId: $userId", e)
            error = "Failed to load profile: ${e.message}"
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
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = userProfile!!.username, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Badge Board", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Their earned badges will appear here soon!")
                            // TODO: Implement badge display logic based on userId
                        }
                    }
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
                Text(text = "User profile not found.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
} 