package com.example.creativecommunity.pages


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// UI
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@Composable
fun MainPage(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Muse!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Dive into your creative community",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // changed mapOf to listOf
        val categories = listOf(
            Pair("ART", "Visual Arts"),
            Pair("CODING", "Programming"),
            Pair("ENGINEERING", "Engineering Projects"),
            Pair("PHOTO", "Photography"),
            Pair("WRITING", "Creative Writing"),
            Pair("MUSIC", "Music Creation"),
            Pair("CRAFTS", "Handmade Crafts"),
            Pair("COOKING", "Culinary Arts"),
            Pair("FILM", "Filmmaking"),
            Pair("SCIENCE", "Science Experiments")
        )

        // Now using LazyVerticalGrid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { (key, displayName) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { navController.navigate("category_feed/$key") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    // looks nicer if elevate card off of background
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}