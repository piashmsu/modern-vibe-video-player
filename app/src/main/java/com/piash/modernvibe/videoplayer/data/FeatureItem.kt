package com.piash.modernvibe.videoplayer.data

/**
 * A single user-tunable feature toggle. Backed by SharedPreferences.
 */
data class FeatureItem(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val defaultEnabled: Boolean = false
)
