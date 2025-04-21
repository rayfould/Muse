package com.example.creativecommunity.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.creativecommunity.SupabaseClient
import com.example.creativecommunity.models.Like
import com.example.creativecommunity.models.LikeCount
import com.example.creativecommunity.models.LikeUpdate
import com.example.creativecommunity.models.PendingLikeAction
import com.example.creativecommunity.models.PostLike
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

// Extension property for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "like_preferences")

// Helper class to get user ID from auth_id
@Serializable
private data class UserIdResponse(
    val id: Int
)

/**
 * Manages like operations with batching and local storage
 * Implemented as a singleton to ensure only one instance exists
 */
class LikeManager private constructor(private val context: Context) {
    private val TAG = "LikeManager"
    private val PENDING_LIKES_KEY = stringPreferencesKey("pending_likes")
    private val FLUSH_INTERVAL_MS = 5000L // 5 seconds
    private val INSTANCE_ID = System.currentTimeMillis() // Unique ID for this instance for logging
    
    // In-memory list for pending like operations
    private val pendingLikes = mutableListOf<PendingLikeAction>()
    
    // State flow to track the current state of post likes
    private val _postLikes = MutableStateFlow<List<PostLike>>(emptyList())
    val postLikes: StateFlow<List<PostLike>> = _postLikes.asStateFlow()
    
