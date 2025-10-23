package com.example.habitnow

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
// import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var editLoginEmail: EditText
    private lateinit var editLoginPassword: EditText
    private lateinit var checkboxRemember: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var textRegisterLink: TextView
    private lateinit var textGuestMode: TextView
    private lateinit var textForgotPassword: TextView
    private lateinit var progressLogin: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EdgeToEdge.enable(this)

        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()

        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("HabitNowPrefs", MODE_PRIVATE)

        // Check if user is already logged in
        if (isUserLoggedIn()) {
            navigateToMainActivity()
            return
        }

        // Check if we should auto-login (after guest registration)
        if (shouldAutoLogin()) {
            handleAutoLogin()
            return
        }

        // Auto-fill email if remember me was checked
        loadRememberedCredentials()
    }

    private fun initializeViews() {
        editLoginEmail = findViewById(R.id.edit_login_email)
        editLoginPassword = findViewById(R.id.edit_login_password)
        checkboxRemember = findViewById(R.id.checkbox_remember)
        btnLogin = findViewById(R.id.btn_login)
        textRegisterLink = findViewById(R.id.text_register_link)
        textGuestMode = findViewById(R.id.text_guest_mode)
        textForgotPassword = findViewById(R.id.text_forgot_password)
        progressLogin = findViewById(R.id.progress_login)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener { handleLogin() }

        textRegisterLink.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        textGuestMode.setOnClickListener {
            // Login as guest
            loginAsGuest()
        }

        textForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun handleLogin() {
        val email = editLoginEmail.text.toString().trim()
        val password = editLoginPassword.text.toString().trim()

        // Validate input
        if (!validateInput(email, password)) {
            return
        }

        // Show progress
        showProgress(true)

        // Simulate login process (in real app, this would be API call)
        loginUser(email, password)
    }

    private fun validateInput(email: String, password: String): Boolean {
        // Check if fields are empty
        if (TextUtils.isEmpty(email)) {
            editLoginEmail.error = "Email is required"
            editLoginEmail.requestFocus()
            return false
        }

        if (TextUtils.isEmpty(password)) {
            editLoginPassword.error = "Password is required"
            editLoginPassword.requestFocus()
            return false
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editLoginEmail.error = "Please enter a valid email address"
            editLoginEmail.requestFocus()
            return false
        }

        return true
    }

    private fun loginUser(email: String, password: String) {
        // Simulate network delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Check if user exists
                val storedPassword = sharedPreferences.getString("user_password_$email", null)
                val storedName = sharedPreferences.getString("user_name_$email", null)

                if (storedPassword != null && storedPassword == password) {
                    // Login successful
                    val editor = sharedPreferences.edit()
                    editor.putString("current_user_email", email)
                    editor.putString("current_user_name", storedName)
                    editor.putBoolean("is_logged_in", true)
                    editor.putLong("last_login", System.currentTimeMillis())

                    // Save remember me preference
                    if (checkboxRemember.isChecked) {
                        editor.putString("remembered_email", email)
                        editor.putBoolean("remember_me", true)
                    } else {
                        editor.remove("remembered_email")
                        editor.putBoolean("remember_me", false)
                    }

                    editor.apply()

                    showProgress(false)

                    Toast.makeText(
                        this@LoginActivity,
                        "Welcome back, $storedName!",
                        Toast.LENGTH_SHORT
                    ).show()

                    navigateToMainActivity()
                } else {
                    // Login failed
                    showProgress(false)
                    Toast.makeText(
                        this@LoginActivity,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                showProgress(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Login failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 1500) // 1.5 second delay to simulate network call
    }

    private fun loginAsGuest() {
        // Set guest mode
        val editor = sharedPreferences.edit()
        editor.putString("current_user_email", "guest@habitnow.com")
        editor.putString("current_user_name", "Guest User")
        editor.putBoolean("is_logged_in", true)
        editor.putBoolean("is_guest", true)
        editor.putLong("last_login", System.currentTimeMillis())
        editor.apply()

        Toast.makeText(this, "Welcome, Guest User!", Toast.LENGTH_SHORT).show()
        navigateToMainActivity()
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun loadRememberedCredentials() {
        if (sharedPreferences.getBoolean("remember_me", false)) {
            val rememberedEmail = sharedPreferences.getString("remembered_email", "")
            if (!TextUtils.isEmpty(rememberedEmail)) {
                editLoginEmail.setText(rememberedEmail)
                checkboxRemember.isChecked = true
                editLoginPassword.requestFocus()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progressLogin.visibility = View.VISIBLE
            btnLogin.visibility = View.GONE
        } else {
            progressLogin.visibility = View.GONE
            btnLogin.visibility = View.VISIBLE
        }
    }

    private fun shouldAutoLogin(): Boolean {
        return sharedPreferences.getBoolean("should_auto_login", false)
    }

    private fun handleAutoLogin() {
        val email = sharedPreferences.getString("auto_login_email", "")
        val password = sharedPreferences.getString("auto_login_password", "")
        val name = sharedPreferences.getString("auto_login_name", "")

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(name)) {
            // Show progress briefly
            showProgress(true)

            // Simulate a brief delay to show the login process
            Handler(Looper.getMainLooper()).postDelayed({
                // Set user as logged in
                val editor = sharedPreferences.edit()
                editor.putString("current_user_email", email)
                editor.putString("current_user_name", name)
                editor.putBoolean("is_logged_in", true)
                editor.putLong("last_login", System.currentTimeMillis())

                // Clear auto-login data
                editor.remove("should_auto_login")
                editor.remove("auto_login_email")
                editor.remove("auto_login_password")
                editor.remove("auto_login_name")

                editor.apply()

                showProgress(false)

                Toast.makeText(this@LoginActivity, "Welcome back, $name!", Toast.LENGTH_SHORT)
                    .show()

                navigateToMainActivity()
            }, 1000) // 1 second delay
        } else {
            // Clear invalid auto-login data and proceed normally
            val editor = sharedPreferences.edit()
            editor.remove("should_auto_login")
            editor.remove("auto_login_email")
            editor.remove("auto_login_password")
            editor.remove("auto_login_name")
            editor.apply()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Exit app or minimize when back is pressed on login
        moveTaskToBack(true)
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.setCancelable(true)

        val editResetEmail: EditText = dialogView.findViewById(R.id.edit_reset_email)
        val editNewPassword: EditText = dialogView.findViewById(R.id.edit_new_password)
        val editConfirmPassword: EditText = dialogView.findViewById(R.id.edit_confirm_password)
        val btnResetPassword: Button = dialogView.findViewById(R.id.btn_reset_password)
        val btnCancelReset: Button = dialogView.findViewById(R.id.btn_cancel_reset)
        val progressReset: ProgressBar = dialogView.findViewById(R.id.progress_reset)

        btnCancelReset.setOnClickListener { dialog.dismiss() }

        btnResetPassword.setOnClickListener {
            val email = editResetEmail.text.toString().trim()
            val newPassword = editNewPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()

            if (validateResetInput(email, newPassword, confirmPassword)) {
                resetPassword(email, newPassword, dialog, progressReset)
            }
        }

        dialog.show()
    }

    private fun validateResetInput(
        email: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return false
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "New password is required", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT)
                .show()
            return false
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun resetPassword(
        email: String,
        newPassword: String,
        dialog: AlertDialog,
        progressReset: ProgressBar
    ) {
        // Show progress
        progressReset.visibility = View.VISIBLE

        // Simulate network delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Check if user exists
                val storedPassword = sharedPreferences.getString("user_password_$email", null)
                val storedName = sharedPreferences.getString("user_name_$email", null)

                if (storedPassword != null && storedName != null) {
                    // User exists, update password
                    val editor = sharedPreferences.edit()
                    editor.putString("user_password_$email", newPassword)
                    editor.apply()

                    progressReset.visibility = View.GONE
                    dialog.dismiss()

                    Toast.makeText(
                        this@LoginActivity,
                        "Password reset successful! You can now login with your new password.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Pre-fill email in login form
                    editLoginEmail.setText(email)
                    editLoginPassword.requestFocus()
                } else {
                    // User not found
                    progressReset.visibility = View.GONE
                    Toast.makeText(
                        this@LoginActivity,
                        "No account found with this email address",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                progressReset.visibility = View.GONE
                Toast.makeText(
                    this@LoginActivity,
                    "Password reset failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 2000) // 2 second delay to simulate network call
    }
}