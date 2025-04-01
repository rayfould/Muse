package com.example.creativecommunity

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://qbucdahljpngblnafgzl.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFidWNkYWhsanBuZ2JsbmFmZ3psIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM0NzAzNjgsImV4cCI6MjA1OTA0NjM2OH0.qsPYT5p6xUsGEroORItdlW311MFvXAb-lu5qhcxnk3s"
    ) {
        install(Auth)
        install(Postgrest)
    }
}