package com.indhg.aiforcoyote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** 更新信息：latest=最新 tag（如 v1.1.6），available=当前版本落后。 */
data class UpdateInfo(
    val latest: String = "",
    val url: String = "",
    val available: Boolean = false,
)

/** 静默查 GitHub Releases 最新版本；失败返回空结果，绝不抛异常打扰用户。 */
object UpdateChecker {

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private fun parseVersion(s: String): Triple<Int, Int, Int>? {
        val m = Regex("""[vV]?(\d+)\.(\d+)\.(\d+)""").find(s.trim()) ?: return null
        return Triple(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
    }

    private fun newerThan(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Boolean {
        if (a.first != b.first) return a.first > b.first
        if (a.second != b.second) return a.second > b.second
        return a.third > b.third
    }

    suspend fun check(current: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val resp = http.newCall(
                Request.Builder()
                    .url("https://api.github.com/repos/indhg/AI-for-Coyote/releases/latest")
                    .header("User-Agent", "CoyoteInCradle-UpdateCheck")
                    .build()
            ).execute()
            if (!resp.isSuccessful) return@withContext UpdateInfo()
            val root = json.parseToJsonElement(resp.body!!.string()).jsonObject
            val tag = root["tag_name"]?.jsonPrimitive?.content ?: return@withContext UpdateInfo()
            if (!tag.startsWith("v")) return@withContext UpdateInfo()
            val url = root["html_url"]?.jsonPrimitive?.content ?: ""
            val cur = parseVersion(current) ?: return@withContext UpdateInfo()
            val latest = parseVersion(tag) ?: return@withContext UpdateInfo()
            UpdateInfo(tag, url, newerThan(latest, cur))
        } catch (_: Exception) {
            UpdateInfo()
        }
    }
}