    // For like updates
    private val _likeUpdates = MutableSharedFlow<LikeUpdate>(replay = 0)
    val likeUpdates: SharedFlow<LikeUpdate> = _likeUpdates
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    init {
        Log.d(TAG, "[$INSTANCE_ID] Initializing LikeManager singleton")
        // Start a periodic flush job
        coroutineScope.launch {
            // First, recover any pending likes from storage in case the app was killed
            recoverPendingLikes()
            
            // Then start the periodic flush
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                try {
                    flush()
                } catch (e: Exception) {
                    Log.e(TAG, "[$INSTANCE_ID] Error during periodic flush: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Get the numeric user ID from auth ID
     */
    private suspend fun getUserIdFromAuthId(authId: String): Int? {
        return try {
            val result = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest.from("users")
                    .select {
                        filter {
                            eq("auth_id", authId)
                        }
                    }
                    .decodeList<UserIdResponse>()
            }
            
            if (result.isNotEmpty()) {
                result.first().id
            } else {
                Log.e(TAG, "[$INSTANCE_ID] No user found with auth_id $authId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$INSTANCE_ID] Error getting user ID for auth_id $authId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Queue a like action for batched processing
     */
    fun queueLike(authId: String, postId: Int, isLiked: Boolean) {
        Log.d(TAG, "[$INSTANCE_ID] Queueing ${if (isLiked) "like" else "unlike"} for post $postId by auth_id $authId")
        
        // Create a pending action
        val action = PendingLikeAction(
            postId = postId,
            userId = authId, // Store auth_id temporarily
            action = if (isLiked) "like" else "unlike",
            timestamp = System.currentTimeMillis()
        )
        
        synchronized(pendingLikes) {
            // Remove any pending actions for this post/user
            pendingLikes.removeAll { it.postId == postId && it.userId == authId }
            // Add the new action
            pendingLikes.add(action)
        }
        
        // Update our internal state immediately (optimistic update)
        updateLikeState(postId, isLiked, true)
        
        // Emit the update for UI with ownUpdate flag to prevent double counting
        coroutineScope.launch {
            _likeUpdates.emit(LikeUpdate(postId, isLiked, ownUpdate = true))
            
            // Store in persistent storage in case app is killed
            saveQueue()
            
            // Force a flush immediately for this like action
            try {
                flush()
            } catch (e: Exception) {
                Log.e(TAG, "[$INSTANCE_ID] Error during immediate flush: ${e.message}", e)
            }
        }
    }
    
    // Helper to update the internal state
    private fun updateLikeState(postId: Int, isLiked: Boolean, ownUpdate: Boolean) {
        Log.d(TAG, "[$INSTANCE_ID] Updating like state for post $postId to isLiked=$isLiked (ownUpdate=$ownUpdate)")
        _postLikes.value = _postLikes.value.filterNot { it.postId == postId } + 
                          PostLike(postId, isLiked, System.currentTimeMillis(), ownUpdate)
    }
    
    /**
     * Get whether the post is liked by the current user
     */
    suspend fun isPostLikedByUser(authId: String, postId: Int): Boolean {
        // Check internal state first
        _postLikes.value.find { it.postId == postId }?.let {
            Log.d(TAG, "[$INSTANCE_ID] Using cached like state for post $postId: isLiked=${it.isLiked}")
            return it.isLiked
        }
        
        // Get numeric user ID
        val userId = getUserIdFromAuthId(authId) ?: return false
        
        // Otherwise query database
        return try {
            Log.d(TAG, "[$INSTANCE_ID] Querying database for like status of post $postId for user $userId")
            val result = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest.from("likes")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("post_id", postId)
                        }
                    }
                    .decodeList<Like>()
            }
            
            val isLiked = result.isNotEmpty()
            Log.d(TAG, "[$INSTANCE_ID] Database result for post $postId: isLiked=$isLiked (${result.size} likes found)")
            
            // Update our internal state
            updateLikeState(postId, isLiked, false)
            isLiked
        } catch (e: Exception) {
            Log.e(TAG, "[$INSTANCE_ID] Error checking if post is liked: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get the like count for a post
     */
    suspend fun getLikeCount(postId: Int): Int {
        return try {
            Log.d(TAG, "[$INSTANCE_ID] Getting like count for post $postId")
            
            // Use a simpler approach - just get all likes for this post and count them
            val likes = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest.from("likes")
                    .select {
                        filter {
                            eq("post_id", postId)
                        }
                    }
                    .decodeList<Like>()
            }
            
            val count = likes.size
            Log.d(TAG, "[$INSTANCE_ID] Retrieved like count for post $postId: $count")
            count
        } catch (e: Exception) {
            Log.e(TAG, "[$INSTANCE_ID] Error getting like count: ${e.message}", e)
            0
        }
    }
    
    /**
     * Refresh like status for multiple posts
     */
    suspend fun refreshLikeStatus(authId: String, postIds: List<Int>) {
        if (postIds.isEmpty()) return
        
        // Get numeric user ID
        val userId = getUserIdFromAuthId(authId) ?: return
        
        try {
            Log.d(TAG, "[$INSTANCE_ID] Refreshing like status for ${postIds.size} posts")
            
            // Get all likes for these posts for this user - making individual calls for each post
            val likedPosts = withContext(Dispatchers.IO) {
                val result = mutableListOf<Like>()
                
                for (postId in postIds) {
                    try {
                        val likes = SupabaseClient.client.postgrest.from("likes")
                            .select {
                                filter {
                                    eq("user_id", userId)
                                    eq("post_id", postId)
                                }
                            }
                            .decodeList<Like>()
                        
                        result.addAll(likes)
                    } catch (e: Exception) {
                        Log.e(TAG, "[$INSTANCE_ID] Error fetching like status for post $postId: ${e.message}")
                    }
                }
                
                result.map { it.postId }.toSet()
            }
            
            // Update our internal state for all posts
            postIds.forEach { postId ->
                val isLiked = likedPosts.contains(postId)
                updateLikeState(postId, isLiked, false)
            }
            
            Log.d(TAG, "[$INSTANCE_ID] Successfully refreshed like status for ${postIds.size} posts")
        } catch (e: Exception) {
            Log.e(TAG, "[$INSTANCE_ID] Error refreshing like status: ${e.message}", e)
        }
    }
    
    /**
     * Check if a like already exists in the database
     */
    private suspend fun checkLikeExists(authId: String, postId: Int): Boolean {
        val userId = getUserIdFromAuthId(authId) ?: return false
        
        return try {
            withContext(Dispatchers.IO) {
                val result = SupabaseClient.client.postgrest.from("likes")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("post_id", postId)
                        }
                    }
                    .decodeList<Like>()
                
                result.isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$INSTANCE_ID] Error checking if like exists: ${e.message}", e)
            false
        }
    }
    
    /**
     * Flush pending likes to the database
     */
    suspend fun flush() {
        if (pendingLikes.isEmpty()) {
            Log.d(TAG, "[$INSTANCE_ID] No pending likes to flush")
            return
        }
        
        val actionsToProcess: List<PendingLikeAction>
        synchronized(pendingLikes) {
            actionsToProcess = pendingLikes.toList()
            if (actionsToProcess.isEmpty()) return
        }
        
        Log.d(TAG, "[$INSTANCE_ID] Flushing ${actionsToProcess.size} pending like actions")
        
        val successfulActions = mutableListOf<PendingLikeAction>()
        
        // Process one action at a time to better handle errors
        for (action in actionsToProcess) {
            try {
                // Get numeric user ID from auth ID
                val userId = getUserIdFromAuthId(action.userId)
                
                if (userId == null) {
                    Log.e(TAG, "[$INSTANCE_ID] Could not find user ID for auth_id ${action.userId}")
                    continue
                }
                
                if (action.action == "like") {
                    // Check if like already exists before inserting
                    val exists = checkLikeExists(action.userId, action.postId)
                    
                    if (!exists) {
                        withContext(Dispatchers.IO) {
                            SupabaseClient.client.postgrest.from("likes")
                                .insert(Like(userId = userId, postId = action.postId))
                        }
                        Log.d(TAG, "[$INSTANCE_ID] Successfully inserted like for post ${action.postId}")
                    } else {
                        Log.d(TAG, "[$INSTANCE_ID] Like already exists for post ${action.postId}, skipping insert")
                    }
                    
                    // Mark as successful either way
                    successfulActions.add(action)
                } else {
                    // Delete like
                    withContext(Dispatchers.IO) {
                        SupabaseClient.client.postgrest.from("likes")
                            .delete {
                                filter {
                                    eq("user_id", userId)
                                    eq("post_id", action.postId)
                                }
                            }
                    }
                    Log.d(TAG, "[$INSTANCE_ID] Successfully deleted like for post ${action.postId}")
                    successfulActions.add(action)
                }
            } catch (e: Exception) {
                if (e.message?.contains("duplicate key") == true) {
                    // If it's a duplicate key error for a like action, consider it successful
                    if (action.action == "like") {
                        Log.d(TAG, "[$INSTANCE_ID] Like already exists (caught duplicate key), marking as successful")
                        successfulActions.add(action)
                    } else {
                        Log.e(TAG, "[$INSTANCE_ID] Error processing unlike for post ${action.postId}: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "[$INSTANCE_ID] Error processing ${action.action} for post ${action.postId}: ${e.message}")
                }
            }
        }
        
        // Remove processed actions from the pending queue
        if (successfulActions.isNotEmpty()) {
            synchronized(pendingLikes) {
                pendingLikes.removeAll { action ->
                    successfulActions.any { it === action }
                }
            }
            
            // Update storage
            saveQueue()
        }
        
        Log.d(TAG, "[$INSTANCE_ID] Successfully flushed ${successfulActions.size}/${actionsToProcess.size} like actions")
        
        // If some actions failed, verify our like states with the database
        if (successfulActions.size < actionsToProcess.size) {
            val postIds = actionsToProcess.map { it.postId }.distinct()
            val userId = actionsToProcess.firstOrNull()?.userId
            
            if (userId != null && postIds.isNotEmpty()) {
                Log.d(TAG, "[$INSTANCE_ID] Some actions failed, refreshing like status for ${postIds.size} posts")
                refreshLikeStatus(userId, postIds)
            }
        }
    }
    
    private suspend fun saveQueue() {
        val pendingLikesCopy: List<PendingLikeAction>
        synchronized(pendingLikes) {
            pendingLikesCopy = pendingLikes.toList()
        }
        
        context.dataStore.edit { preferences ->
            preferences[PENDING_LIKES_KEY] = Json.encodeToString(pendingLikesCopy)
        }
        Log.d(TAG, "[$INSTANCE_ID] Saved ${pendingLikesCopy.size} pending likes to persistent storage")
    }
    
    private suspend fun recoverPendingLikes() {
        try {
            val pendingActionsJson = context.dataStore.data.map { preferences ->
                preferences[PENDING_LIKES_KEY] ?: ""
            }.first()
            
            if (pendingActionsJson.isNotEmpty()) {
                val recoveredActions = Json.decodeFromString<List<PendingLikeAction>>(pendingActionsJson)
                
                synchronized(pendingLikes) {
                    pendingLikes.clear()
                    pendingLikes.addAll(recoveredActions)
                }
                
                // Also update internal state
                pendingLikes.groupBy { it.postId }
                    .forEach { (postId, actions) ->
                        // Take the most recent action for each post
                        val mostRecentAction = actions.maxByOrNull { it.timestamp }
                        mostRecentAction?.let {
                            updateLikeState(postId, it.action == "like", true)
                        }
                    }
                
                Log.d(TAG, "[$INSTANCE_ID] Recovered ${recoveredActions.size} pending like actions")
            } else {
                Log.d(TAG, "[$INSTANCE_ID] No pending like actions to recover")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$INSTANCE_ID] Error recovering pending likes: ${e.message}", e)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: LikeManager? = null
        
        /**
         * Get the singleton instance of LikeManager
         */
        fun getInstance(context: Context): LikeManager {
            return INSTANCE ?: synchronized(this) {
                // Create a new instance if it doesn't exist yet
                INSTANCE ?: LikeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
} 