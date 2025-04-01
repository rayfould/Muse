package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.creativecommunity.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch


@Composable
fun LoginPage(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignup by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    // Email and Password fields
    {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Login \ Sign Up Button
        Button(
            onClick = {
                scope.launch {
                    try {
                        if (isSignup) {
                            SupabaseClient.client.auth.signUpWith(Email) {
                                this.email = email
                                this.password = password
                                this.data = kotlinx.serialization.json.buildJsonObject {
                                    //Create json object, before passing it, defaulting to a basic username from email
                                    put("username", kotlinx.serialization.json.JsonPrimitive(email.split("@")[0]))
                                }
                            }
                            message = "Signup successful!"
                        } else {
                            SupabaseClient.client.auth.signInWith(Email) {
                                this.email = email
                                this.password = password
                            }
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true } // Clear login from back stack
                            }
                        }
                    } catch (e: Exception) {
                        message = "${if (isSignup) "Signup" else "Login"} failed: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSignup) "Sign Up" else "Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { isSignup = !isSignup }) {
            Text(if (isSignup) "Already have an account? Login" else "Need an account? Sign Up")
        }
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                it,
                color = if (it.contains("failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}