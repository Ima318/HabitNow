package com.example.habitnow

import android.app.Activity
import android.content.Intent
import android.view.View

object BaseNavigation {
    fun wireTopNav(activity: Activity, root: View) {
        // Top navigation buttons have been removed - no longer needed
    }

    fun wireBottomNav(activity: Activity, root: View) {
        val home = root.findViewById<View>(R.id.nav_home)
        val habits = root.findViewById<View>(R.id.nav_habits)
        val mood = root.findViewById<View>(R.id.nav_mood)
        val hydration = root.findViewById<View>(R.id.nav_hydration)
        val settings = root.findViewById<View>(R.id.nav_settings)

        home?.setOnClickListener {
            activity.startActivity(
                Intent(
                    activity,
                    MainActivity::class.java
                )
            )
        }
        habits?.setOnClickListener {
            activity.startActivity(
                Intent(
                    activity,
                    HabitsActivity::class.java
                )
            )
        }
        mood?.setOnClickListener {
            activity.startActivity(
                Intent(
                    activity,
                    MoodJournalActivity::class.java
                )
            )
        }
        hydration?.setOnClickListener {
            activity.startActivity(
                Intent(
                    activity,
                    HydrationActivity::class.java
                )
            )
        }
        settings?.setOnClickListener {
            activity.startActivity(
                Intent(
                    activity,
                    WidgetSettingsActivity::class.java
                )
            )
        }
    }
}