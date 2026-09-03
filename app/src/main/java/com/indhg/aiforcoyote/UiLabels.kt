package com.indhg.aiforcoyote

import android.content.Context

/** 角色名 / 强度档显示名（内部键仍是中文）。 */
object UiLabels {
    fun role(context: Context, name: String): String = when (name) {
        "体验版" -> context.getString(R.string.role_trial)
        "触手" -> context.getString(R.string.role_tentacle)
        "品评会" -> context.getString(R.string.role_appraisal)
        "哥布林" -> context.getString(R.string.role_goblin)
        "史莱姆" -> context.getString(R.string.role_slime)
        "蛛后" -> context.getString(R.string.role_arachne)
        else -> name
    }

    fun intensity(context: Context, level: String): String = when (level) {
        "轻" -> context.getString(R.string.intensity_tender)
        "中" -> context.getString(R.string.intensity_dominant)
        "重" -> context.getString(R.string.intensity_rough)
        else -> level
    }
}
