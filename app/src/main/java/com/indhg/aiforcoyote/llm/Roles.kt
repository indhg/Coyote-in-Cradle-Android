package com.indhg.aiforcoyote.llm

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * 角色入口（对齐 PC v1.1.6）：
 * - 体验版：内置 assets，永远可用。
 * - 5 个正式角色：运行时检测 filesDir/content/roles/<角色>-角色提示词.md（或 -EN.md）。
 *   存在=可用，不存在=未导入，禁止选中生成。
 * 不再有「纯爱/调教/凌辱」多 Profile。电击强度三档与内容无关。
 */
object Roles {

    /** 相对 filesDir。与 PC DLC zip 内 `content/roles/` 同构，解压后按文件名落入此目录。 */
    const val CONTENT_ROLES_DIR = "content/roles"

    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    val INTENSITY_LEVELS = listOf("轻", "中", "重")

    data class Entry(
        val key: String,
        val name: String,
        val title: String,
        val narrative: String, // 触手 / 装置 / 本体
        val trial: Boolean = false,
        val recommended: Boolean = false,
        val fileName: String? = null, // 正式稿 CN 文件名，如 触手-角色提示词.md
    ) {
        fun promptFileName(lang: String): String? {
            val base = fileName ?: return null
            return if (lang == LANG_EN) base.removeSuffix(".md") + "-EN.md" else base
        }

        fun available(context: Context, lang: String = LANG_ZH): Boolean {
            if (trial) return true
            val fn = promptFileName(lang) ?: return false
            return File(context.filesDir, "$CONTENT_ROLES_DIR/$fn").isFile
        }

        /**
         * @param scriptLang 角色稿语言 zh/en
         * @param uiLang 界面语言 zh/en；体验版仅在两者都是 en 时读英文纯爱稿
         */
        fun load(context: Context, scriptLang: String = LANG_ZH, uiLang: String = LANG_ZH): String {
            if (trial) {
                val asset = trialAsset(scriptLang, uiLang)
                return context.assets.open(asset).bufferedReader().use { it.readText() }
            }
            val fn = promptFileName(scriptLang)
                ?: throw IOException(context.getString(com.indhg.aiforcoyote.R.string.err_no_prompt))
            val f = File(context.filesDir, "$CONTENT_ROLES_DIR/$fn")
            if (!f.isFile) {
                throw IOException(context.getString(com.indhg.aiforcoyote.R.string.err_role_load, name))
            }
            return f.readText()
        }
    }

    val ALL: List<Entry> = listOf(
        Entry(
            key = "trial",
            name = "体验版",
            title = "主人",
            narrative = "触手",
            trial = true,
        ),
        Entry(
            key = "cushou",
            name = "触手",
            title = "主人",
            narrative = "触手",
            recommended = true,
            fileName = "触手-角色提示词.md",
        ),
        Entry(
            key = "appraisal",
            name = "品评会",
            title = "主人",
            narrative = "装置",
            recommended = true,
            fileName = "品评会-角色提示词.md",
        ),
        Entry(
            key = "goblin",
            name = "哥布林",
            title = "主人",
            narrative = "本体",
            fileName = "哥布林-角色提示词.md",
        ),
        Entry(
            key = "slime",
            name = "史莱姆",
            title = "主人",
            narrative = "本体",
            fileName = "史莱姆-角色提示词.md",
        ),
        Entry(
            key = "zhuhou",
            name = "蛛后",
            title = "主人",
            narrative = "本体",
            fileName = "蛛后-角色提示词.md",
        ),
    )

    /** DLC zip / 单 md 允许落入 content/roles 的文件名（CN + EN）。旧 -调教/-凌辱 不在此列。 */
    val KNOWN_DLC_FILES: Set<String> = ALL.mapNotNull { it.fileName }.flatMap { cn ->
        listOf(cn, cn.removeSuffix(".md") + "-EN.md")
    }.toSet()

    const val TRIAL_ASSET_ZH = "prompts/触手-角色提示词-纯爱.md"
    const val TRIAL_ASSET_EN = "prompts/触手-角色提示词-纯爱-EN.md"

    /** 体验版 EN 稿仅在界面语言与角色稿语言都是 en 时启用。 */
    fun trialAsset(scriptLang: String, uiLang: String): String =
        if (uiLang == LANG_EN && scriptLang == LANG_EN) TRIAL_ASSET_EN else TRIAL_ASSET_ZH

    fun find(name: String): Entry? = ALL.firstOrNull { it.name == name }

    fun contentRolesDir(context: Context): File = File(context.filesDir, CONTENT_ROLES_DIR)

    /**
     * 旧档位 → 新入口。触手+纯爱 = 体验版；其余已知角色名保留；未知回体验版。
     */
    fun migrateRole(role: String, profile: String): String {
        if (role == "触手" && profile == "纯爱") return "体验版"
        if (find(role) != null) return role
        return "体验版"
    }
}
