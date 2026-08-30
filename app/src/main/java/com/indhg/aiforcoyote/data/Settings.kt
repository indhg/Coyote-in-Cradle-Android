package com.indhg.aiforcoyote.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-v4-flash-vision-exp",
    val nick: String = "小柳",
    val role: String = "触手",
    val profile: String = "纯爱",
    val autopilot: Boolean = true,
    val jsonMode: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            apiKey = p[KEY_API_KEY] ?: "",
            baseUrl = p[KEY_BASE_URL] ?: "https://api.deepseek.com",
            model = p[KEY_MODEL] ?: "deepseek-v4-flash-vision-exp",
            nick = p[KEY_NICK] ?: "小柳",
            role = p[KEY_ROLE] ?: "触手",
            profile = p[KEY_PROFILE] ?: "纯爱",
            autopilot = p[KEY_AUTOPILOT] ?: true,
            jsonMode = p[KEY_JSON_MODE] ?: true,
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        val next = transform(settings.first())
        context.dataStore.edit { p ->
            p[KEY_API_KEY] = next.apiKey
            p[KEY_BASE_URL] = next.baseUrl
            p[KEY_MODEL] = next.model
            p[KEY_NICK] = next.nick
            p[KEY_ROLE] = next.role
            p[KEY_PROFILE] = next.profile
            p[KEY_AUTOPILOT] = next.autopilot
            p[KEY_JSON_MODE] = next.jsonMode
        }
    }

    private companion object {
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_MODEL = stringPreferencesKey("model")
        val KEY_NICK = stringPreferencesKey("nick")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_PROFILE = stringPreferencesKey("profile")
        val KEY_AUTOPILOT = booleanPreferencesKey("autopilot")
        val KEY_JSON_MODE = booleanPreferencesKey("json_mode")
    }
}
