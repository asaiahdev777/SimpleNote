package com.ajt.simplenote

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ApplicationSingleton : Application() {

    companion object {
        lateinit var app: Application
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        app = this
    }

}