package com.example.habitnow

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class AuthenticationManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "HabitNowAuth"
        private const val IS_LOGGED_IN = "is_logged_in"
        private const val CURRENT_USER_EMAIL = "current_user_email"
        private const val CURRENT_USER_NAME = "current_user_name"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(IS_LOGGED_IN, false)
    }

    fun getCurrentUserEmail(): String {
        return sharedPreferences.getString(CURRENT_USER_EMAIL, "") ?: ""
    }

    fun getCurrentUserName(): String {
        return sharedPreferences.getString(CURRENT_USER_NAME, "User") ?: "User"
    }

    fun logout() {
        sharedPreferences.edit().apply {
            putBoolean(IS_LOGGED_IN, false)
            remove(CURRENT_USER_EMAIL)
            remove(CURRENT_USER_NAME)
            apply()
        }
    }

    fun navigateToLogin() {
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    fun checkAuthenticationAndRedirect() {
        if (!isUserLoggedIn()) {
            navigateToLogin()
        }
    }
}