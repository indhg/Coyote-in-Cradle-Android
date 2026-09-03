package com.indhg.aiforcoyote

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * 界面语言持久化（SharedPreferences，可在 attachBaseContext / Application 同步读取）。
 * 取值：system | zh | en。system = 跟随系统，不调用固定 LocaleList。
 */
object LocalePrefs {
    const val SYSTEM = "system"
    const val ZH = "zh"
    const val EN = "en"

    private const val PREF = "locale_prefs"
    private const val KEY = "ui_lang"

    fun get(context: Context): String {
        val v = context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, SYSTEM) ?: SYSTEM
        return if (v == ZH || v == EN) v else SYSTEM
    }

    fun set(context: Context, tag: String) {
        val v = if (tag == ZH || tag == EN) tag else SYSTEM
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, v).apply()
    }

    /** 解析后的界面语言：zh 或 en。system 时看系统 language。 */
    fun resolved(context: Context): String {
        val stored = get(context)
        if (stored == ZH || stored == EN) return stored
        val lang = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (lang.startsWith("zh")) ZH else EN
    }

    fun apply(tag: String) {
        val list = when (tag) {
            ZH -> LocaleListCompat.forLanguageTags("zh")
            EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(list)
    }

    fun applyStored(context: Context) = apply(get(context))
}
