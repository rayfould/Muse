package com.example.creativecommunity.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("username") val username: String,
    @SerialName("profile_image") val profileImage: String?,
    @SerialName("bio") val bio: String?
) 