package config

import utils.Env

object SupabaseConfig {
    val SUPABASE_URL = Env.SUPABASE_URL
    val SUPABASE_KEY = Env.SUPABASE_KEY
    val STORAGE_BUCKET = Env.STORAGE_BUCKET
}
