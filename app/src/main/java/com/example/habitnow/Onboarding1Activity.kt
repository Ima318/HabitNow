package com.example.habitnow

import android.content.Intent
import android.os.Bundle
import android.view.View
// import androidx.activity.EdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Onboarding1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EdgeToEdge.enable(this)
        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()

        setContentView(R.layout.activity_onboarding1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onb1_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.button_next).setOnClickListener {
            startActivity(Intent(this@Onboarding1Activity, Onboarding2Activity::class.java))
            finish()
        }

        findViewById<View>(R.id.button_skip).setOnClickListener {
            startActivity(Intent(this@Onboarding1Activity, LoginActivity::class.java))
            finish()
        }
    }
}