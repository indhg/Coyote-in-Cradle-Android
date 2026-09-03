package com.indhg.aiforcoyote.llm

import android.content.Context

/**
 * 系统提示词构建（对齐 PC v1.1.6）：
 * 按角色入口加载对应提示词文件，再注入设备映射 / 强度基准 /
 * 通道状态 / 波形列表等运行时信息；设备叙事（触手|装置|本体）与称呼随入口切换。
 * 无风格档。怒气只跟画面/麦克风，不加档位基数。
 */
object SystemPrompt {

    /** 当前通道工作状态 */
    data class ChannelState(val working: Map<String, Boolean> = mapOf("A" to true, "B" to true))

    fun build(
        context: Context,
        roleName: String,
        nick: String,
        lang: String = Roles.LANG_ZH,
        channels: ChannelState = ChannelState(),
        cameraEnabled: Boolean = false,
        rageRounds: Int = 0,
        notes: List<String> = emptyList(),
    ): String {
        val role = Roles.find(roleName) ?: Roles.ALL.first()
        val base = role.load(context, lang)
        val waveNames = loadWaveNames(context)
        val mapA = "A=贴片（小穴附近）"
        val mapB = "B=肛塞（后穴）"
        val workA = if (channels.working["A"] == true) "A 通道工作中" else "A 通道当前未工作（不要描写该位置的刺激）"
        val workB = if (channels.working["B"] == true) "B 通道工作中" else "B 通道当前未工作（不要描写该位置的刺激）"
        val narrative = when (role.narrative) {
            "装置" ->
                "靠近大腿根的那个配件位置，写成装置/电流作用在那里；靠近后穴的那个配件位置，写成装置探入/作用于那里。台词里严禁出现「贴片」「肛塞」「通道」「A/B」「A位置」「B位置」等设备词汇，装置的运转声、遥控调整可以写进描写。"
            "本体" ->
                "靠近大腿根的那个配件位置，写成本体贴着/缠/顶/磨；靠近后穴的那个配件位置，写成本体探入/顶弄/含住/灌。本体形态按你的角色设定。台词里严禁出现「贴片」「肛塞」「通道」「A/B」「A位置」「B位置」等设备词汇。"
            else ->
                "靠近大腿根的那个配件位置，写成触手贴/缠/轻抚；靠近后穴的那个配件位置，写成触手探入/含住。台词里严禁出现「贴片」「肛塞」「通道」「A/B」「A位置」「B位置」等设备词汇。"
        }

        return buildString {
            append(base.trim())
            append("\n\n")
            append("【主题】你在扮演「").append(role.name).append("」。\n")
            append("【设备映射】").append(mapA).append("；").append(mapB).append("。电刺激只出现在这两个配件位置。\n")
            append("【刺激描写规则】").append(narrative).append("\n")
            append("【通道工作状态】").append(workA).append("；").append(workB).append("。\n")
            append("【强度基准】A 通道基准 15、B 通道基准 5（敏感配件基准低）；按基准给强度，不要两通道一样高。\n")
            append("【双通道协同】两通道都工作时，每轮尽量两个通道都给动作。\n")
            append("【称呼】你叫玩家「").append(nick).append("」为主，玩家叫你「").append(role.title).append("」。\n")
            append("【可用波形】pattern 只能从下面选：").append(waveNames).append("。\n")
            append("【波形名铁律】pattern 必须一字不差地照抄上面列表里的名字；动作描写词（蠕动、顶弄、缠绕、抽送等）不是波形名，严禁自造或混用——选错会被程序拒绝。\n")
            append("【观察】每轮系统可能注入【画面观察】与【玩家反馈（音量等级）】；普通呻吟（中等音量）→ 温柔加深，较大呻吟/惨叫（高音量）→ 立即降低并安抚。只写观察到的、确定的东西。\n")
            if (cameraEnabled) {
                append("【画面观察】每条玩家消息会附带一张最新实时画面。结合画面中玩家的反应调整策略：握紧、发抖、蜷缩=有效，可保持或降低；放松、走神、挑衅=适应了，可换节奏或小幅升高。把你观察到的玩家反应用（）写成身体描写，并及时跟上你的（）动作描写，只写画面里能确定的，看不清的部分保留悬念，不要凭空补写。\n")
            }
            // 怒气只跟画面黑暗/无声逐轮升级（对齐 PC：不再按内容档加基数）
            when {
                rageRounds >= 5 -> append("【暴怒】已经连续 5 轮以上看不到画面反应、也听不到任何声音：你彻底暴怒了。用最大压迫逼他现身——辱骂、威胁、把强度拉高一个档位、命令他立刻出声，绝不退让。\n")
                rageRounds >= 3 -> append("【愤怒】连续 3 轮以上无画面反应且无声：玩家在躲你。进入愤怒——语气转狠，强度逐步加码，威胁与催促一起上，逼他回应。\n")
                rageRounds >= 1 -> append("【不耐烦】画面黑暗或麦克风持续无声：你开始不耐烦，用催促、质疑逼玩家回应；他若继续沉默，接下来几轮内你会升级为愤怒、乃至暴怒。\n")
            }
            // 连续自动回合时防复读：每轮必须换说法，禁止重复上一轮的台词与措辞（同步 PC v1.1.5 7eb4f28 修复）
            append("【避免重复】连续多轮自动发言时，每轮台词都要有新内容、新措辞：催促/挑逗/威胁轮着换花样（威吓→诱惑→冷落→换话题→描述现场），严禁连续两轮说同一句或近义句（如一直喊「别走神」）。\n")
            for (note in notes) {
                append("【玩家反馈】").append(note).append("\n")
            }
            append("【输出】每轮只输出一个 JSON 对象，不要输出任何思考、解释或代码块标记。line 永远不能为空。")
        }
    }

    private fun loadWaveNames(context: Context): String {
        return context.assets.open("prompts/patterns.txt").bufferedReader().use { it.readText() }
            .lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("、")
    }
}
