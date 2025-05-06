package com.example.creativecommunity.pages

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.BuildConfig
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.creativecommunity.components.BadgeBoard
import com.example.creativecommunity.models.Badge
import com.example.creativecommunity.components.AchievementTiersDisplay
import java.time.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilePage(navController: NavController) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // For profile picture expansion
    var showPfpDialog by remember { mutableStateOf(false) }
    
    // For email change dialog
    var showEmailDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var confirmEmail by remember { mutableStateOf("") }
    var isEmailChanging by remember { mutableStateOf(false) }
    var emailChangeError by remember { mutableStateOf<String?>(null) }
    var currentEmail by remember { mutableStateOf("") }
    
    // For password change dialog
    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordChanging by remember { mutableStateOf(false) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    // Editable fields
    var editedUsername by remember { mutableStateOf("") }
    var editedBio by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // For profile picture upload
    var currImage by remember { mutableStateOf<Uri?>(null) }
    var imgurImageURL by remember { mutableStateOf<String?>(null) }
    var isUploadingPfp by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        currImage = uri
        imgurImageURL = null
    }

    // Add a scroll state
    val scrollState = rememberScrollState()

    suspend fun uploadImageToImgur(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val bytes = contentResolver.openInputStream(imageUri)?.use { stream ->
            stream.readBytes()
        }
        if (bytes == null) {
            Log.e("SupabaseTest", "Failed to read image bytes from URI: $imageUri")
            return@withContext null
        }

        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (base64Image.isEmpty()) {
            Log.e("SupabaseTest", "Base64 encoding failed: empty string")
            return@withContext null
        }

        val okHttpClient = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("image", base64Image)
            .add("type", "base64")
            .build()

        val request = Request.Builder()
            .header("Authorization", "Client-ID ${BuildConfig.IMGUR_CLIENT_ID}")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "MuseApp/1.0")
            .post(requestBody)
            .url("https://api.imgur.com/3/image")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.e("SupabaseTest", "Imgur upload failed: ${response.code} - ${response.message}")
            return@withContext null
        }

        val linkRegex = """"link":"(.*?)"""".toRegex()
        val matchResult = linkRegex.find(responseBody)
        val fetchedLink = matchResult?.groups?.get(1)?.value
        fetchedLink?.replace("\\/", "/")
    }

    LaunchedEffect(currImage) {
        if (currImage != null && !isUploadingPfp) {
            isUploadingPfp = true
            try {
                val url = uploadImageToImgur(currImage!!)
                if (url != null) {
                    imgurImageURL = url
                    hasChanges = true
                } else {
                    error = "Failed to upload profile picture"
                }
            } catch (e: Exception) {
                error = "Failed to upload profile picture: ${e.message}"
            } finally {
                isUploadingPfp = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        error = null
        try {
            // Fetch the current auth user session
            val currentUser = SupabaseClient.client.auth.retrieveUserForCurrentSession()
            val authId = currentUser.id
            val emailFromAuth = currentUser.email ?: "N/A" // Get email, handle null
            
            val profileFromView = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest
                    .from("user_stats")
                    .select {
                         filter { eq("auth_id", authId) } 
                    }
                    .decodeSingle<UserProfile>()
            }
            userProfile = profileFromView
            
            editedUsername = profileFromView.username
            editedBio = profileFromView.bio ?: ""
            currentEmail = emailFromAuth // Store the fetched email in the state

        } catch (e: Exception) {
            error = "Failed to load profile stats: ${e.message}"
            Log.e("ProfilePageLoad", "Error loading profile stats from view", e)
        } finally {
            isLoading = false
        }
    }

    // Watch for changes
    LaunchedEffect(editedUsername, editedBio, imgurImageURL) {
        hasChanges = editedUsername != userProfile?.username || 
                    editedBio != (userProfile?.bio ?: "") ||
                    (imgurImageURL != null && imgurImageURL != userProfile?.profileImage)
    }

    fun saveChanges() {
        scope.launch {
            isSaving = true
            try {
                val authId = SupabaseClient.client.auth.retrieveUserForCurrentSession().id
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.postgrest.from("users")
                        .update({
                            set("username", editedUsername)
                            set("bio", editedBio)
                            if (imgurImageURL != null) {
                                set("profile_image", imgurImageURL)
                            }
                        }) {
                            filter { eq("auth_id", authId) }
                        }
                }
                // Update local state
                userProfile = userProfile?.copy(
                    username = editedUsername,
                    bio = editedBio,
                    profileImage = imgurImageURL ?: userProfile?.profileImage
                )
                hasChanges = false
                imgurImageURL = null
            } catch (e: Exception) {
                error = "Failed to save changes: ${e.message}"
            } finally {
                isSaving = false
            }
        }
    }

    // Function to update email
    fun updateEmail(email: String) {
        scope.launch {
            isEmailChanging = true
            emailChangeError = null
            
            try {
                // Update the auth table using the proper auth API
                Log.d("ProfilePage", "Attempting to update auth email to: $email")
                val response = withContext(Dispatchers.IO) {
                    SupabaseClient.client.auth.updateUser {
                        this.email = email
                    }
                }
                
                Log.d("ProfilePage", "Auth update response: ${response?.toString() ?: "null"}")
                
                // If auth update was successful, update the users table
                if (response != null) {
                    val authId = SupabaseClient.client.auth.retrieveUserForCurrentSession().id
                    Log.d("ProfilePage", "Auth update successful, proceeding to update users table for auth_id: $authId")
                    
                    withContext(Dispatchers.IO) {
                        SupabaseClient.client.postgrest.from("users")
                            .update({
                                set("email", email)
                            }) {
                                filter { eq("auth_id", authId) }
                            }
                    }
                    
                    // Show confirmation and close dialog
                    showEmailDialog = false
                    newEmail = ""
                    confirmEmail = ""
                    Log.d("ProfilePage", "Email updated successfully in both auth and users tables")
                } else {
                    Log.e("ProfilePage", "Auth update returned null response")
                    throw Exception("Failed to update auth email")
                }
            } catch (e: Exception) {
                Log.e("ProfilePage", "Error updating email: ${e.message}", e)
                emailChangeError = "Failed to update email: ${e.message}"
            } finally {
                isEmailChanging = false
            }
        }
    }

    // Function to update password
    fun updatePassword(password: String) {
        scope.launch {
            isPasswordChanging = true
            passwordChangeError = null
            try {
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.auth.updateUser {
                        this.password = password
                    }
                }
                showPasswordDialog = false
                newPassword = ""
                confirmPassword = ""
                Log.d("ProfilePage", "Password update initiated.")
            } catch (e: Exception) {
                Log.e("ProfilePage", "Error updating password: ${e.message}", e)
                passwordChangeError = "Failed to update password: ${e.message}"
            } finally {
                isPasswordChanging = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }
            userProfile != null -> {
                // Use BoxWithConstraints to determine layout based on width
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val breakpoint = 600.dp
                    val isWideScreen = maxWidth >= breakpoint

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        // Parse createdAt safely for the display component, assuming UTC if no timezone
                        val parsedCreatedAt = remember(userProfile?.createdAt) { 
                            userProfile?.createdAt?.let { 
                                // Simplify: Replace space with 'T' and always append 'Z'
                                val timestampString = it.replace(' ', 'T') + "Z"
                                Log.d("TimestampParse", "Attempting to parse (Simplified): $timestampString") 
                                runCatching { 
                                    Instant.parse(timestampString) 
                                }.onFailure { e -> // Add logging for failure
                                    Log.e("TimestampParse", "Failed to parse timestamp: $timestampString", e)
                                }.getOrNull() 
                            }
                        }

                        if (isWideScreen) {
                            // Wide Screen Layout: Row for PFP/Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Left side: Profile Picture
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProfilePictureSection(
                                        profileImageUrl = imgurImageURL ?: (userProfile!!.profileImage ?: "https://i.imgur.com/DyFZblf.jpeg"),
                                        isUploading = isUploadingPfp,
                                        onPfpClick = { showPfpDialog = true },
                                        onEditClick = { imagePickerLauncher.launch("image/*") }
                                    )
                                }

                                // Right side: Username & Bio Fields in a Column
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Username
                                    OutlinedTextField(
                                        value = editedUsername,
                                        onValueChange = { editedUsername = it },
                                        label = { Text("Username") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            textColor = MaterialTheme.colorScheme.onSurface,
                                            cursorColor = MaterialTheme.colorScheme.primary,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )

                                    // Bio
                                    OutlinedTextField(
                                        value = editedBio,
                                        onValueChange = { editedBio = it },
                                        label = { Text("Bio") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 120.dp),
                                        minLines = 3,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            textColor = MaterialTheme.colorScheme.onSurface,
                                            cursorColor = MaterialTheme.colorScheme.primary,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // --- Use AchievementTiersDisplay with data from userProfile state --- 
                            AchievementTiersDisplay(
                                postCount = userProfile!!.postCount,
                                commentCount = userProfile!!.commentCount,
                                likesReceived = userProfile!!.likesReceivedCount,
                                savesReceived = userProfile!!.savesReceivedCount,
                                accountCreatedAt = parsedCreatedAt
                            )
                            // --- End Component Usage --- 

                        } else {
                            // Narrow Screen Layout: Original Column layout
                            // Profile Picture with Edit Button
                            Box(
                                modifier = Modifier.size(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ProfilePictureSection(
                                    profileImageUrl = imgurImageURL ?: (userProfile!!.profileImage ?: "https://i.imgur.com/DyFZblf.jpeg"),
                                    isUploading = isUploadingPfp,
                                    onPfpClick = { showPfpDialog = true },
                                    onEditClick = { imagePickerLauncher.launch("image/*") }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            // --- Use AchievementTiersDisplay with data from userProfile state --- 
                            AchievementTiersDisplay(
                                postCount = userProfile!!.postCount,
                                commentCount = userProfile!!.commentCount,
                                likesReceived = userProfile!!.likesReceivedCount,
                                savesReceived = userProfile!!.savesReceivedCount,
                                accountCreatedAt = parsedCreatedAt
                            )
                            // --- End Component Usage --- 
                            // Username
                            OutlinedTextField(
                                value = editedUsername,
                                onValueChange = {
                                    editedUsername = it
                                },
                                label = { Text("Username") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bio
                            OutlinedTextField(
                                value = editedBio,
                                onValueChange = {
                                    editedBio = it
                                },
                                label = { Text("Bio") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }

                        // Common elements below PFP/Info/Badges section
                        Spacer(modifier = Modifier.height(32.dp))

                        // Settings Buttons
                        if (isWideScreen) {
                            // Wide Screen: Use FlowRow for buttons
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 3 // Example: Limit items per row for better structure
                            ) {
                                SettingsButtons(
                                    navController,
                                    showEmailDialog = { showEmailDialog = it },
                                    showPasswordDialog = { showPasswordDialog = it }
                                )
                            }
                        } else {
                            // Narrow Screen: Use Column
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SettingsButtons(
                                    navController,
                                    showEmailDialog = { showEmailDialog = it },
                                    showPasswordDialog = { showPasswordDialog = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp)) // Adjusted spacer

                        // Logout Button
                        Button(
                            onClick = {
                                scope.launch {
                                    SupabaseClient.client.auth.signOut()
                                    navController.navigate("login") { 
                                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                    }
                                }
                            },
                            // Apply fixed width and height
                            modifier = Modifier.height(48.dp).width(180.dp), 
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.error)
                        ) {
                            // Wrap in Row for centering
                            Row(
                                modifier = Modifier.fillMaxWidth(), // Row fills button
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Logout", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Save Changes Button (only shown when there are changes)
                        if (hasChanges) {
                            Button(
                                onClick = { saveChanges() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = !isSaving
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(), 
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    // Dialog to show enlarged profile picture
    if (showPfpDialog) {
        Dialog(onDismissRequest = { showPfpDialog = false }) {
            // Use a Surface for better dialog appearance (optional background/shape)
            Surface(
                shape = RoundedCornerShape(16.dp),
                 // Optional: Add a background color if needed, otherwise defaults
                 // color = MaterialTheme.colorScheme.surface 
            ) {
                 // Use the latest image URL (either uploaded or from profile)
                val imageUrlToShow = imgurImageURL ?: userProfile?.profileImage ?: "https://i.imgur.com/DyFZblf.jpeg" // Fallback

                AsyncImage(
                    model = imageUrlToShow,
                    contentDescription = "Enlarged Profile Picture",
                    modifier = Modifier
                        .sizeIn(maxHeight = 500.dp, maxWidth = 500.dp) // Limit size
                        .clip(RoundedCornerShape(16.dp)) // Clip image inside surface
                        .clickable { showPfpDialog = false }, // Click image to dismiss
                    contentScale = ContentScale.Fit // Fit within the bounds
                )
            }
        }
    }

    // Email Change Dialog
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEmailDialog = false
                emailChangeError = null // Clear error on dismiss
            },
            // Explicitly set colors for better contrast
            containerColor = MaterialTheme.colorScheme.surface, 
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            // --- End of color setting ---
            title = { 
                 // Explicitly set title color
                Text("Change Email", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column {
                    // --- Add Text for Current Email inside Dialog ---
                     Text(
                        text = "Current: $currentEmail",
                        style = MaterialTheme.typography.bodyMedium, // Use slightly smaller style inside dialog
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), // Slightly faded
                        modifier = Modifier.padding(bottom = 12.dp) // Add spacing below
                    )
                    // --- End Text for Current Email ---
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New Email") },
                        isError = emailChangeError != null,
                        singleLine = true,
                        // Set TextField colors for contrast
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmEmail,
                        onValueChange = { confirmEmail = it },
                        label = { Text("Confirm New Email") },
                        isError = emailChangeError != null,
                        singleLine = true,
                        // Set TextField colors for contrast
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    if (emailChangeError != null) {
                        Text(
                            text = emailChangeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (newEmail.isBlank() || confirmEmail.isBlank()) {
                            emailChangeError = "Emails cannot be empty."
                        } else if (newEmail != confirmEmail) {
                            emailChangeError = "Emails do not match."
                        } else {
                            // Consider adding more robust email validation here
                            updateEmail(newEmail)
                        }
                    },
                    enabled = !isEmailChanging,
                    // Explicitly set button text color
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isEmailChanging) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Update") // Text color now controlled by ButtonDefaults
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showEmailDialog = false 
                        emailChangeError = null
                    },
                    // Explicitly set button text color
                     colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel") // Text color now controlled by ButtonDefaults
                }
            }
        )
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPasswordDialog = false 
                passwordChangeError = null // Clear error on dismiss
            },
             // Explicitly set colors for better contrast
            containerColor = MaterialTheme.colorScheme.surface, 
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            // --- End of color setting ---
            title = { 
                // Explicitly set title color
                Text("Change Password", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordChangeError != null,
                        singleLine = true,
                        // Set TextField colors for contrast
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordChangeError != null,
                        singleLine = true,
                        // Set TextField colors for contrast
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                     if (passwordChangeError != null) {
                        Text(
                            text = passwordChangeError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        if (newPassword.isBlank() || confirmPassword.isBlank()) {
                            passwordChangeError = "Passwords cannot be empty."
                        } else if (newPassword.length < 6) { // Example validation
                            passwordChangeError = "Password must be at least 6 characters."
                        } else if (newPassword != confirmPassword) {
                            passwordChangeError = "Passwords do not match."
                        } else {
                            updatePassword(newPassword)
                        }
                     },
                     enabled = !isPasswordChanging,
                     // Explicitly set button text color
                     colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                     if (isPasswordChanging) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Update") // Text color now controlled by ButtonDefaults
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPasswordDialog = false
                        passwordChangeError = null
                    },
                    // Explicitly set button text color
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel") // Text color now controlled by ButtonDefaults
                }
            }
        )
    }
}

// Shared composable for the main settings buttons
@Composable
private fun SettingsButtons(
    navController: NavController,
    showEmailDialog: (Boolean) -> Unit,
    showPasswordDialog: (Boolean) -> Unit
) {
    val buttonWidth = 180.dp // Define a common width

    Button(
        onClick = { showEmailDialog(true) },
        modifier = Modifier.height(48.dp).width(buttonWidth), // Apply fixed width
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        // Wrap content in Row for centering
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Email, contentDescription = "Change Email", modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Email", style = MaterialTheme.typography.labelLarge)
        }
    }

    Button(
        onClick = { showPasswordDialog(true) },
        modifier = Modifier.height(48.dp).width(buttonWidth), // Apply fixed width
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        // Wrap content in Row for centering
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Change Password", modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Password", style = MaterialTheme.typography.labelLarge)
        }
    }

    Button(
        onClick = { navController.navigate("about_us") },
        modifier = Modifier.height(48.dp).width(buttonWidth), // Apply fixed width
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        // Wrap content in Row for centering
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About Us",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("About Us", style = MaterialTheme.typography.labelLarge)
        }
    }

    // Saved Posts button
    Button(
        onClick = { navController.navigate("saved_posts") },
        modifier = Modifier.height(48.dp).width(buttonWidth), // Apply fixed width
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        // Wrap content in Row for centering
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "\uD83C\uDF1F Saved Posts",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    // My Posts button
    Button(
        onClick = { navController.navigate("my_posts") },
        modifier = Modifier.height(48.dp).width(buttonWidth), // Apply fixed width
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        // Wrap content in Row for centering
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📝 My Posts",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// Extracted composable for Profile Picture and Edit Button
@Composable
private fun ProfilePictureSection(
    profileImageUrl: String,
    isUploading: Boolean,
    onPfpClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable(onClick = onPfpClick),
                contentScale = ContentScale.Crop
            )
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.Center)
                )
            }
        }
        IconButton(
            onClick = onEditClick,
            enabled = !isUploading,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (10).dp, y = (10).dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Profile Picture",
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 