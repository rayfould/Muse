package com.example.creativecommunity.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalConfiguration
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
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun MainPage(navController: NavController) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    // for screen sizes - different values for different screens; don't want super stretched UI
    val isWideScreen = screenWidth > 600
    
    // DIFFERENT VALUES FOR DIFFERENT SCREEN SIZES
    val horizontalPadding = if (isWideScreen) 32.dp else 20.dp
    val cardHeight = if (isWideScreen) 160.dp else 140.dp
    val gridSpacing = if (isWideScreen) 20.dp else 16.dp
    val titleFontSize = if (isWideScreen) 24.sp else 20.sp
    val subtitleFontSize = if (isWideScreen) 18.sp else 16.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 8.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome to Muse!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = titleFontSize
                )
                Text(
                    text = "Dive into your creative community",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = subtitleFontSize
                )
            }
        }

        // Add an image URL for each category
        // adding photos to the background of the cards
        val categories = listOf(
            Triple("ART", "Visual Arts", "https://i.imgur.com/J3b8X5J.jpeg"),
            Triple("CODING", "Programming", "https://images.unsplash.com/photo-1519389950473-47ba0277781c?auto=format&fit=crop&w=400&q=80"),
            Triple("ENGINEERING", "Engineering Projects", "https://i.imgur.com/zpLrgNL.jpeg"),
            Triple("PHOTO", "Photography", "https://images.unsplash.com/photo-1465101046530-73398c7f28ca?auto=format&fit=crop&w=400&q=80"),
            Triple("WRITING", "Creative Writing", "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?auto=format&fit=crop&w=400&q=80"),
            Triple("MUSIC", "Music Creation", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=400&q=80"),
            Triple("CRAFTS", "Handmade Crafts", "https://i.imgur.com/RWeAiGV.jpeg"),
            Triple("COOKING", "Culinary Arts", "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=400&q=80"),
            Triple("FILM", "Filmmaking", "https://i.imgur.com/T0baZzI.jpeg"),
            Triple("SCIENCE", "Science Experiments", "https://i.imgur.com/GJ2d9PP.gif")
        )

        // Now using LazyVerticalGrid with responsive columns
        LazyVerticalGrid(
            columns = if (isWideScreen) GridCells.Fixed(4) else GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(categories) { (key, displayName, imageUrl) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .clickable { navController.navigate("category_feed/$key") },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = CardDefaults.cardElevation(5.dp)
                ) {
                    // Box for images
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = if (isWideScreen) 18.sp else 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}