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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun NewPostPage(navController: NavController, category: String) {
    val context = LocalContext.current

    // Submission serializable class
    @Serializable
    data class Submission(
        val user_id: Int,
        val prompt_id: Int,
        val content: String,
        val image_url: String,
        val category: String
    )

    // For caption
    var postCaption by remember { mutableStateOf(TextFieldValue("")) }

    // For photos
    var currentlyUploading by remember { mutableStateOf(false) }
    // shouldUpload - just boolean for checking if post button was clicked
    var shouldUpload by remember { mutableStateOf(false) }

    // contains Current image uploaded - use this to check if should call API (if no image then don't call it)
    var currImage by remember { mutableStateOf<Uri?>(null) }
    // this is our imageURL need to pass into db later...
    var imgurImageURL by remember { mutableStateOf<String?>(null) }

//    https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.GetContent
//    https://fvilarino.medium.com/using-activity-result-contracts-in-jetpack-compose-14b179fb87de
//    val imagePicker = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent(),
//        onResult = { uri ->
//            // 3
//            hasImage = uri != null
//            imageUri = uri
//        }
//    )
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        currImage = uri
        imgurImageURL = null
    }


    // Temp URI for camera capture
    val contentResolver = LocalContext.current.contentResolver
    val tempFileUri = remember {
        FileProvider.getUriForFile(context, "com.example.creativecommunity.fileprovider", java.io.File(context.cacheDir, "temp_image.jpg"))
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // currImage is set by the preview URI below
            currImage = tempFileUri // Set currImage after snap
            imgurImageURL = null
        }
    }




//https://github.com/AKiniyalocts/imgur-android/tree/master
    //https://stackoverflow.com/questions/13549559/getcontentresolver-openinputstreamuri-throws-filenotfoundexception
    suspend fun uploadImageToImgur(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        // Access android file system to read the image data
        val contentResolver = context.contentResolver
        val bytes = contentResolver.openInputStream(imageUri)?.use { stream ->
            stream.readBytes()
        }
        if (bytes == null) {
            Log.e("SupabaseTest", "Failed to read image bytes from URI: $imageUri")
            return@withContext null
        }
        Log.d("SupabaseTest", "Image bytes read: ${bytes.size / 1024} KB")

        // Convert to Base64 with NO_WRAP
        val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
        if (base64Image.isEmpty()) {
            Log.e("SupabaseTest", "Base64 encoding failed: empty string")
            return@withContext null
        }
        Log.d("SupabaseTest", "Base64 length: ${base64Image.length}, Sample: ${base64Image.take(50)}...")

        // Create HTTP request
        val okHttpClient = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("image", base64Image)
            .add("type", "base64") // Specify Base64 format
            .build()
        Log.d("SupabaseTest", "Request body size: ${requestBody.contentLength()} bytes")
        Log.d("SupabaseTest", "Using Client-ID: ${BuildConfig.IMGUR_CLIENT_ID.take(8)}...") // Partial for safety
        val request = Request.Builder()
            // add Imgur endpoint for uploading image, required Authorization header for api call
            .header("Authorization", "Client-ID ${BuildConfig.IMGUR_CLIENT_ID}")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", "MuseApp/1.0") // Add User-Agent to avoid server suspicion
            .post(requestBody)
            .url("https://api.imgur.com/3/image")
            .build()
        Log.d("SupabaseTest", "Request headers: ${request.headers}")
        Log.d("SupabaseTest", "Request URL: ${request.url}")

        // https://apidocs.imgur.com/
        val response = okHttpClient.newCall(request).execute() // makes a HTTP call
        val responseBody = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            Log.e("SupabaseTest", "Imgur upload failed: ${response.code} - ${response.message}")
            Log.e("SupabaseTest", "Response body: $responseBody")
            Log.d("SupabaseTest", "Response headers: ${response.headers}")
            return@withContext null
        }
        Log.d("SupabaseTest", "Response body: $responseBody")

        // extract json response from imgur fetch
        //        val json = response.body?.string()
        //        val linkRegex = """"link":"(.*?)"""".toRegex()
        //        return@withContext linkRegex.find(json ?: "")?.groups?.get(1)?.value?.replace("\\/", "/")

        // creates matching regex for looking for a link - like how used to do in python
        val linkRegex = """"link":"(.*?)"""".toRegex()
        // example response from Imgur parsing with regex
        // val json = """
        //{
        //  "data": {
        //    "id": "abc123",
        //    "title": null,
        //    "description": null,
        //    "datetime": 1641234567,
        //    "type": "image/jpeg",
        //    "animated": false,
        //    "width": 1024,
        //    "height": 768,
        //    "size": 123456,
        //    "views": 0,
        //    "bandwidth": 0,
        //    "deletehash": "XYZ789",
        //    "link": "https:\/\/i.imgur.com\/abc123.jpg"            <----
        //  },
        //  "success": true,
        //  "status": 200
        //}
        val matchResult = linkRegex.find(responseBody) // apply the regex to the link response
        val fetchedLink = matchResult?.groups?.get(1)?.value // gets actual link from matchResult
        val actualLink = fetchedLink?.replace("\\/", "/") // remove all escape keys
        //Log link issues
        if (actualLink == null) {
            Log.e("SupabaseTest", "Failed to extract link from Imgur response: $responseBody")
        }
        return@withContext actualLink // 'return' is not allowed here - made me add @withContext
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("You are creating a new '$category' post")
        Spacer(modifier = Modifier.height(5.dp))
        Text("This week's challenge: Paint a park near you!")
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Take a photo button
            Button(
                onClick = {
                    cameraLauncher.launch(tempFileUri)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Take a photo")
            }
            Text("or")
            Button(
                // replaced placeholder with image picker method
                onClick = { imagePickerLauncher.launch("image/*") }, // image/* means choose any image type file
                modifier = Modifier.weight(1f)
            ) {
                Text("Upload a photo")
            }
        }

        // Display our image
