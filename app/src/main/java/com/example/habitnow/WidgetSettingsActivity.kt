package com.example.habitnow

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

//Variables / UI Elements
class WidgetSettingsActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var textUserName: TextView
    private lateinit var textUserEmail: TextView
    private lateinit var textCurrentTheme: TextView
    private lateinit var editUserName: EditText
    private lateinit var editUserEmail: EditText
    private lateinit var btnEditProfile: Button
    private lateinit var btnSaveChanges: Button
    private lateinit var btnCancelEdit: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var btnChangeTheme: Button
    private lateinit var layoutEditButtons: LinearLayout
    private var isEditMode = false
    private lateinit var originalName: String
    private lateinit var originalEmail: String
    private lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()

        setContentView(R.layout.activity_widget_settings)

        BaseNavigation.wireTopNav(this, findViewById(R.id.top_nav_include))
        setupBottomNavigation()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("HabitNowPrefs", MODE_PRIVATE)

        // Initialize ThemeManager
        themeManager = ThemeManager.getInstance(this)

        // Check authentication
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        initializeViews()
        setupClickListeners()
        loadUserInfo()
        loadCurrentTheme()

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
    }
//initializeViews() – Connect UI Elements
    private fun initializeViews() {
        textUserName = findViewById(R.id.text_user_name)
        textUserEmail = findViewById(R.id.text_user_email)
        editUserName = findViewById(R.id.edit_user_name)
        editUserEmail = findViewById(R.id.edit_user_email)
        btnEditProfile = findViewById(R.id.btn_edit_profile)
        btnSaveChanges = findViewById(R.id.btn_save_changes)
        btnCancelEdit = findViewById(R.id.btn_cancel_edit)
        btnChangePassword = findViewById(R.id.btn_change_password)
        btnLogout = findViewById(R.id.btn_logout)
        layoutEditButtons = findViewById(R.id.layout_edit_buttons)
        textCurrentTheme = findViewById(R.id.text_current_theme)
        btnChangeTheme = findViewById(R.id.btn_change_theme)
    }
