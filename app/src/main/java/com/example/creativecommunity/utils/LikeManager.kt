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
import com.example.creativecommunity.models.PendingLikeAction
import com.example.creativecommunity.models.PostLike
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

// Extension property for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "like_preferences")

/**
 * Manages like operations with batching and local storage
 */
class LikeManager(private val context: Context) {
    private val TAG = "LikeManager"
    private val PENDING_LIKES_KEY = stringPreferencesKey("pending_likes")
    private val FLUSH_INTERVAL_MS = 10000L // 10 seconds
    
    // In-memory queue for like operations
    private val pendingLikes = ConcurrentHashMap<String, PostLike>()
    
    // For real-time updates
    private val _likeUpdates = MutableSharedFlow<PostLike>(replay = 0)
    val likeUpdates: SharedFlow<PostLike> = _likeUpdates
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Map to store which posts are liked by the current user
    private val userLikedPosts = ConcurrentHashMap<String, Boolean>()
    
    init {
        // Start a periodic flush job
        coroutineScope.launch {
            // First, recover any pending likes from storage in case the app was killed
            recoverPendingLikes()
            
            // Then start the periodic flush
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                flush()
            }
        }
        
        // Setup realtime listener for likes
        setupRealtimeListener()
    }
    
    private fun setupRealtimeListener() {
        coroutineScope.launch {
            try {
                val channel = SupabaseClient.client.realtime.channel("public:likes")
                
                channel.postgresChangeFlow<PostgresAction.Insert>("public:likes") {
                    // Filter only for inserts
                }.collect { change ->
                    val newLike = change.record as? Map<String, Any> ?: return@collect
                    val postIdRaw = newLike["post_id"] ?: return@collect
                    val userId = newLike["user_id"] as? String ?: return@collect
                    
                    // Convert postId to Int from any possible type (String, Number, etc.)
                    val postId = when (postIdRaw) {
                        is Number -> postIdRaw.toInt()
                        is String -> postIdRaw.toIntOrNull() ?: return@collect
                        else -> return@collect
                    }
                    
                    // Only emit if it's not our own like
                    if (!pendingLikes.containsKey("$userId:$postId")) {
                        _likeUpdates.emit(PostLike(postId, true))
                    }
                }
                
                channel.postgresChangeFlow<PostgresAction.Delete>("public:likes") {
                    // Filter only for deletes
                }.collect { change ->
                    val oldLike = change.oldRecord as? Map<String, Any> ?: return@collect
                    val postIdRaw = oldLike["post_id"] ?: return@collect
                    val userId = oldLike["user_id"] as? String ?: return@collect
                    
                    // Convert postId to Int from any possible type (String, Number, etc.)
                    val postId = when (postIdRaw) {
                        is Number -> postIdRaw.toInt()
                        is String -> postIdRaw.toIntOrNull() ?: return@collect
                        else -> return@collect
                    }
                    
                    // Only emit if it's not our own unlike
                    if (!pendingLikes.containsKey("$userId:$postId")) {
                        _likeUpdates.emit(PostLike(postId, false))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up realtime listener: ${e.message}", e)
            }
        }
    }
    
    /**
     * Queue a like action for batched processing
     */
    fun queueLike(userId: String, postId: Int, isLiked: Boolean) {
        val key = "$userId:$postId"
        val postLike = PostLike(postId, isLiked)
        
        // Store in memory
        pendingLikes[key] = postLike
        
        // Update local state immediately for optimistic UI
        userLikedPosts[postId.toString()] = isLiked
        
        // Store in persistent storage in case app is killed
        coroutineScope.launch {
            storePendingLikes()
        }
        
        // Emit the event for UI updates
        coroutineScope.launch {
            _likeUpdates.emit(postLike)
        }
    }
    
    /**
     * Get whether the post is liked by the current user
     */
    suspend fun isPostLikedByUser(userId: String, postId: Int): Boolean {
        // Check local cache first
        userLikedPosts[postId.toString()]?.let { return it }
        
        return try {
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
            userLikedPosts[postId.toString()] = isLiked
            isLiked
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if post is liked: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get the like count for a post
     */
    suspend fun getLikeCount(postId: Int): Int {
        return try {
            val result = withContext(Dispatchers.IO) {
                SupabaseClient.client.postgrest.from("likes")
                    .select(Columns.raw("count(*)")) {
                        filter {
                            eq("post_id", postId)
                        }
                    }
                    .decodeSingle<LikeCount>()
            }
            result.count
        } catch (e: Exception) {
            Log.e(TAG, "Error getting like count: ${e.message}", e)
            0
        }
    }
    
    /**
     * Flush pending likes to the database
     */
    suspend fun flush() {
        if (pendingLikes.isEmpty()) return
        
        // Group by action (like or unlike)
        val likesToAdd = mutableListOf<Like>()
        val likesToRemove = mutableListOf<Pair<String, Int>>() // user_id, post_id pairs
        
        pendingLikes.forEach { (key, postLike) ->
            val parts = key.split(":")
            val userId = parts[0]
            val postId = parts[1].toInt()
            
            if (postLike.isLiked) {
                likesToAdd.add(Like(userId = userId, postId = postId))
            } else {
                likesToRemove.add(userId to postId)
            }
        }
        
        try {
            // Process inserts
            if (likesToAdd.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.postgrest.from("likes")
                        .insert(likesToAdd)
                }
            }
            
            // Process deletes
            likesToRemove.forEach { (userId, postId) ->
                withContext(Dispatchers.IO) {
                    SupabaseClient.client.postgrest.from("likes")
                        .delete {
                            filter {
                                eq("user_id", userId)
                                eq("post_id", postId)
                            }
                        }
                }
            }
            
            // Clear the pending queue after successful flush
            pendingLikes.clear()
            // Clear from persistent storage too
            context.dataStore.edit { preferences ->
                preferences.remove(PENDING_LIKES_KEY)
            }
            
            Log.d(TAG, "Successfully flushed ${likesToAdd.size} likes and ${likesToRemove.size} unlikes")
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing likes: ${e.message}", e)
            // Keep items in the queue to retry later
        }
    }
    
    private suspend fun storePendingLikes() {
        val pendingActions = pendingLikes.map { (key, postLike) ->
            val parts = key.split(":")
            val userId = parts[0]
            val postId = parts[1].toInt()
            
            PendingLikeAction(
                postId = postId,
                userId = userId,
                action = if (postLike.isLiked) "like" else "unlike",
                timestamp = postLike.timestamp
            )
        }
        
        context.dataStore.edit { preferences ->
            preferences[PENDING_LIKES_KEY] = Json.encodeToString(pendingActions)
        }
    }
    
    private suspend fun recoverPendingLikes() {
        try {
            val pendingActionsJson = context.dataStore.data.map { preferences ->
                preferences[PENDING_LIKES_KEY] ?: ""
            }.first()
            
            if (pendingActionsJson.isNotEmpty()) {
                val pendingActions = Json.decodeFromString<List<PendingLikeAction>>(pendingActionsJson)
                
                pendingActions.forEach { action ->
                    val key = "${action.userId}:${action.postId}"
                    pendingLikes[key] = PostLike(
                        postId = action.postId,
                        isLiked = action.action == "like",
                        timestamp = action.timestamp
                    )
                    
                    // Also update local state
                    userLikedPosts[action.postId.toString()] = action.action == "like"
                }
                
                Log.d(TAG, "Recovered ${pendingActions.size} pending like actions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recovering pending likes: ${e.message}", e)
        }
    }
} 