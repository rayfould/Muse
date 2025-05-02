package com.example.creativecommunity.models

/**
 * Represents a badge earned by a user.
 *
 * @property name Unique identifier for the badge (e.g., "early_adopter").
 * @property title Display name shown to the user (e.g., "Early Adopter").
 * @property description Explanation of how the badge was earned.
 * @property iconName Name of the Material Icon
 */
data class Badge(
    val name: String, 
    val title: String, 
    val description: String,
    val iconName: String
) 