//Button Actions
    private fun setupClickListeners() {
        // Widget settings save button
        findViewById<View>(R.id.button_save).setOnClickListener {
            val group = findViewById<RadioGroup>(R.id.group_display_mode)
            val selected = group.checkedRadioButtonId
            val mode = if (selected == R.id.radio_percent) "percent" else "fraction"

            getSharedPreferences(HabitProgressWidgetProvider.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(HabitProgressWidgetProvider.KEY_DISPLAY_MODE, mode)
                .apply()

            HabitProgressWidgetProvider.requestUpdateAll(this@WidgetSettingsActivity)

            val result = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, result)

            Toast.makeText(
                this@WidgetSettingsActivity,
                "Widget settings saved!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Profile editing buttons
        btnEditProfile.setOnClickListener { enterEditMode() }
        btnSaveChanges.setOnClickListener { saveProfileChanges() }
        btnCancelEdit.setOnClickListener { cancelEdit() }
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnLogout.setOnClickListener { showLogoutDialog() }
        btnChangeTheme.setOnClickListener { changeTheme() }
    }

    private fun loadUserInfo() {
        val userName = sharedPreferences.getString("current_user_name", "User") ?: "User"
        val userEmail =
            sharedPreferences.getString("current_user_email", "user@email.com") ?: "user@email.com"
        val isGuest = sharedPreferences.getBoolean("is_guest", false)

        if (isGuest) {
            textUserName.text = "Guest User"
            textUserEmail.text = "Not logged in"
            btnLogout.text = "Login"
            btnEditProfile.visibility = View.GONE
            btnChangePassword.visibility = View.GONE
        } else {
            textUserName.text = userName
            textUserEmail.text = userEmail
            btnLogout.text = "Logout"
            btnEditProfile.visibility = View.VISIBLE
            btnChangePassword.visibility = View.VISIBLE
        }

        // Store original values
        originalName = userName
        originalEmail = userEmail
    }

    private fun loadCurrentTheme() {
        val currentTheme = themeManager.getThemeMode()
        textCurrentTheme.text = themeManager.getThemeName(currentTheme)
    }

    private fun enterEditMode() {
        isEditMode = true

        // Hide display views
        textUserName.visibility = View.GONE
        textUserEmail.visibility = View.GONE
        btnEditProfile.visibility = View.GONE

        // Show edit views
        editUserName.visibility = View.VISIBLE
        editUserEmail.visibility = View.VISIBLE
        layoutEditButtons.visibility = View.VISIBLE

        // Set current values
        editUserName.setText(originalName)
        editUserEmail.setText(originalEmail)

        // Focus on name field
        editUserName.requestFocus()
    }

    private fun saveProfileChanges() {
        val newName = editUserName.text.toString().trim()
        val newEmail = editUserEmail.text.toString().trim()

        // Validate input
        if (TextUtils.isEmpty(newName)) {
            editUserName.error = "Name is required"
            editUserName.requestFocus()
            return
        }

        if (TextUtils.isEmpty(newEmail)) {
            editUserEmail.error = "Email is required"
            editUserEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            editUserEmail.error = "Please enter a valid email address"
            editUserEmail.requestFocus()
            return
        }

        // Check if email is already taken (by another user)
        if (newEmail != originalEmail && sharedPreferences.contains("user_email_$newEmail")) {
            editUserEmail.error = "This email is already registered"
            editUserEmail.requestFocus()
            return
        }

        // Save changes
        val editor = sharedPreferences.edit()

        // If email changed, update all user data keys
        if (newEmail != originalEmail) {
            // Get current password
            val currentPassword =
                sharedPreferences.getString("user_password_$originalEmail", "") ?: ""

            // Remove old entries
            editor.remove("user_email_$originalEmail")
            editor.remove("user_name_$originalEmail")
            editor.remove("user_password_$originalEmail")

            // Add new entries
            editor.putString("user_email_$newEmail", newEmail)
            editor.putString("user_name_$newEmail", newName)
            editor.putString("user_password_$newEmail", currentPassword)
        } else {
            // Just update name
            editor.putString("user_name_$newEmail", newName)
        }

        // Update current session
        editor.putString("current_user_name", newName)
        editor.putString("current_user_email", newEmail)

        editor.apply()

        // Update display
        originalName = newName
        originalEmail = newEmail
        exitEditMode()

        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun cancelEdit() {
        exitEditMode()
    }

    private fun exitEditMode() {
        isEditMode = false

        // Show display views
        textUserName.visibility = View.VISIBLE
        textUserEmail.visibility = View.VISIBLE
        btnEditProfile.visibility = View.VISIBLE

        // Hide edit views
        editUserName.visibility = View.GONE
        editUserEmail.visibility = View.GONE
        layoutEditButtons.visibility = View.GONE

        // Update display with current values
        textUserName.text = originalName
        textUserEmail.text = originalEmail
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null)

        val editCurrentPassword = dialogView.findViewById<EditText>(R.id.edit_current_password)
        val editNewPassword = dialogView.findViewById<EditText>(R.id.edit_new_password)
        val editConfirmNewPassword =
            dialogView.findViewById<EditText>(R.id.edit_confirm_new_password)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.btn_cancel_password)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.btn_update_password).setOnClickListener {
            val currentPassword = editCurrentPassword.text.toString().trim()
            val newPassword = editNewPassword.text.toString().trim()
            val confirmNewPassword = editConfirmNewPassword.text.toString().trim()

            if (validatePasswordChange(
                    currentPassword, newPassword, confirmNewPassword,
                    editCurrentPassword, editNewPassword, editConfirmNewPassword
                )
            ) {
                updatePassword(newPassword)
                dialog.dismiss()
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String,
        editCurrentPassword: EditText,
        editNewPassword: EditText,
        editConfirmNewPassword: EditText
    ): Boolean {
        // Check current password
        if (TextUtils.isEmpty(currentPassword)) {
            editCurrentPassword.error = "Current password is required"
            editCurrentPassword.requestFocus()
            return false
        }

        val storedPassword = sharedPreferences.getString("user_password_$originalEmail", "") ?: ""
        if (currentPassword != storedPassword) {
            editCurrentPassword.error = "Current password is incorrect"
            editCurrentPassword.requestFocus()
            return false
        }

        // Check new password
        if (TextUtils.isEmpty(newPassword)) {
            editNewPassword.error = "New password is required"
            editNewPassword.requestFocus()
            return false
        }

        if (newPassword.length < 6) {
            editNewPassword.error = "Password must be at least 6 characters long"
            editNewPassword.requestFocus()
            return false
        }

        // Check confirm password
        if (TextUtils.isEmpty(confirmNewPassword)) {
            editConfirmNewPassword.error = "Please confirm your new password"
            editConfirmNewPassword.requestFocus()
            return false
        }

        if (newPassword != confirmNewPassword) {
            editConfirmNewPassword.error = "Passwords do not match"
            editConfirmNewPassword.requestFocus()
            return false
        }

        return true
    }

    private fun updatePassword(newPassword: String) {
        val editor = sharedPreferences.edit()
        editor.putString("user_password_$originalEmail", newPassword)
        editor.apply()
    }

    private fun showLogoutDialog() {
        val isGuest = sharedPreferences.getBoolean("is_guest", false)

        if (isGuest) {
            // If guest, navigate to register page
            navigateToRegister()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        // Clear user session
        val editor = sharedPreferences.edit()
        editor.remove("current_user_email")
        editor.remove("current_user_name")
        editor.putBoolean("is_logged_in", false)
        editor.remove("is_guest")
        editor.remove("remembered_email")
        editor.putBoolean("remember_me", false)
        editor.apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        navigateToLogin()
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun navigateToLogin() {
        val intent = Intent(this@WidgetSettingsActivity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister() {
        val intent = Intent(this@WidgetSettingsActivity, RegisterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (isEditMode) {
            cancelEdit()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupBottomNavigation() {
        // Home navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this@WidgetSettingsActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Habits navigation
        findViewById<View>(R.id.nav_habits).setOnClickListener {
            val intent = Intent(this@WidgetSettingsActivity, HabitsActivity::class.java)
            startActivity(intent)
        }

        // Mood Journal navigation
        findViewById<View>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this@WidgetSettingsActivity, MoodJournalActivity::class.java)
            startActivity(intent)
        }

        // Hydration navigation
        findViewById<View>(R.id.nav_hydration).setOnClickListener {
            val intent = Intent(this@WidgetSettingsActivity, HydrationActivity::class.java)
            startActivity(intent)
        }

        // Settings navigation (current page - do nothing)
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            // Already on settings page
        }
    }

    private fun changeTheme() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_theme_selection, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radio_group_theme)
        val radioLight = dialogView.findViewById<RadioButton>(R.id.radio_light_theme)
        val radioDark = dialogView.findViewById<RadioButton>(R.id.radio_dark_theme)
        val radioSystem = dialogView.findViewById<RadioButton>(R.id.radio_system_theme)

        // Set current selection
        val currentTheme = themeManager.getThemeMode()
        when (currentTheme) {
            ThemeManager.THEME_LIGHT -> radioLight.isChecked = true
            ThemeManager.THEME_DARK -> radioDark.isChecked = true
            ThemeManager.THEME_SYSTEM -> radioSystem.isChecked = true
            else -> radioSystem.isChecked = true
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.btn_cancel_theme).setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<View>(R.id.btn_apply_theme).setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            val newTheme = when (selectedId) {
                R.id.radio_light_theme -> ThemeManager.THEME_LIGHT
                R.id.radio_dark_theme -> ThemeManager.THEME_DARK
                R.id.radio_system_theme -> ThemeManager.THEME_SYSTEM
                else -> ThemeManager.THEME_SYSTEM
            }

            themeManager.setThemeMode(newTheme)
            loadCurrentTheme()
            dialog.dismiss()

            Toast.makeText(this, "Theme applied successfully!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }
}