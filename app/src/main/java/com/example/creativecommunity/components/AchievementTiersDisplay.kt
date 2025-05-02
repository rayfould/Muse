package com.example.creativecommunity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// --- Data Structures ---

enum class AchievementCategory {
    POSTS, COMMENTS, LIKES_RECEIVED, SAVES_RECEIVED, ACCOUNT_AGE
}

data class TierInfo(
    val tier: Int, // 0, 1, 2, 3
    val name: String, // e.g., "Bronze", "Silver", "Gold"
    val color: Color,
    val nextThreshold: Long? // Value needed for next tier (null if max tier)
)

data class AchievementProgress(
    val category: AchievementCategory,
    val icon: ImageVector,
    val currentValue: Long,
    val currentTierInfo: TierInfo,
    val categoryDisplayName: String // e.g., "Posts Made"
)

// --- REVISED Tier Calculation Logic ---

object AchievementTiersRevised {
    // Define thresholds required to REACH each tier (Tier 1, Tier 2, Tier 3)
    val POSTS_THRESHOLDS = listOf(10L, 25L, 50L)
    val COMMENTS_THRESHOLDS = listOf(10L, 50L, 100L)
    val LIKES_THRESHOLDS = listOf(10L, 50L, 100L)
    val SAVES_THRESHOLDS = listOf(1L, 10L, 50L)
    val AGE_THRESHOLDS_DAYS = listOf(30L, 180L, 365L)

    // Define Tier names and colors
    val TIER_NAMES = listOf("Bronze", "Silver", "Gold")
    val TIER_COLORS = listOf(Color(0xFFCD7F32), Color(0xFFC0C0C0), Color(0xFFFFD700))
    val TIER_ZERO_COLOR = Color.Gray

    // Function to get current tier (0-3) based on value and thresholds
    fun getCurrentTierIndex(value: Long, thresholds: List<Long>): Int {
        var tier = 0
        for (threshold in thresholds) {
            if (value >= threshold) {
                tier++
            } else {
                break
            }
        }
        return tier
    }

    fun getTierInfo(tierIndex: Int): TierInfo {
        return if (tierIndex > 0 && tierIndex <= TIER_NAMES.size) {
             TierInfo(
                 tier = tierIndex,
                 name = TIER_NAMES[tierIndex - 1],
                 color = TIER_COLORS[tierIndex - 1],
                 nextThreshold = null // We'll calculate this based on category if needed
             )
        } else {
            TierInfo(0, "Unranked", TIER_ZERO_COLOR, null)
        }
    }

    fun getProgress(category: AchievementCategory, value: Long, createdAt: Instant? = null): AchievementProgress {
        val (thresholds, icon, displayName) = when (category) {
            AchievementCategory.POSTS -> Triple(POSTS_THRESHOLDS, Icons.Filled.Create, "Posts Made")
            AchievementCategory.COMMENTS -> Triple(COMMENTS_THRESHOLDS, Icons.Filled.ChatBubble, "Comments Made")
            AchievementCategory.LIKES_RECEIVED -> Triple(LIKES_THRESHOLDS, Icons.Filled.Favorite, "Likes Received")
            AchievementCategory.SAVES_RECEIVED -> Triple(SAVES_THRESHOLDS, Icons.Filled.Bookmark, "Posts Saved")
            AchievementCategory.ACCOUNT_AGE -> Triple(AGE_THRESHOLDS_DAYS, Icons.Filled.Cake, "Account Age")
        }

        val currentValue = if (category == AchievementCategory.ACCOUNT_AGE && createdAt != null) {
            ChronoUnit.DAYS.between(createdAt, Instant.now())
        } else {
            value
        }

        val currentTierIndex = getCurrentTierIndex(currentValue, thresholds)
        val currentTierInfoBase = getTierInfo(currentTierIndex)

        // Calculate next threshold if not max tier
        val nextThreshold = if (currentTierIndex < thresholds.size) thresholds[currentTierIndex] else null
        val currentTierInfo = currentTierInfoBase.copy(nextThreshold = nextThreshold)


        return AchievementProgress(
            category = category,
            icon = icon,
            currentValue = currentValue, // Use calculated age for age category
            currentTierInfo = currentTierInfo,
            categoryDisplayName = displayName
        )
    }
}


// --- Composable ---

