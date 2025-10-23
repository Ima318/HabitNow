package com.example.habitnow

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.*
//listen for changes in stored data,
// like moods, habits, or hydration, and update the UI automatically
class MainActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {

    // Widget Views
    private lateinit var moodEmojiText: TextView
    private lateinit var moodNameText: TextView
    private lateinit var hydrationPercentageText: TextView
    private lateinit var hydrationAmountText: TextView
    private lateinit var habitsPercentageText: TextView
    private lateinit var habitsCountText: TextView
    private lateinit var hydrationProgressBar: ProgressBar
    private lateinit var habitsProgressBar: ProgressBar
    private lateinit var greetingText: TextView

    // Sample data for demonstration
    private val moodEmojis = mutableListOf<String>()
    private val moodNames = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var habitsPrefs: SharedPreferences
    private lateinit var moodPrefs: SharedPreferences
    private lateinit var hydrationPrefs: SharedPreferences

    // Handler for periodic updates
    private lateinit var updateHandler: Handler
    private lateinit var updateRunnable: Runnable
    private val updateInterval = 5000L // 5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()

        // Initialize SharedPreferences
        //Sets up SharedPreferences and registers listeners so changes in data automatically refresh the UI
        sharedPreferences = getSharedPreferences("HabitNowPrefs", MODE_PRIVATE)
        habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)

        habitsPrefs.registerOnSharedPreferenceChangeListener(this)
        moodPrefs.registerOnSharedPreferenceChangeListener(this)
        hydrationPrefs.registerOnSharedPreferenceChangeListener(this)

        // Check if user is logged in
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        initializeSampleData()
        initializeWidgetViews()
        setupClickListeners()
        loadTodaysSummary()

        setupBottomNavigation()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Update greeting with user name
        updateGreeting()

        // Start periodic updates
        updateHandler = Handler(Looper.getMainLooper())
        updateRunnable = Runnable {
            loadTodaysSummary()
            updateHandler.postDelayed(updateRunnable, updateInterval)
        }
        updateHandler.postDelayed(updateRunnable, updateInterval)
    }
//refreshes data when returning to home screen.
    override fun onResume() {
        super.onResume()
        // Check authentication on resume
        if (!isUserLoggedIn()) {
            navigateToLogin()
            return
        }
        loadTodaysSummary() // Refresh data when returning to main screen
        updateGreeting()
    }
