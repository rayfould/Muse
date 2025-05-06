package dev.riss.muse.pages

// UI
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// Import the new colors
import dev.riss.muse.ui.theme.DeepAquaContainer
import dev.riss.muse.ui.theme.OnDeepAquaContainer
// Add graphicsLayer import
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun MainPage(navController: NavController) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    // for screen sizes - different values for different screens; don't want super stretched UI
    val isWideScreen = screenWidth > 600
    
    // DIFFERENT VALUES FOR DIFFERENT SCREEN SIZES
    val horizontalPadding = if (isWideScreen) 32.dp else 20.dp
    val cardHeight = if (isWideScreen) 150.dp else 130.dp
    val gridSpacing = if (isWideScreen) 20.dp else 16.dp
    val titleFontSize = if (isWideScreen) 24.sp else 20.sp
    val subtitleFontSize = if (isWideScreen) 18.sp else 16.sp
    val iconSize = if (isWideScreen) 48.dp else 40.dp
    val cardTextSize = if (isWideScreen) 16.sp else 14.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Add status bar padding
            .statusBarsPadding()
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .graphicsLayer { shadowElevation = 4.dp.toPx() }
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Welcome to Muse!",
                    style = MaterialTheme.typography.headlineLarge,
                    // Use Primary color
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = titleFontSize
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dive into your creative community",
                    style = MaterialTheme.typography.titleMedium,
                    // Use onSurface color
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), // Keep slight transparency
                    fontSize = subtitleFontSize,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Updated categories list with Icons instead of URLs
        val categories = listOf(
            Triple("ART", "Visual Arts", Icons.Outlined.Palette),
            Triple("CODING", "Programming", Icons.Outlined.Code),
            Triple("ENGINEERING", "Engineering", Icons.Outlined.Build),
            Triple("PHOTO", "Photography", Icons.Outlined.PhotoCamera),
            Triple("WRITING", "Writing", Icons.Outlined.Create),
            Triple("MUSIC", "Music", Icons.Outlined.MusicNote),
            Triple("CRAFTS", "Crafts", Icons.Outlined.ContentCut),
            Triple("COOKING", "Cooking", Icons.Outlined.Restaurant),
            Triple("FILM", "Filmmaking", Icons.Outlined.Videocam),
            Triple("SCIENCE", "Science", Icons.Outlined.Science)
        )

        LazyVerticalGrid(
            columns = if (isWideScreen) GridCells.Fixed(4) else GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(gridSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(categories) { index, (key, displayName, iconVector) ->
                val usePrimaryContainer = index % 2 == 0
                // Use primaryContainer or the new DeepAquaContainer
                val containerColor = if (usePrimaryContainer) MaterialTheme.colorScheme.primaryContainer else DeepAquaContainer
                // Use onPrimaryContainer or the new OnDeepAquaContainer
                val contentColor = if (usePrimaryContainer) MaterialTheme.colorScheme.onPrimaryContainer else OnDeepAquaContainer

                Card(
                    modifier = Modifier
                        .height(cardHeight)
                        .clickable { navController.navigate("category_feed/$key") },
                    colors = CardDefaults.cardColors(
                        containerColor = containerColor,
                        contentColor = contentColor
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = "$displayName Icon",
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = cardTextSize,
                            textAlign = TextAlign.Center,
                            color = contentColor
                        )
                    }
                }
            }
            item { 
                Spacer(modifier = Modifier.height(80.dp)) // Adjust height as needed
            }
        }
    }
}