@OptIn(ExperimentalMaterial3Api::class) // For AlertDialog
@Composable
fun AchievementTiersDisplay(
    postCount: Long,
    commentCount: Long,
    likesReceived: Long,
    savesReceived: Long,
    accountCreatedAt: Instant?
) {
    val progressList = remember(postCount, commentCount, likesReceived, savesReceived, accountCreatedAt) {
        listOfNotNull(
            AchievementTiersRevised.getProgress(AchievementCategory.POSTS, postCount),
            AchievementTiersRevised.getProgress(AchievementCategory.COMMENTS, commentCount),
            AchievementTiersRevised.getProgress(AchievementCategory.LIKES_RECEIVED, likesReceived),
            AchievementTiersRevised.getProgress(AchievementCategory.SAVES_RECEIVED, savesReceived),
            accountCreatedAt?.let { AchievementTiersRevised.getProgress(AchievementCategory.ACCOUNT_AGE, 0, it) }
        )
    }

    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedProgress by remember { mutableStateOf<AchievementProgress?>(null) }

    // --- ADD Surface Wrapper ---
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Keep vertical padding outside surface
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant, // Add background color
        shadowElevation = 2.dp // Optional subtle shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp), // Add padding inside surface
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            progressList.forEach { progress ->
                AchievementIcon(
                    progress = progress,
                    onClick = {
                        selectedProgress = progress
                        showDetailsDialog = true
                    }
                )
            }
        }
    }

    // Details Dialog
    if (showDetailsDialog && selectedProgress != null) {
        val currentVal = selectedProgress!!.currentValue
        val nextThresh = selectedProgress!!.currentTierInfo.nextThreshold
        val progressText = when (selectedProgress!!.category) {
             AchievementCategory.ACCOUNT_AGE -> "Current Age: ${currentVal} days" + (nextThresh?.let { ". Next tier at $it days." } ?: ". Max tier reached!")
             else -> "Current: $currentVal" + (nextThresh?.let { ". Next tier at $it." } ?: ". Max tier reached!")
         }
        // --- ADD Description Text ---
        val descriptionText = getAchievementDescription(selectedProgress!!.category)

        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            icon = { Icon(selectedProgress!!.icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = selectedProgress!!.currentTierInfo.color) },
            title = { Text(selectedProgress!!.categoryDisplayName, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Tier: ${selectedProgress!!.currentTierInfo.name}", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(progressText)
                    // --- ADD Description ---
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(descriptionText, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// --- ADD Helper Function for Description ---
private fun getAchievementDescription(category: AchievementCategory): String {
    return when (category) {
        AchievementCategory.POSTS -> "Tracks the total number of posts you have created."
        AchievementCategory.COMMENTS -> "Tracks the total number of comments you have made on posts."
        AchievementCategory.LIKES_RECEIVED -> "Tracks the total number of likes your posts have received from others."
        AchievementCategory.SAVES_RECEIVED -> "Tracks the total number of times your posts have been saved by others."
        AchievementCategory.ACCOUNT_AGE -> "Tracks how long ago your account was created (in days)."
    }
}

@Composable
private fun AchievementIcon(
    progress: AchievementProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = progress.currentTierInfo.color
    val iconSize = 36.dp

    Box(
        modifier = modifier
            // Apply size and padding *before* clip
            .size(iconSize + 8.dp) // Total size (e.g., 44dp)
            .padding(4.dp)         // Padding for the content inside the final size
            .clip(CircleShape)     // Clip to circle
            .clickable(onClick = onClick), // Clickable area is the circle
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = progress.icon,
            contentDescription = "${progress.categoryDisplayName}: ${progress.currentTierInfo.name}",
            modifier = Modifier.size(iconSize),
            tint = tierColor // Use tier color directly
        )
        // Optional: Add a tier indicator overlay (e.g., stars, number)
        if (progress.currentTierInfo.tier > 0) {
             // Example: Small text number indicator - customize as needed
             Text(
                 text = progress.currentTierInfo.tier.toString(),
                 color = Color.White, // Contrasting color
                 fontWeight = FontWeight.Bold,
                 style = MaterialTheme.typography.labelSmall,
                 modifier = Modifier
                     .align(Alignment.BottomEnd)
                     // Adjust offset slightly inwards if needed
                     .offset(x = (-2).dp, y = (-2).dp) 
                     .background(tierColor, CircleShape)
                     .padding(horizontal = 4.dp, vertical = 1.dp)
             )
        }

    }
} 