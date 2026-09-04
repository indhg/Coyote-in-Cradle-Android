package com.indhg.aiforcoyote.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.indhg.aiforcoyote.llm.Roles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = "deepseek-v4-flash-vision-exp",
    val nick: String = "小柳",
    val role: String = "体验版",
    val intensityLevel: String = "中",
    val contentLang: String = Roles.LANG_ZH,
    val uiLang: String = "system",
    val scriptFollowUi: Boolean = true,
    val autopilot: Boolean = true,
    val jsonMode: Boolean = true,
    val checkUpdate: Boolean = true,
    val trialBadgeSeen: Boolean = false,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        val rawRole = p[KEY_ROLE] ?: "体验版"
        val rawProfile = p[KEY_PROFILE] ?: "纯爱"
        Settings(
            apiKey = p[KEY_API_KEY] ?: "",
            baseUrl = p[KEY_BASE_URL] ?: "https://api.deepseek.com",
            model = p[KEY_MODEL] ?: "deepseek-v4-flash-vision-exp",
            nick = p[KEY_NICK] ?: "小柳",
            role = Roles.migrateRole(rawRole, rawProfile),
            intensityLevel = p[KEY_INTENSITY]?.takeIf { it in Roles.INTENSITY_LEVELS } ?: "中",
            uiLang = p[KEY_UI_LANG]?.takeIf { it == "zh" || it == "en" || it == "system" } ?: "system",
            scriptFollowUi = p[KEY_SCRIPT_FOLLOW] ?: true,
            contentLang = run {
                val stored = p[KEY_CONTENT_LANG]?.takeIf { it == Roles.LANG_EN || it == Roles.LANG_ZH }
                val follow = p[KEY_SCRIPT_FOLLOW] ?: true
                if (follow || stored == null) {
                    val ui = p[KEY_UI_LANG]?.takeIf { it == "zh" || it == "en" || it == "system" } ?: "system"
                    if (ui == "en") Roles.LANG_EN else if (ui == "zh") Roles.LANG_ZH else
                        if (com.indhg.aiforcoyote.LocalePrefs.systemLanguage().startsWith("zh")) Roles.LANG_ZH else Roles.LANG_EN
                } else stored
            },
            autopilot = p[KEY_AUTOPILOT] ?: true,
            jsonMode = p[KEY_JSON_MODE] ?: true,
            checkUpdate = p[KEY_CHECK_UPDATE] ?: true,
            trialBadgeSeen = p[KEY_TRIAL_SEEN] ?: false,
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
            p[KEY_PROFILE] = "正式"
            p[KEY_INTENSITY] = next.intensityLevel
            p[KEY_CONTENT_LANG] = next.contentLang
            p[KEY_UI_LANG] = next.uiLang
            p[KEY_SCRIPT_FOLLOW] = next.scriptFollowUi
            p[KEY_AUTOPILOT] = next.autopilot
            p[KEY_JSON_MODE] = next.jsonMode
            p[KEY_CHECK_UPDATE] = next.checkUpdate
            p[KEY_TRIAL_SEEN] = next.trialBadgeSeen
        }
    }

    private companion object {
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_MODEL = stringPreferencesKey("model")
        val KEY_NICK = stringPreferencesKey("nick")
        val KEY_ROLE = stringPreferencesKey("role")
        val KEY_PROFILE = stringPreferencesKey("profile") // 旧档位，只用于迁移，写入固定「正式」
        val KEY_INTENSITY = stringPreferencesKey("intensity_level")
        val KEY_CONTENT_LANG = stringPreferencesKey("content_lang")
        val KEY_UI_LANG = stringPreferencesKey("ui_lang")
        val KEY_SCRIPT_FOLLOW = booleanPreferencesKey("script_follow_ui")
        val KEY_AUTOPILOT = booleanPreferencesKey("autopilot")
        val KEY_JSON_MODE = booleanPreferencesKey("json_mode")
        val KEY_CHECK_UPDATE = booleanPreferencesKey("check_update")
        val KEY_TRIAL_SEEN = booleanPreferencesKey("trial_badge_seen")
    }
}
