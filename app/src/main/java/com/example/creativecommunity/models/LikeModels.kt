package com.example.creativecommunity.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Like(
    val id: String? = null,
    @SerialName("user_id") val userId: Int,
    @SerialName("post_id") val postId: Int,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LikeCount(
    val count: Int
)

// For tracking post likes in memory
data class PostLike(
    val postId: Int, // Changed from String to Int
    val isLiked: Boolean,  // true for like, false for unlike
    val timestamp: Long = System.currentTimeMillis(),
    val ownUpdate: Boolean = false // Track if update is from current user
)

// For storing in local storage
@Serializable
data class PendingLikeAction(
    val postId: Int,
    val userId: String, // This is the auth_id from Supabase Auth
    val action: String, // "like" or "unlike"
    val timestamp: Long
)

// For tracking like updates
@Serializable
data class LikeUpdate(
    val postId: Int,
    val isLiked: Boolean,
    val ownUpdate: Boolean = false // Track if the update is from current user
) 