//        https://developer.android.com/develop/ui/compose/graphics/images/loading
//        AsyncImage(
//            model = "https://example.com/image.jpg",
//            contentDescription = "Translated description of what the image contains"
//        )
//        https://stackoverflow.com/questions/58606651/what-is-the-purpose-of-let-keyword-in-kotlin
//        let is one of Kotlin's Scope functions which allow you to execute a code block within the context of an object. In this case the context object is str. There are five of them: let, run, with, apply, and also. Their usages range from but are not exclusive to initialization and mapping.
        currImage?.let {
            AsyncImage(model = it, contentDescription = null, modifier = Modifier.height(100.dp))
        }
        imgurImageURL?.let {
            Text("Link to image on imgur: $it")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Caption input text field
        TextField(
            value = postCaption,
            onValueChange = { postCaption = it },
            label = { Text("Write your caption here...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )


        Spacer(modifier = Modifier.height(16.dp))

        // launch coroutine to upload image to imgur
        if (shouldUpload && currImage != null) { // MAKE SURE POST button clicked AND an image is actually selected
            LaunchedEffect(currImage) { // coroutine
                try {
                    val url = uploadImageToImgur(currImage!!) // upload to imgur if currImage not null
                    if (url != null) {
                        val authId = SupabaseClient.client.auth.retrieveUserForCurrentSession().id // UUID from auth.users
                        Log.d("SupabaseTest", "Auth ID: $authId")
                        val userResponse = SupabaseClient.client.postgrest.from("users")
                            .select(Columns.raw("id")) { filter { eq("auth_id", authId) } }
                            .decodeSingle<Map<String, Int>>() // Get user_id as integer
                        Log.d("SupabaseTest", "User response: $userResponse")
                        val userId = userResponse["id"] ?: throw Exception("User ID not found in response")
                        val mockPromptId = 1 // Replace with real prompt ID later
                        SupabaseClient.client.postgrest.from("submissions").insert(
                            Submission(
                                user_id = userId,
                                prompt_id = mockPromptId,
                                content = postCaption.text,
                                image_url = url,
                                category = category
                            )
                        )
                        imgurImageURL = "Post submitted successfully!" // Update UI with success message
                    } else {
                        imgurImageURL = "Upload failed - try again later" // More informative message
                        Log.e("SupabaseTest", "Post failed: Imgur returned null URL")
                    }
                } catch (e: Exception) {
                    imgurImageURL = "Error: ${e.message}" // Show error in UI
                    Log.e("SupabaseTest", "Upload crashed", e)
                }
                shouldUpload = false // prevent infinite loop for uploading, stop after image uploaded
                currentlyUploading = false // reset the button functionality --> redisplay "Post to Community" instead of uploading... forever
            }
        }

        // Post button
//        Button(
//            onClick = { /* Submit the post --> integrate with Supabase */ },
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Text("Post to the Community!")
//        }
        Button(
            onClick = {
                if (currImage != null && !currentlyUploading) {
                    shouldUpload = true
                    currentlyUploading = true
                    imgurImageURL = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currImage != null && !currentlyUploading // DISABLE BUTTON when upload in progress
        ) {
            Text(if (currentlyUploading) "Uploading..." else "Post to the Community!")
        }


        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = {
            // Acts as a "back" button, pops this page off the stack
            // The login page is defaulted as the starting page in the stack, goes back to the Login page
            navController.popBackStack()
        }) {
            Text("Back to $category Feed")
        }
    }
}
