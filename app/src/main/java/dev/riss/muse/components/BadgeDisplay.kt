package dev.riss.muse.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.riss.muse.models.Badge

/**
 * Maps a badge icon name (String) to its corresponding Material Icon Vector.
 * TODO: Expand this mapping or use a more robust method if many icons are needed.
 */
fun getIconForBadge(iconName: String): ImageVector {
    return when (iconName) {
        "VerifiedUser" -> Icons.Filled.VerifiedUser
        "MilitaryTech" -> Icons.Filled.MilitaryTech
        "EmojiEvents" -> Icons.Filled.EmojiEvents
        "Stars" -> Icons.Filled.Stars
        "RocketLaunch" -> Icons.Filled.RocketLaunch
        "Create" -> Icons.Filled.Create
        "ChatBubble" -> Icons.Filled.ChatBubble
        "Favorite" -> Icons.Filled.Favorite
        "Whatshot" -> Icons.Filled.Whatshot
        // Add more mappings as needed
        else -> Icons.Filled.Star // Default icon
    }
}

/**
 * Displays a list of badges as icons in a FlowRow.
 * Shows a dialog with badge details when an icon is clicked.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BadgeBoard(badges: List<Badge>) {
    var showBadgeDialog by remember { mutableStateOf(false) }
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        if (badges.isEmpty()) {
            Text(
                text = "No badges earned yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { badge ->
                    Icon(
                        imageVector = getIconForBadge(badge.iconName),
                        contentDescription = badge.title,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                selectedBadge = badge
                                showBadgeDialog = true
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Badge Details Dialog
    if (showBadgeDialog && selectedBadge != null) {
        AlertDialog(
            onDismissRequest = { showBadgeDialog = false },
            icon = { Icon(getIconForBadge(selectedBadge!!.iconName), contentDescription = null, modifier = Modifier.size(48.dp)) },
            title = { Text(selectedBadge!!.title, fontWeight = FontWeight.Bold) },
            text = { Text(selectedBadge!!.description) },
            confirmButton = {
                TextButton(onClick = { showBadgeDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
} 