//cleans up listeners and handler callbacks to avoid memory leaks.
    override fun onDestroy() {
        super.onDestroy()
        habitsPrefs.unregisterOnSharedPreferenceChangeListener(this)
        moodPrefs.unregisterOnSharedPreferenceChangeListener(this)
        hydrationPrefs.unregisterOnSharedPreferenceChangeListener(this)
        updateHandler.removeCallbacksAndMessages(null)
    }

    private fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false)
    }

    private fun navigateToLogin() {
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun initializeSampleData() {
        // Sample mood data
        moodEmojis.apply {
            add("😊")
            add("😍")
            add("😎")
            add("🤗")
            add("😌")
            add("🙂")
            add("😐")
            add("😔")
        }

        moodNames.apply {
            add("Happy")
            add("Love")
            add("Cool")
            add("Excited")
            add("Calm")
            add("Content")
            add("Neutral")
            add("Sad")
        }
    }

    private fun initializeWidgetViews() {
        // Mood widget views
        moodEmojiText = findViewById(R.id.text_today_mood_emoji)
        moodNameText = findViewById(R.id.text_today_mood_name)

        // Hydration widget views
        hydrationPercentageText = findViewById(R.id.text_hydration_percentage)
        hydrationAmountText = findViewById(R.id.text_hydration_amount)
        hydrationProgressBar = findViewById(R.id.progress_hydration_widget)

        // Habits widget views
        habitsPercentageText = findViewById(R.id.text_habits_percentage)
        habitsCountText = findViewById(R.id.text_habits_count)
        habitsProgressBar = findViewById(R.id.progress_habits_widget)

        // Greeting text
        greetingText = findViewById(R.id.text_greeting)
    }

    private fun setupClickListeners() {
        // Main action buttons
        findViewById<View>(R.id.button_my_habits).setOnClickListener {
            startActivity(Intent(this@MainActivity, HabitsActivity::class.java))
        }

        findViewById<View>(R.id.button_mood_journal).setOnClickListener {
            startActivity(Intent(this@MainActivity, MoodJournalActivity::class.java))
        }

        findViewById<View>(R.id.button_hydration).setOnClickListener {
            startActivity(Intent(this@MainActivity, HydrationActivity::class.java))
        }

        findViewById<View>(R.id.button_analytics).setOnClickListener {
            startActivity(Intent(this@MainActivity, AnalyticsActivity::class.java))
        }

        findViewById<View>(R.id.button_settings).setOnClickListener {
            startActivity(Intent(this@MainActivity, WidgetSettingsActivity::class.java))
        }

        // Widget click listeners
        val moodWidget = findViewById<LinearLayout>(R.id.widget_mood)
        moodWidget.setOnClickListener {
            val intent = Intent(this@MainActivity, MoodJournalActivity::class.java)
            startActivity(intent)
        }

        val hydrationWidget = findViewById<LinearLayout>(R.id.widget_hydration)
        hydrationWidget.setOnClickListener {
            val intent = Intent(this@MainActivity, HydrationActivity::class.java)
            startActivity(intent)
        }

        val habitsWidget = findViewById<LinearLayout>(R.id.widget_habits)
        habitsWidget.setOnClickListener {
            val intent = Intent(this@MainActivity, HabitsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTodaysSummary() {
        loadTodaysMood()
        loadTodaysHydration()
        loadTodaysHabits()
    }

    private fun loadTodaysMood() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = dateFormat.format(Date())

        val todayMood = moodPrefs.getString("${todayKey}_emoji", "")
        val todayMoodName = moodPrefs.getString("${todayKey}_name", "")

        if (todayMood.isNullOrEmpty()) {
            // Show default mood if none logged today
            moodEmojiText.text = "😐"
            moodNameText.text = "Not set"
        } else {
            moodEmojiText.text = todayMood
            moodNameText.text = todayMoodName
        }
    }

    private fun loadTodaysHydration() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = dateFormat.format(Date())

        val todayIntake = hydrationPrefs.getInt("${todayKey}_intake", 0)
        var dailyGoal = hydrationPrefs.getInt("daily_goal", 2000)

        // Set default goal for new users if not set
        if (hydrationPrefs.getInt("daily_goal", 0) == 0) {
            val editor = hydrationPrefs.edit()
            editor.putInt("daily_goal", 2000)
            editor.apply()
            dailyGoal = 2000
        }

        val percentage = minOf(100, ((todayIntake * 100f) / dailyGoal).toInt())

        hydrationPercentageText.text = "$percentage%"
        hydrationAmountText.text = "$todayIntake ml / $dailyGoal ml"
        hydrationProgressBar.progress = percentage
    }

    private fun loadTodaysHabits() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = dateFormat.format(Date())

        val completedHabits = habitsPrefs.getInt("${todayKey}_completed", 0)
        val totalHabits = habitsPrefs.getInt("${todayKey}_total", 0)

        val percentage =
            if (totalHabits == 0) 0 else ((completedHabits * 100f) / totalHabits).toInt()

        habitsPercentageText.text = "$percentage%"
        habitsCountText.text = "$completedHabits / $totalHabits completed"
        habitsProgressBar.progress = percentage

        // Update HabitProgressWidgetProvider for consistency
        val widgetPrefs = getSharedPreferences(HabitProgressWidgetProvider.PREFS_NAME, MODE_PRIVATE)
        val widgetEditor = widgetPrefs.edit()
        widgetEditor.putInt(HabitProgressWidgetProvider.KEY_COMPLETED, completedHabits)
        widgetEditor.putInt(HabitProgressWidgetProvider.KEY_TOTAL, totalHabits)
        widgetEditor.apply()
        HabitProgressWidgetProvider.requestUpdateAll(this)
    }

    private fun updateGreeting() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        // Get user name
        val userName = sharedPreferences.getString("current_user_name", "User")
        val isGuest = sharedPreferences.getBoolean("is_guest", false)

        val greeting = if (isGuest) {
            "$timeGreeting, Guest! 👋"
        } else {
            "$timeGreeting, $userName! 👋"
        }

        greetingText.text = greeting
    }

    private fun setupBottomNavigation() {
        // Home navigation (current page - already highlighted in layout)
        findViewById<View>(R.id.nav_home).setOnClickListener {
            // Already on home page
        }

        // Habits navigation
        findViewById<View>(R.id.nav_habits).setOnClickListener {
            val intent = Intent(this@MainActivity, HabitsActivity::class.java)
            startActivity(intent)
        }

        // Mood Journal navigation  
        findViewById<View>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this@MainActivity, MoodJournalActivity::class.java)
            startActivity(intent)
        }

        // Hydration navigation
        findViewById<View>(R.id.nav_hydration).setOnClickListener {
            val intent = Intent(this@MainActivity, HydrationActivity::class.java)
            startActivity(intent)
        }

        // Settings navigation
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this@MainActivity, WidgetSettingsActivity::class.java)
            startActivity(intent)
        }
    }
//Automatically updates widgets when any relevant data changes, e.g., hydration, habits, or mood.
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadTodaysSummary()
    }
}