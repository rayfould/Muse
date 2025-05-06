package dev.riss.muse.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    @SerialName("profile_image") val profile_image: String? = null,
    @SerialName("username") val username: String? = "Unknown User"
) 