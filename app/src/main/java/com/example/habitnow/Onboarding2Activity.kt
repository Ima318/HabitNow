package com.example.habitnow

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Onboarding2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding2)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.onb2_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<android.view.View>(R.id.button_next).setOnClickListener {
            startActivity(Intent(this@Onboarding2Activity, Onboarding3Activity::class.java))
            finish()
        }

        findViewById<android.view.View>(R.id.button_skip).setOnClickListener {
            startActivity(Intent(this@Onboarding2Activity, LoginActivity::class.java))
            finish()
        }
    }
}