package com.example.creativecommunity.pages

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.input.PasswordVisualTransformation

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
        try {
            val authId = SupabaseClient.client.auth.retrieveUserForCurrentSession().id
            val profile = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest.from("users")
                    .select(Columns.raw("username, profile_image, bio")) {
                        filter { eq("auth_id", authId) }
                    }
                    .decodeSingle<UserProfile>()
            }
            userProfile = profile
            editedUsername = profile.username
            editedBio = profile.bio ?: ""
        } catch (e: Exception) {
            error = "Failed to load profile: ${e.message}"
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
            
            // Debug log for email passed
            Log.d("ProfilePage", "Calling auth.updateUser with email='$email'")
            
            try {
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.auth.updateUser {
                        this.email = email
                    }
                }
                
                // Then, update email in the users table
                try {
                    val authId = SupabaseClient.client.auth.retrieveUserForCurrentSession().id
                    withContext(Dispatchers.IO) {
                        SupabaseClient.client.postgrest.from("users")
                            .update({
                                set("email", email)
                            }) {
                                filter { eq("auth_id", authId) }
                            }
                    }
                    Log.d("ProfilePage", "User table email updated successfully to: $email")
                } catch (e: Exception) {
                    Log.e("ProfilePage", "Failed to update email in user table: ${e.message}", e)
                    // We don't throw here because the auth update was successful
                    // Just log the error since the email will be verified and synced later
                }
                
                // Show confirmation and close dialog
                showEmailDialog = false
                newEmail = ""
                confirmEmail = ""
                // Show a success message (you could use a Snackbar or Toast here)
                Log.d("ProfilePage", "Email update initiated. Verification email sent to: $email")
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
            .padding(16.dp),
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
                // Profile Picture with Edit Button
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val buttonOffset = maxWidth * 0.25f  // Calculate 15% of screen width for button offset
                    
                    // Profile Picture Container
                    Box {
                        AsyncImage(
                            model = imgurImageURL ?: (userProfile!!.profileImage ?: "https://i.imgur.com/DyFZblf.jpeg"),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .clickable { showPfpDialog = true },
                            contentScale = ContentScale.Crop
                        )
                        if (isUploadingPfp) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    
                    // Edit Button (positioned absolutely)
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploadingPfp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = buttonOffset)  // Use calculated offset
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture"
                        )
                    }
                }

                // Profile Picture Expansion Dialog
                if (showPfpDialog) {
                    Dialog(onDismissRequest = { showPfpDialog = false }) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = imgurImageURL ?: (userProfile!!.profileImage ?: "https://i.imgur.com/DyFZblf.jpeg"),
                                contentDescription = "Enlarged profile picture",
                                modifier = Modifier
                                    .size(500.dp)
                                    .clickable { showPfpDialog = false }
                            )
                        }
                    }
                }

                // Email Change Dialog
                if (showEmailDialog) {
                    // Fetch current email when dialog opens
                    LaunchedEffect(Unit) {
                        try {
                            val user = SupabaseClient.client.auth.retrieveUserForCurrentSession()
                            currentEmail = user.email ?: ""
                        } catch (e: Exception) {
                            Log.e("ProfilePage", "Error fetching current email: ${e.message}", e)
                            currentEmail = ""
                        }
                    }
                    
                    Dialog(onDismissRequest = { 
                        showEmailDialog = false
                        newEmail = ""
                        confirmEmail = ""
                        emailChangeError = null
                    }) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Change Email",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            // Current email display
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Current email:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentEmail,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            OutlinedTextField(
                                value = newEmail,
                                onValueChange = { 
                                    newEmail = it
                                    emailChangeError = null 
                                },
                                label = { Text("New Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = emailChangeError != null
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = confirmEmail,
                                onValueChange = { 
                                    confirmEmail = it
                                    emailChangeError = null 
                                },
                                label = { Text("Confirm Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                isError = emailChangeError != null
                            )
                            
                            if (emailChangeError != null) {
                                Text(
                                    text = emailChangeError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { 
                                        showEmailDialog = false
                                        newEmail = ""
                                        confirmEmail = ""
                                        emailChangeError = null
                                    }
                                ) {
                                    Text("Cancel")
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(
                                    onClick = {
                                        if (newEmail.isBlank()) {
                                            emailChangeError = "Email cannot be empty"
                                            return@Button
                                        }
                                        if (confirmEmail.isBlank()) {
                                            emailChangeError = "Please confirm your email"
                                            return@Button
                                        }
                                        if (newEmail != confirmEmail) {
                                            emailChangeError = "Email addresses don't match"
                                            return@Button
                                        }
                                        // Debug log for email values
                                        Log.d("ProfilePage", "Update button clicked. newEmail='$newEmail', confirmEmail='$confirmEmail'")
                                        updateEmail(newEmail)
                                    },
                                    enabled = !isEmailChanging
                                ) {
                                    if (isEmailChanging) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Update")
                                    }
                                }
                            }
                        }
                    }
                }

                // Password Change Dialog
                if (showPasswordDialog) {
                    Dialog(onDismissRequest = {
                        showPasswordDialog = false
                        newPassword = ""
                        confirmPassword = ""
                        passwordChangeError = null
                    }) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Change Password", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                            
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it; passwordChangeError = null },
                                label = { Text("New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                isError = passwordChangeError != null
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; passwordChangeError = null },
                                label = { Text("Confirm Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                isError = passwordChangeError != null
                            )
                            if (passwordChangeError != null) {
                                Text(text = passwordChangeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showPasswordDialog = false; newPassword = ""; confirmPassword = ""; passwordChangeError = null }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    if (newPassword.isBlank()) { passwordChangeError = "Password cannot be empty"; return@Button }
                                    if (confirmPassword.isBlank()) { passwordChangeError = "Please confirm your password"; return@Button }
                                    if (newPassword != confirmPassword) { passwordChangeError = "Passwords don't match"; return@Button }
                                    updatePassword(newPassword)
                                }, enabled = !isPasswordChanging) {
                                    if (isPasswordChanging) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text("Update")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Username
                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = { 
                        editedUsername = it
                    },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
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
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Settings Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEmailDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Change Email", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Email")
                    }

                    Button(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Change Password", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Password")
                    }

                    Button(
                        onClick = { /* TODO: Handle about us */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Us",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About Us")
                    }
                    
                    // Saved Posts button
                    Button(
                        onClick = { navController.navigate("saved_posts") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "\uD83C\uDF1F Saved Posts",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    // My Posts button
                    Button(
                        onClick = { navController.navigate("my_posts") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "📝 My Posts",
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Save Changes Button (only shown when there are changes)
                if (hasChanges) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { saveChanges() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
} 