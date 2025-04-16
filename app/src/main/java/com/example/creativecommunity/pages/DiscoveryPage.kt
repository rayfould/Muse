package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun DiscoveryPage(navController: NavController) {
    Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        Text("Discovery Page")
    }
} 