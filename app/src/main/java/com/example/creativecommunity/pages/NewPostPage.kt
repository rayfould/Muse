package com.example.creativecommunity.pages

import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.creativecommunity.BuildConfig
import com.example.creativecommunity.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun NewPostPage(navController: NavController, category: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Post serializable class
    @Serializable
    data class Post(
        val user_id: Int,
        val prompt_id: Int,
        val content: String,
        val image_url: String,
        val category: String
    )

    // For caption
    var postCaption by remember { mutableStateOf("") }
    val maxCaptionLength = 300

    // For photos
    var currentlyUploading by remember { mutableStateOf(false) }
    var shouldUpload by remember { mutableStateOf(false) }
    var currImage by remember { mutableStateOf<Uri?>(null) }
    var imgurImageURL by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        currImage = uri
        imgurImageURL = null
    }

    // Ensure tempFileUri is created only once per composable instance
    val tempFileUri = remember {
        FileProvider.getUriForFile(
            context,
            "com.example.creativecommunity.fileprovider",
            java.io.File(context.cacheDir, "temp_image.jpg")
        )
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currImage = tempFileUri
            imgurImageURL = null
        }
    }

    val cardShape: Shape = RoundedCornerShape(20.dp)
    val imageShape: Shape = RoundedCornerShape(16.dp)

    suspend fun uploadImageToImgur(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val bytes = contentResolver.openInputStream(imageUri)?.use { stream ->
            stream.readBytes()
        }
        if (bytes == null) return@withContext null
        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (base64Image.isEmpty()) return@withContext null
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
        if (!response.isSuccessful) return@withContext null
        val linkRegex = """"link":"(.*?)"""".toRegex()
        val matchResult = linkRegex.find(responseBody)
        val fetchedLink = matchResult?.groups?.get(1)?.value
        val actualLink = fetchedLink?.replace("\\/", "/")
        return@withContext actualLink
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text("Create a New Post", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp))
                Text("Category: $category", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, cardShape),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Step 1: Add a Photo", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ElevatedButton(
                                onClick = { cameraLauncher.launch(tempFileUri) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Take Photo", modifier = Modifier.padding(end = 4.dp))
                                Text("Take Photo")
                            }
                            ElevatedButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Upload Photo", modifier = Modifier.padding(end = 4.dp))
                                Text("Upload Photo")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        currImage?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .height(220.dp)
                                    .fillMaxWidth()
                                    .clip(imageShape)
                                    .shadow(8.dp, imageShape)
                            )
                        } ?: Text("No image selected", color = Color.Gray, modifier = Modifier.padding(8.dp))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(4.dp, cardShape),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Step 2: Write a Caption", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = postCaption,
                            onValueChange = {
                                if (it.length <= maxCaptionLength) postCaption = it
                            },
                            label = { Text("Caption (max $maxCaptionLength chars)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text("${postCaption.length}/$maxCaptionLength", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                ElevatedButton(
                    onClick = {
                        if (currImage == null) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Please select an image.") }
                            return@ElevatedButton
                        }
                        if (postCaption.isBlank()) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("Please enter a caption.") }
                            return@ElevatedButton
                        }
                        if (!currentlyUploading) {
                            shouldUpload = true
                            currentlyUploading = true
                            imgurImageURL = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !currentlyUploading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (currentlyUploading) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Posting...")
                    } else {
                        Text("Post to Community")
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Back to $category Feed")
                }
            }
            // Feedback dialogs
            item {
                if (showSuccessDialog) {
                    AlertDialog(
                        onDismissRequest = { showSuccessDialog = false; navController.popBackStack() },
                        title = { Text("Success!") },
                        text = { Text("Your post was submitted successfully.") },
                        confirmButton = {
                            ElevatedButton(onClick = { showSuccessDialog = false; navController.popBackStack() }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
            item {
                if (showErrorDialog != null) {
                    AlertDialog(
                        onDismissRequest = { showErrorDialog = null },
                        title = { Text("Error") },
                        text = { Text(showErrorDialog ?: "") },
                        confirmButton = {
                            ElevatedButton(onClick = { showErrorDialog = null }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
        // Upload logic
        if (shouldUpload && currImage != null) {
            LaunchedEffect(currImage, shouldUpload) {
                try {
                    val url = uploadImageToImgur(currImage!!)
                    if (url != null) {
                        val authId = SupabaseClient.client.auth.retrieveUserForCurrentSession().id
                        val userResponse = SupabaseClient.client.postgrest.from("users")
                            .select(Columns.raw("id")) { filter { eq("auth_id", authId) } }
                            .decodeSingle<Map<String, Int>>()
                        val userId = userResponse["id"] ?: throw Exception("User ID not found in response")
                        val mockPromptId = 1 // Replace with real prompt ID later
                        SupabaseClient.client.postgrest.from("posts").insert(
                            Post(
                                user_id = userId,
                                prompt_id = mockPromptId,
                                content = postCaption,
                                image_url = url,
                                category = category
                            )
                        )
                        showSuccessDialog = true
                    } else {
                        showErrorDialog = "Image upload failed. Please try again."
                    }
                } catch (e: Exception) {
                    showErrorDialog = "Error: ${e.message}"
                }
                shouldUpload = false
                currentlyUploading = false
            }
        }
    }
}
