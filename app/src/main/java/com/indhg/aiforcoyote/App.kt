package com.indhg.aiforcoyote

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalePrefs.applyStored(this)
    }
}
