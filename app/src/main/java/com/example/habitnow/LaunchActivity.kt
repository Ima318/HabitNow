package com.example.habitnow

import android.content.Intent
import android.os.Bundle
// import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LaunchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EdgeToEdge.enable(this)

        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()

        setContentView(R.layout.activity_launch)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.launch_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.button_get_started).setOnClickListener {
            startActivity(Intent(this@LaunchActivity, Onboarding1Activity::class.java))
            finish()
        }
    }
}