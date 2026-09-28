package com.kidslanguage.companion

import android.app.Application
import com.kidslanguage.companion.app.AppContainer

class KidsLanguageApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
