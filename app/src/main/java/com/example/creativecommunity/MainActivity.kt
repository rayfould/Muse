package com.example.creativecommunity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.creativecommunity.ui.theme.CreativeCommunityTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CreativeCommunityTheme {
                AppNavigation()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = SupabaseClient.client.auth.signUpWith(Email) {
                    email = "test.user123@gmail.com"
                    password = "password123"
                }
                Log.d("SupabaseTest", "Signup successful!")
            } catch (e: Exception) {
                Log.e("SupabaseTest", "Signup failed: ${e.message}")
            }
        }
    }


}
