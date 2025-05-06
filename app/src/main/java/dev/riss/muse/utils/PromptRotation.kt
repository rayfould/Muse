package dev.riss.muse.utils

import android.util.Log
import dev.riss.muse.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class PromptWithDates(
    val id: Int,
    val title: String,
    val description: String? = null,
    val category: String,
    val start_date: String? = null,
    val end_date: String? = null,
    val used: Boolean = false,
    val is_active: Boolean = false
)

/**
 * Utility class for fetching prompts
 */
object PromptRotation {
    private const val TAG = "PromptRotation"
    
    /**
     * Gets the current active prompt for a category
     */
    suspend fun getCurrentPrompt(category: String): PromptWithDates? = withContext(Dispatchers.IO) {
        try {
            val client = SupabaseClient.client
            
            val response = client.postgrest["prompts"]
                .select() {
                    filter { 
                        eq("category", category)
                        eq("is_active", true)
                    }
                }
            
            val prompts = response.decodeList<PromptWithDates>()
            if (prompts.isNotEmpty()) {
                return@withContext prompts.first()
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current prompt: ${e.message}", e)
            return@withContext null
        }
    }
}