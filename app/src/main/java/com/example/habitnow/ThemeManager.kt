package com.example.habitnow

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class ThemeManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode"

        const val THEME_LIGHT = 0
        const val THEME_DARK = 1
        const val THEME_SYSTEM = 2

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setThemeMode(themeMode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply()
        applyTheme(themeMode)
    }

    fun getThemeMode(): Int {
        return prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
    }

    fun applyTheme(themeMode: Int) {
        when (themeMode) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun applyStoredTheme() {
        applyTheme(getThemeMode())
    }

    fun getThemeName(themeMode: Int): String {
        return when (themeMode) {
            THEME_LIGHT -> "Light"
            THEME_DARK -> "Dark"
            THEME_SYSTEM -> "System Default"
            else -> "System Default"
        }
    }

    fun isDarkMode(context: Context): Boolean {
        val currentMode = getThemeMode()
        return if (currentMode == THEME_SYSTEM) {
            val nightModeFlags = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            currentMode == THEME_DARK
        }
    }
}