package com.example.creativecommunity.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Like(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("post_id") val postId: String, // Keep as String for now, will be converted to Int in DB calls
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LikeCount(
    val count: Int
)

// For tracking post likes in memory
data class PostLike(
    val postId: String, // Keep as String for compatibility with existing code
    val isLiked: Boolean,  // true for like, false for unlike
    val timestamp: Long = System.currentTimeMillis()
)

// For storing in local storage
@Serializable
data class PendingLikeAction(
    val postId: String, // Keep as String for compatibility with existing code
    val userId: String,
    val action: String, // "like" or "unlike"
    val timestamp: Long
) 