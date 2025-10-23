package com.example.habitnow

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Onboarding3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding3)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onb3_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.button_get_started).setOnClickListener {
            startActivity(Intent(this@Onboarding3Activity, LoginActivity::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.button_skip).setOnClickListener {
            startActivity(Intent(this@Onboarding3Activity, LoginActivity::class.java))
            finish()
        }
    }
}