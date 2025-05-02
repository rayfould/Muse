package com.example.creativecommunity.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("username") val username: String,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("post_count") val postCount: Long = 0,
    @SerialName("comment_count") val commentCount: Long = 0,
    @SerialName("likes_received_count") val likesReceivedCount: Long = 0,
    @SerialName("saves_received_count") val savesReceivedCount: Long = 0,
    @SerialName("auth_id") val authId: String? = null,
    @SerialName("user_id") val userId: Int? = null
) 