package com.example.creativecommunity.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.creativecommunity.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedButton
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush

@Composable
fun LoginPage(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignup by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var showTitleWithForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000) // Initial delay before showing title
        showTitle = true
        delay(2000) // Show initial title for 2 seconds
        showTitle = false
        delay(1000) // Brief pause before showing form
        showForm = true
        showTitleWithForm = true
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    // for screen sizes - different values for different screens; don't want super stretched UI
    val isLandscape = screenWidth > 600
    
    // DIFFERENT VALUES FOR DIFFERENT SCREN SIZES
    val maxFormWidth = if (isLandscape) 500.dp else screenWidth.dp
    val horizontalPadding = if (isLandscape) 32.dp else 20.dp
    val verticalPadding = if (isLandscape) 24.dp else 16.dp
    val titleFontSize = if (isLandscape) 48.sp else 40.sp
    val buttonFontSize = if (isLandscape) 18.sp else 16.sp
    val textFieldFontSize = if (isLandscape) 18.sp else 16.sp

    // Define gradient based on theme
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientColor = if (isSystemInDarkTheme()) Color(0xFF2A2A2A) else Color(0xFFF0F0F0) // Slightly darker/lighter than surface
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(gradientColor, surfaceColor)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush) // Apply the gradient background
            .padding(horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = showTitle && !showForm,
            enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
            exit = fadeOut(animationSpec = tween(durationMillis = 1000))
        ) {
            Text(
                text = "Muse",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
        AnimatedVisibility(
            visible = showForm,
            enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
            exit = fadeOut(animationSpec = tween(durationMillis = 1000))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(max = maxFormWidth)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(verticalPadding)
            ) {
                AnimatedVisibility(
                    visible = showTitleWithForm,
                    enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 1000))
                ) {
                    Text(
                        text = "Muse",
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", fontSize = textFieldFontSize) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = textFieldFontSize)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", fontSize = textFieldFontSize) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = textFieldFontSize)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                if (isSignup) {
                                    SupabaseClient.client.auth.signUpWith(Email) {
                                        this.email = email
                                        this.password = password
                                        this.data = kotlinx.serialization.json.buildJsonObject {
                                            put("username", kotlinx.serialization.json.JsonPrimitive(email.split("@")[0]))
                                        }
                                    }
                                    SupabaseClient.client.auth.signInWith(Email) {
                                        this.email = email
                                        this.password = password
                                    }
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    SupabaseClient.client.auth.signInWith(Email) {
                                        this.email = email
                                        this.password = password
                                    }
                                    navController.navigate("main") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                message = "${if (isSignup) "Signup" else "Login"} failed: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        if (isSignup) "Sign Up" else "Login",
                        color = Color.Black,
                        fontSize = buttonFontSize
                    )
                }
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { isSignup = !isSignup }) {
                    Text(
                        text = if (isSignup) "Already have an account? Login" else "Need an account? Sign Up",
                        fontSize = textFieldFontSize,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                }
                message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        it,
                        color = if (it.contains("failed")) MaterialTheme.colorScheme.error else Color.Black,
                        fontSize = textFieldFontSize
                    )
                }
            }
        }
    }
}