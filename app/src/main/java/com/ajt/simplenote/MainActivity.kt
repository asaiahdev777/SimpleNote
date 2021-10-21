package com.ajt.simplenote

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) R.style.NightTheme else R.style.DayTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        toolbar.overflowIcon?.setTint(getThemeColor(R.attr.titleTextColor))

        with(supportFragmentManager) {
            if (findFragmentById(R.id.fragmentArea) == null)
                beginTransaction().add(R.id.fragmentArea, NoteFragment()).commitNow()
        }

    }

    fun switchNote() = supportFragmentManager.beginTransaction().replace(R.id.fragmentArea, NoteFragment()).commitNow()

}