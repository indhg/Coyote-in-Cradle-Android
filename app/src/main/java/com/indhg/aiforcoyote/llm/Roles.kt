package com.indhg.aiforcoyote.llm

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * 主题体系（对齐桌面版 roles 结构）：
 * 每个主题（角色）自带若干风格档（轻/中/重 → 显示名 纯爱/调教/凌辱）。
 * 提示词来源两种：assets（本体，永远可用）/ DLC 文件（filesDir/dlc/，导入后可用）。
 */
object Roles {

    const val DLC_DIR = "dlc"

    data class Profile(
        val name: String,          // 纯爱 / 调教
        val level: String,         // 轻 / 中 / 重
        val asset: String? = null, // assets 内路径（本体）
        val dlcRel: String? = null, // filesDir 内相对路径（DLC）
        val note: String = "",     // 风格说明（注入提示词）
    ) {
        fun available(context: Context): Boolean =
            asset != null || (dlcRel != null && File(context.filesDir, dlcRel).exists())

        fun load(context: Context): String = when {
            asset != null ->
                context.assets.open(asset).bufferedReader().use { it.readText() }
            dlcRel != null -> {
                val f = File(context.filesDir, dlcRel)
                if (f.exists()) f.readText()
                else throw IOException("该风格未安装：请在设置页导入对应 DLC 包")
            }
            else -> throw IOException("该风格没有提示词来源")
        }
    }

    data class Role(
        val name: String,          // 触手 / 品评会
        val title: String,         // 玩家对它的称呼
        val narrative: String,     // 设备叙事：触手=本体即设备 / 装置=遥控支配
        val profiles: List<Profile>,
    ) {
        fun usable(context: Context): Boolean = profiles.any { it.available(context) }
    }

    val ALL: List<Role> = listOf(
        Role(
            "触手", "主人", "触手",
            listOf(
                Profile("纯爱", "轻", asset = "prompts/触手-角色提示词-纯爱.md"),
                Profile("调教", "中", dlcRel = "$DLC_DIR/触手-角色提示词-调教.md"),
            ),
        ),
        Role(
            "品评会", "主人", "装置",
            listOf(
                Profile(
                    "调教", "中", dlcRel = "$DLC_DIR/品评会-角色提示词-调教.md",
                    note = "调教版（品评会）：公开审评、装置支配、围观施压；以羞辱与驯化为核心，从哭腔抗拒走向条件反射式服从。",
                ),
            ),
        ),
    )

    fun find(name: String): Role? = ALL.firstOrNull { it.name == name }

    /** 档位显示名（用户定名）：轻→纯爱、中→调教、重→凌辱 */
    val LEVEL_LABELS = mapOf("轻" to "纯爱", "中" to "调教", "重" to "凌辱")

    val LEVELS = listOf("轻", "中", "重")

    /** 旧版兼容：调教版提示词相对路径（DLC1 导入检测用）。 */
    const val DLC1_PROMPT_REL = "$DLC_DIR/触手-角色提示词-调教.md"
}
