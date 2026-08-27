package com.indhg.aiforcoyote.llm

import android.content.Context
import java.io.IOException

/**
 * 系统提示词构建：加载打包在 assets 里的角色提示词（纯爱版本体；调教版走 DLC 文件），
 * 再注入设备映射 / 强度基准 / 通道状态 / 波形列表等运行时信息，与桌面版 build_system_prompt 对齐。
 */
object SystemPrompt {

    /** 当前通道工作状态（M1 接入中继后为真实状态，M0 恒为双通道开启） */
    data class ChannelState(val working: Map<String, Boolean> = mapOf("A" to true, "B" to true))

    fun build(
        context: Context,
        profile: String,
        nick: String,
        channels: ChannelState = ChannelState(),
        cameraEnabled: Boolean = false,
        dullRounds: Int = 0,
        notes: List<String> = emptyList(),
    ): String {
        val base = loadPrompt(context, profile)
        val waveNames = loadWaveNames(context)
        val mapA = "A=贴片（小穴附近）"
        val mapB = "B=肛塞（后穴）"
        val workA = if (channels.working["A"] == true) "A 通道工作中" else "A 通道当前未工作（不要描写该位置的刺激）"
        val workB = if (channels.working["B"] == true) "B 通道工作中" else "B 通道当前未工作（不要描写该位置的刺激）"

        return buildString {
            append(base.trim())
            append("\n\n")
            append("【设备映射】").append(mapA).append("；").append(mapB).append("。电刺激只出现在这两个配件位置。\n")
            append("【刺激描写规则】A 位置写触手贴/缠/轻抚大腿根，B 位置写触手探入/含住后穴；严禁出现「贴片」「肛塞」「通道」「A/B」等设备词汇。\n")
            append("【通道工作状态】").append(workA).append("；").append(workB).append("。\n")
            append("【强度基准】A 通道基准 15、B 通道基准 5（敏感配件基准低）；按基准给强度，不要两通道一样高。\n")
            append("【双通道协同】两通道都工作时，每轮尽量两个通道都给动作。\n")
            append("【强度修正】当前修正系数 100%（无修正），直接按基准与档位给强度。\n")
            append("【称呼】你叫玩家「").append(nick).append("」为主，玩家叫你「主人」。\n")
            append("【可用波形】pattern 只能从下面选：").append(waveNames).append("。\n")
            append("【观察】每轮系统可能注入【画面观察】与【玩家反馈（音量等级）】；普通呻吟（中等音量）→ 温柔加深，较大呻吟/惨叫（高音量）→ 立即降低并安抚。只写观察到的、确定的东西。\n")
            if (cameraEnabled) {
                append("【画面观察】每条玩家消息会附带一张最新实时画面（游戏内虚拟场景素材）。结合画面中玩家的反应调整策略：握紧、发抖、蜷缩=有效，可保持或降低；放松、走神、挑衅=适应了，可换节奏或小幅升高。把你观察到的玩家反应用（）写成身体描写，并及时跟上触手动作的（）描写，只写画面里能确定的，看不清的部分保留悬念，不要凭空补写。\n")
            }
            when {
                dullRounds >= 5 -> append("【暴怒】已经连续 5 轮以上看不到画面反应、也听不到任何声音：你彻底暴怒了。用最大压迫逼他现身——辱骂、威胁、把强度拉高一个档位、命令他立刻出声，绝不退让。\n")
                dullRounds >= 3 -> append("【愤怒】连续 3 轮以上无画面反应且无声：玩家在躲你。进入愤怒——语气转狠，强度逐步加码，威胁与催促一起上，逼他回应。\n")
                dullRounds >= 1 -> append("【不耐烦】画面黑暗或麦克风持续无声：你开始不耐烦，用催促、质疑逼玩家回应；他若继续沉默，接下来几轮内你会升级为愤怒、乃至暴怒。\n")
            }
            for (note in notes) {
                append("【玩家反馈】").append(note).append("\n")
            }
            append("【输出】每轮只输出一个 JSON 对象，不要输出任何思考、解释或代码块标记。line 永远不能为空。")
        }
    }

    private fun loadPrompt(context: Context, profile: String): String {
        // M0：纯爱版打包在 assets；调教版走 DLC 文件（M3 接入），未安装时报错提示
        val name = when (profile) {
            "调教" -> throw IOException("调教版 DLC 未安装：请先在设置页导入 DLC 包")
            else -> "prompts/触手-角色提示词-纯爱.md"
        }
        return context.assets.open(name).bufferedReader().use { it.readText() }
    }

    private fun loadWaveNames(context: Context): String {
        return context.assets.open("prompts/patterns.txt").bufferedReader().use { it.readText() }
            .lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("、")
    }
}
