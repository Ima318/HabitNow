package com.example.habitnow

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterActivity : AppCompatActivity() {

    private lateinit var editFullName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var textLoginLink: TextView
    private lateinit var progressRegister: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupClickListeners()

        // Initialize SharedPreferences for storing user data
        sharedPreferences = getSharedPreferences("HabitNowPrefs", MODE_PRIVATE)
    }

    private fun initializeViews() {
        editFullName = findViewById(R.id.edit_full_name)
        editEmail = findViewById(R.id.edit_email)
        editPassword = findViewById(R.id.edit_password)
        editConfirmPassword = findViewById(R.id.edit_confirm_password)
        btnRegister = findViewById(R.id.btn_register)
        textLoginLink = findViewById(R.id.text_login_link)
        progressRegister = findViewById(R.id.progress_register)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener { handleRegistration() }

        textLoginLink.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun handleRegistration() {
        val fullName = editFullName.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val confirmPassword = editConfirmPassword.text.toString().trim()

        // Validate input
        if (!validateInput(fullName, email, password, confirmPassword)) {
            return
        }

        // Show progress
        showProgress(true)

        // Simulate registration process (in real app, this would be API call)
        registerUser(fullName, email, password)
    }

    private fun validateInput(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Check if fields are empty
        if (TextUtils.isEmpty(fullName)) {
            editFullName.error = "Full name is required"
            editFullName.requestFocus()
            return false
        }

        if (TextUtils.isEmpty(email)) {
            editEmail.error = "Email is required"
            editEmail.requestFocus()
            return false
        }

        if (TextUtils.isEmpty(password)) {
            editPassword.error = "Password is required"
            editPassword.requestFocus()
            return false
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            editConfirmPassword.error = "Please confirm your password"
            editConfirmPassword.requestFocus()
            return false
        }

        // Validate email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Please enter a valid email address"
            editEmail.requestFocus()
            return false
        }

        // Validate password length
        if (password.length < 6) {
            editPassword.error = "Password must be at least 6 characters long"
            editPassword.requestFocus()
            return false
        }

        // Check if passwords match
        if (password != confirmPassword) {
            editConfirmPassword.error = "Passwords do not match"
            editConfirmPassword.requestFocus()
            return false
        }

        // Check if user already exists
        if (sharedPreferences.contains("user_email_$email")) {
            editEmail.error = "An account with this email already exists"
            editEmail.requestFocus()
            showProgress(false)
            return false
        }

        return true
    }

    private fun registerUser(fullName: String, email: String, password: String) {
        // Simulate network delay
        Handler().postDelayed({
            try {
                // Check if user was previously a guest
                val wasGuest = sharedPreferences.getBoolean("is_guest", false)

                // Store user data in SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("user_email_$email", email)
                editor.putString("user_name_$email", fullName)
                editor.putString("user_password_$email", password)
                editor.putLong("registration_date", System.currentTimeMillis())

                // Clear guest status if they were a guest
                if (wasGuest) {
                    editor.remove("is_guest")
                }

                editor.apply()

                showProgress(false)

                Toast.makeText(
                    this@RegisterActivity,
                    "Account created successfully! Welcome $fullName",
                    Toast.LENGTH_LONG
                ).show()

                if (wasGuest) {
                    // If user was a guest, redirect to login and auto-login them
                    navigateToLoginAndAutoLogin(email, password, fullName)
                } else {
                    // Regular registration flow - directly login and go to main activity
                    loginUserAndNavigateToMain(email, fullName)
                }

            } catch (e: Exception) {
                showProgress(false)
                Toast.makeText(
                    this@RegisterActivity,
                    "Registration failed. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, 2000) // 2 second delay to simulate network call
    }

    private fun navigateToLoginAndAutoLogin(email: String, password: String, fullName: String) {
        // Set login credentials and user data for auto-login
        val editor = sharedPreferences.edit()
        editor.putString("auto_login_email", email)
        editor.putString("auto_login_password", password)
        editor.putString("auto_login_name", fullName)
        editor.putBoolean("should_auto_login", true)
        editor.apply()

        // Navigate to login activity
        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loginUserAndNavigateToMain(email: String, fullName: String) {
        // Set user as logged in and navigate to main activity
        val editor = sharedPreferences.edit()
        editor.putString("current_user_email", email)
        editor.putString("current_user_name", fullName)
        editor.putBoolean("is_logged_in", true)
        editor.apply()

        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progressRegister.visibility = View.VISIBLE
            btnRegister.visibility = View.GONE
        } else {
            progressRegister.visibility = View.GONE
            btnRegister.visibility = View.VISIBLE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Navigate to login if user presses back
        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}