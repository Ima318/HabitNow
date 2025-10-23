package com.example.habitnow

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// import androidx.activity.EdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var analyticsRecycler: RecyclerView
    private lateinit var adapter: AnalyticsAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var currentPeriod = "WEEKLY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // EdgeToEdge.enable(this)

        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()

        setContentView(R.layout.activity_analytics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.analytics_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("HabitNowPrefs", MODE_PRIVATE)

        initializeViews()
        setupTabs()
        setupRecyclerView()
        loadAnalyticsData()

        BaseNavigation.wireTopNav(this, findViewById(R.id.top_nav_include))
        setupBottomNavigation()
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.tab_layout_period)
        analyticsRecycler = findViewById(R.id.recycler_analytics)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Weekly"))
        tabLayout.addTab(tabLayout.newTab().setText("Monthly"))
        tabLayout.addTab(tabLayout.newTab().setText("Yearly"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentPeriod = when (tab.position) {
                    0 -> "WEEKLY"
                    1 -> "MONTHLY"
                    2 -> "YEARLY"
                    else -> "WEEKLY"
                }
                loadAnalyticsData()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = AnalyticsAdapter()
        analyticsRecycler.layoutManager = LinearLayoutManager(this)
        analyticsRecycler.adapter = adapter
    }

    //It creates four cards: Overview, Habits, Mood, Hydration.
    //Each card calls helper functions that iterate over dates in the selected
    // period and fetch SharedPreferences data.
    private fun loadAnalyticsData() {
        val cards = mutableListOf<AnalyticsCard>()

        // Add overview card
        cards.add(createOverviewCard())

        // Add habits analytics
        cards.add(createHabitsAnalyticsCard())

        // Add mood analytics
        cards.add(createMoodAnalyticsCard())

        // Add hydration analytics
        cards.add(createHydrationAnalyticsCard())

        adapter.updateCards(cards)
    }

    private fun createOverviewCard(): AnalyticsCard {
        val card = AnalyticsCard()
        card.title = getPeriodTitle() + " Overview"
        card.type = AnalyticsCard.TYPE_OVERVIEW

        val cal = Calendar.getInstance()
        val range = getDateRange(cal)

        // Calculate overall stats
        val stats = OverviewStats().apply {
            habitsCompletionRate = calculateHabitsCompletionRate(range)
            averageMoodScore = calculateAverageMoodScore(range)
            hydrationGoalAchievement = calculateHydrationGoalAchievement(range)
            totalDays = calculateTotalDays(range)
            activeDays = calculateActiveDays(range)
        }

        card.data = stats
        return card
    }

    private fun createHabitsAnalyticsCard(): AnalyticsCard {
        val card = AnalyticsCard()
        card.title = "Habits ${currentPeriod.lowercase()} Report"
        card.type = AnalyticsCard.TYPE_HABITS

        val cal = Calendar.getInstance()
        val range = getDateRange(cal)

        val stats = HabitsStats().apply {
            totalHabits = getTotalHabitsCount(range)
            completedHabits = getCompletedHabitsCount(range)
            completionRate = if (totalHabits > 0) (completedHabits * 100) / totalHabits else 0
            bestStreak = calculateBestHabitStreak(range)
            currentStreak = calculateCurrentHabitStreak()
            dailyAverages = calculateDailyHabitAverages(range)
        }

        card.data = stats
        return card
    }

    private fun createMoodAnalyticsCard(): AnalyticsCard {
        val card = AnalyticsCard()
        card.title = "Mood ${currentPeriod.lowercase()} Report"
        card.type = AnalyticsCard.TYPE_MOOD

        val cal = Calendar.getInstance()
        val range = getDateRange(cal)

        val stats = MoodStats().apply {
            totalEntries = getTotalMoodEntries(range)
            averageScore = calculateAverageMoodScore(range)
            bestMood = getBestMood(range)
            moodDistribution = getMoodDistribution(range)
            moodTrend = calculateMoodTrend(range)
        }

        card.data = stats
        return card
    }

    private fun createHydrationAnalyticsCard(): AnalyticsCard {
        val card = AnalyticsCard()
        card.title = "Hydration ${currentPeriod.lowercase()} report"
        card.type = AnalyticsCard.TYPE_HYDRATION

        val cal = Calendar.getInstance()
        val range = getDateRange(cal)

        val stats = HydrationStats().apply {
            averageDailyIntake = calculateAverageDailyIntake(range)
            goalAchievementDays = calculateHydrationGoalDays(range)
            totalDays = calculateTotalDays(range)
            goalAchievementRate = if (totalDays > 0) (goalAchievementDays * 100) / totalDays else 0
            bestDay = getBestHydrationDay(range)
            dailyAverages = calculateDailyHydrationAverages(range)
        }

        card.data = stats
        return card
    }

    private fun getPeriodTitle(): String {
        return when (currentPeriod) {
            "WEEKLY" -> "This Week's"
            "MONTHLY" -> "This Month's"
            "YEARLY" -> "This Year's"
            else -> "Weekly"
        }
    }

    private fun getDateRange(cal: Calendar): DateRange {
        val range = DateRange()
        val start = cal.clone() as Calendar
        val end = cal.clone() as Calendar

        when (currentPeriod) {
            "WEEKLY" -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.add(Calendar.WEEK_OF_YEAR, 1)
                end.add(Calendar.DAY_OF_YEAR, -1)
            }

            "MONTHLY" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            "YEARLY" -> {
                start.set(Calendar.DAY_OF_YEAR, 1)
                end.set(Calendar.DAY_OF_YEAR, end.getActualMaximum(Calendar.DAY_OF_YEAR))
            }
        }

        range.startDate = start.time
        range.endDate = end.time
        return range
    }

    // Analytics calculation methods
    private fun calculateHabitsCompletionRate(range: DateRange): Int {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var totalHabits = 0
        var completedHabits = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val dayTotal = habitsPrefs.getInt("${dateKey}_total", 0)
            val dayCompleted = habitsPrefs.getInt("${dateKey}_completed", 0)

            totalHabits += dayTotal
            completedHabits += dayCompleted

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return if (totalHabits > 0) (completedHabits * 100) / totalHabits else 0
    }

    private fun calculateAverageMoodScore(range: DateRange): Double {
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var totalScore = 0.0
        var entryCount = 0

        // Mood to score mapping
        val moodScores = mapOf(
            "😊" to 5.0, // Happy
            "😍" to 5.0, // Love
            "😎" to 4.5, // Cool
            "🤗" to 4.5, // Excited
            "😌" to 4.0, // Calm
            "🙂" to 3.5, // Content
            "😐" to 3.0, // Neutral
            "😔" to 2.0, // Sad
            "😟" to 2.5, // Worried
            "😩" to 1.5, // Frustrated
            "😴" to 3.0, // Tired
            "🤒" to 1.5  // Sick
        )

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val moodEmoji = moodPrefs.getString("${dateKey}_emoji", "") ?: ""

            if (moodEmoji.isNotEmpty() && moodScores.containsKey(moodEmoji)) {
                totalScore += moodScores[moodEmoji] ?: 0.0
                entryCount++
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return if (entryCount > 0) totalScore / entryCount else 0.0
    }

    private fun calculateHydrationGoalAchievement(range: DateRange): Int {
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyGoal = hydrationPrefs.getInt("daily_goal", 2000)

        var goalAchievedDays = 0
        var totalDays = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val dailyIntake = hydrationPrefs.getInt("${dateKey}_intake", 0)

            if (dailyIntake >= dailyGoal) {
                goalAchievedDays++
            }
            totalDays++

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return if (totalDays > 0) (goalAchievedDays * 100) / totalDays else 0
    }

    private fun calculateActiveDays(range: DateRange): Int {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var activeDays = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)

            val hasHabits = habitsPrefs.getInt("${dateKey}_total", 0) > 0
            val hasMood = (moodPrefs.getString("${dateKey}_emoji", "") ?: "").isNotEmpty()
            val hasHydration = hydrationPrefs.getInt("${dateKey}_intake", 0) > 0

            if (hasHabits || hasMood || hasHydration) {
                activeDays++
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return activeDays
    }

    private fun getTotalHabitsCount(range: DateRange): Int {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var totalHabits = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            totalHabits += habitsPrefs.getInt("${dateKey}_total", 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return totalHabits
    }

    private fun getCompletedHabitsCount(range: DateRange): Int {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var completedHabits = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            completedHabits += habitsPrefs.getInt("${dateKey}_completed", 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return completedHabits
    }

    private fun calculateBestHabitStreak(range: DateRange): Int {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var bestStreak = 0
        var currentStreak = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val total = habitsPrefs.getInt("${dateKey}_total", 0)
            val completed = habitsPrefs.getInt("${dateKey}_completed", 0)

            if (total > 0 && completed == total) {
                currentStreak++
                bestStreak = maxOf(bestStreak, currentStreak)
            } else {
                currentStreak = 0
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return bestStreak
    }

    private fun calculateCurrentHabitStreak(): Int {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var currentStreak = 0
        val cal = Calendar.getInstance()

        // Check backwards from today
        for (i in 0 until 30) { // Check last 30 days max
            val dateKey = dateFormat.format(cal.time)
            val total = habitsPrefs.getInt("${dateKey}_total", 0)
            val completed = habitsPrefs.getInt("${dateKey}_completed", 0)

            if (total > 0 && completed == total) {
                currentStreak++
            } else if (total > 0) {
                break // Streak broken
            }

            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        return currentStreak
    }

    private fun calculateDailyHabitAverages(range: DateRange): List<DailyAverage> {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dayOfWeekData = mutableMapOf<Int, MutableList<Int>>()
        for (i in 1..7) {
            dayOfWeekData[i] = mutableListOf()
        }

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val total = habitsPrefs.getInt("${dateKey}_total", 0)
            val completed = habitsPrefs.getInt("${dateKey}_completed", 0)

            if (total > 0) {
                val percentage = (completed * 100) / total
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                dayOfWeekData[dayOfWeek]?.add(percentage)
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val averages = mutableListOf<DailyAverage>()
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 1..7) {
            val percentages = dayOfWeekData[i] ?: mutableListOf()
            val average = if (percentages.isNotEmpty()) {
                percentages.sum() / percentages.size
            } else {
                0
            }

            averages.add(DailyAverage().apply {
                day = days[i - 1]
                value = average
            })
        }

        return averages
    }

    private fun getTotalMoodEntries(range: DateRange): Int {
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var entryCount = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val moodEmoji = moodPrefs.getString("${dateKey}_emoji", "") ?: ""

            if (moodEmoji.isNotEmpty()) {
                entryCount++
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return entryCount
    }

    private fun getBestMood(range: DateRange): String {
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val moodCounts = mutableMapOf<String, Int>()
        val moodNames = mapOf(
            "😊" to "Happy",
            "😍" to "Love",
            "😎" to "Cool",
            "🤗" to "Excited",
            "😌" to "Calm",
            "🙂" to "Content",
            "😐" to "Neutral",
            "😔" to "Sad",
            "😟" to "Worried",
            "😩" to "Frustrated",
            "😴" to "Tired",
            "🤒" to "Sick"
        )

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val moodEmoji = moodPrefs.getString("${dateKey}_emoji", "") ?: ""

            if (moodEmoji.isNotEmpty()) {
                moodCounts[moodEmoji] = moodCounts.getOrDefault(moodEmoji, 0) + 1
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        var bestMoodEmoji = ""
        var maxCount = 0
        for ((emoji, count) in moodCounts) {
            if (count > maxCount) {
                maxCount = count
                bestMoodEmoji = emoji
            }
        }

        return if (bestMoodEmoji.isNotEmpty()) {
            "$bestMoodEmoji ${moodNames[bestMoodEmoji]}"
        } else {
            "No mood data"
        }
    }

    private fun getMoodDistribution(range: DateRange): Map<String, Int> {
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val distribution = mutableMapOf<String, Int>()
        val moodNames = mapOf(
            "😊" to "😊 Happy",
            "😍" to "😍 Love",
            "😎" to "😎 Cool",
            "🤗" to "🤗 Excited",
            "😌" to "😌 Calm",
            "🙂" to "🙂 Content",
            "😐" to "😐 Neutral",
            "😔" to "😔 Sad",
            "😟" to "😟 Worried",
            "😩" to "😩 Frustrated",
            "😴" to "😴 Tired",
            "🤒" to "🤒 Sick"
        )

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val moodEmoji = moodPrefs.getString("${dateKey}_emoji", "") ?: ""

            if (moodEmoji.isNotEmpty() && moodNames.containsKey(moodEmoji)) {
                val moodName = moodNames[moodEmoji] ?: ""
                distribution[moodName] = distribution.getOrDefault(moodName, 0) + 1
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return distribution
    }

    private fun calculateMoodTrend(range: DateRange): String {
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val moodScores = mapOf(
            "😊" to 5.0, // Happy
            "😍" to 5.0, // Love
            "😎" to 4.5, // Cool
            "🤗" to 4.5, // Excited
            "😌" to 4.0, // Calm
            "🙂" to 3.5, // Content
            "😐" to 3.0, // Neutral
            "😔" to 2.0, // Sad
            "😟" to 2.5, // Worried
            "😩" to 1.5, // Frustrated
            "😴" to 3.0, // Tired
            "🤒" to 1.5  // Sick
        )

        val scores = mutableListOf<Double>()
        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val moodEmoji = moodPrefs.getString("${dateKey}_emoji", "") ?: ""

            if (moodEmoji.isNotEmpty() && moodScores.containsKey(moodEmoji)) {
                scores.add(moodScores[moodEmoji] ?: 0.0)
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (scores.size < 2) {
            return "Not enough data"
        }

        // Calculate trend based on first half vs second half
        val midPoint = scores.size / 2
        val firstHalfAvg = scores.take(midPoint).average()
        val secondHalfAvg = scores.drop(midPoint).average()

        val difference = secondHalfAvg - firstHalfAvg

        return when {
            difference > 0.2 -> "Improving ↗"
            difference < -0.2 -> "Declining ↘"
            else -> "Stable →"
        }
    }

    private fun calculateAverageDailyIntake(range: DateRange): Int {
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var totalIntake = 0
        var daysWithData = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val dailyIntake = hydrationPrefs.getInt("${dateKey}_intake", 0)

            if (dailyIntake > 0) {
                totalIntake += dailyIntake
                daysWithData++
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return if (daysWithData > 0) totalIntake / daysWithData else 0
    }

    private fun calculateHydrationGoalDays(range: DateRange): Int {
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dailyGoal = hydrationPrefs.getInt("daily_goal", 2000)

        var goalAchievedDays = 0

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val dailyIntake = hydrationPrefs.getInt("${dateKey}_intake", 0)

            if (dailyIntake >= dailyGoal) {
                goalAchievedDays++
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return goalAchievedDays
    }

    private fun getBestHydrationDay(range: DateRange): String {
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var bestIntake = 0
        var bestDate = ""

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val dailyIntake = hydrationPrefs.getInt("${dateKey}_intake", 0)

            if (dailyIntake > bestIntake) {
                bestIntake = dailyIntake
                bestDate = dateKey
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return if (bestIntake > 0) {
            try {
                val date = dateFormat.parse(bestDate)
                val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                "$bestIntake ml on ${date?.let { displayFormat.format(it) }}"
            } catch (e: Exception) {
                "$bestIntake ml"
            }
        } else {
            "No hydration data"
        }
    }

    private fun calculateDailyHydrationAverages(range: DateRange): List<DailyAverage> {
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val dayOfWeekData = mutableMapOf<Int, MutableList<Int>>()
        for (i in 1..7) {
            dayOfWeekData[i] = mutableListOf()
        }

        val cal = Calendar.getInstance()
        cal.time = range.startDate

        while (!cal.time.after(range.endDate)) {
            val dateKey = dateFormat.format(cal.time)
            val dailyIntake = hydrationPrefs.getInt("${dateKey}_intake", 0)

            if (dailyIntake > 0) {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                dayOfWeekData[dayOfWeek]?.add(dailyIntake)
            }

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val averages = mutableListOf<DailyAverage>()
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 1..7) {
            val intakes = dayOfWeekData[i] ?: mutableListOf()
            val average = if (intakes.isNotEmpty()) {
                intakes.sum() / intakes.size
            } else {
                0
            }

            averages.add(DailyAverage().apply {
                day = days[i - 1]
                value = average
            })
        }

        return averages
    }

    private fun calculateTotalDays(range: DateRange): Int {
        val diffInMillies = kotlin.math.abs(range.endDate.time - range.startDate.time)
        return (diffInMillies / (1000 * 60 * 60 * 24)).toInt() + 1
    }

    // Data classes
    private class AnalyticsCard {
        companion object {
            const val TYPE_OVERVIEW = 0
            const val TYPE_HABITS = 1
            const val TYPE_MOOD = 2
            const val TYPE_HYDRATION = 3
        }

        var title: String = ""
        var type: Int = 0
        var data: Any? = null
    }

    private class DateRange {
        var startDate: Date = Date()
        var endDate: Date = Date()
    }

    private class OverviewStats {
        var habitsCompletionRate: Int = 0
        var averageMoodScore: Double = 0.0
        var hydrationGoalAchievement: Int = 0
        var totalDays: Int = 0
        var activeDays: Int = 0
    }

    private class HabitsStats {
        var totalHabits: Int = 0
        var completedHabits: Int = 0
        var completionRate: Int = 0
        var bestStreak: Int = 0
        var currentStreak: Int = 0
        var dailyAverages: List<DailyAverage> = listOf()
    }

    private class MoodStats {
        var totalEntries: Int = 0
        var averageScore: Double = 0.0
        var bestMood: String = ""
        var moodDistribution: Map<String, Int> = mapOf()
        var moodTrend: String = ""
    }

    private class HydrationStats {
        var averageDailyIntake: Int = 0
        var goalAchievementDays: Int = 0
        var totalDays: Int = 0
        var goalAchievementRate: Int = 0
        var bestDay: String = ""
        var dailyAverages: List<DailyAverage> = listOf()
    }

    private class DailyAverage {
        var day: String = ""
        var value: Int = 0
    }

    // Adapter class
    private class AnalyticsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var cards = listOf<AnalyticsCard>()

        fun updateCards(newCards: List<AnalyticsCard>) {
            this.cards = newCards
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return cards[position].type
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                AnalyticsCard.TYPE_OVERVIEW -> OverviewViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_analytics_overview, parent, false)
                )

                AnalyticsCard.TYPE_HABITS -> HabitsViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_analytics_habits, parent, false)
                )

                AnalyticsCard.TYPE_MOOD -> MoodViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_analytics_mood, parent, false)
                )

                AnalyticsCard.TYPE_HYDRATION -> HydrationViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_analytics_hydration, parent, false)
                )

                else -> OverviewViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_analytics_overview, parent, false)
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val card = cards[position]

            when (holder.itemViewType) {
                AnalyticsCard.TYPE_OVERVIEW -> (holder as OverviewViewHolder).bind(card)
                AnalyticsCard.TYPE_HABITS -> (holder as HabitsViewHolder).bind(card)
                AnalyticsCard.TYPE_MOOD -> (holder as MoodViewHolder).bind(card)
                AnalyticsCard.TYPE_HYDRATION -> (holder as HydrationViewHolder).bind(card)
            }
        }

        override fun getItemCount(): Int = cards.size
    }

    // ViewHolder classes
    private class OverviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.text_card_title)
        val habitsRateText: TextView = itemView.findViewById(R.id.text_habits_rate)
        val moodScoreText: TextView = itemView.findViewById(R.id.text_mood_score)
        val hydrationRateText: TextView = itemView.findViewById(R.id.text_hydration_rate)
        val activeDaysText: TextView = itemView.findViewById(R.id.text_active_days)

        fun bind(card: AnalyticsCard) {
            val stats = card.data as OverviewStats
            titleText.text = card.title
            habitsRateText.text = "${stats.habitsCompletionRate}%"
            moodScoreText.text =
                String.format(Locale.getDefault(), "%.1f/5.0", stats.averageMoodScore)
            hydrationRateText.text = "${stats.hydrationGoalAchievement}%"
            activeDaysText.text = "${stats.activeDays}/${stats.totalDays} days"
        }
    }

    private class HabitsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.text_card_title)
        val completionText: TextView = itemView.findViewById(R.id.text_completion_rate)
        val streakText: TextView = itemView.findViewById(R.id.text_streak_info)
        val dailyAvgText: TextView = itemView.findViewById(R.id.text_daily_avg)
        val dailyAveragesLayout: LinearLayout = itemView.findViewById(R.id.layout_daily_averages)

        fun bind(card: AnalyticsCard) {
            val stats = card.data as HabitsStats
            titleText.text = card.title
            completionText.text =
                "${stats.completedHabits}/${stats.totalHabits} (${stats.completionRate}%)"
            streakText.text =
                "Best: ${stats.bestStreak} days | Current: ${stats.currentStreak} days"

            // Show daily averages
            dailyAveragesLayout.removeAllViews()
            for (avg in stats.dailyAverages) {
                val dayView = TextView(itemView.context).apply {
                    text = "${avg.day}: ${avg.value}%"
                    textSize = 12f
                    setPadding(8, 4, 8, 4)
                }
                dailyAveragesLayout.addView(dayView)
            }
        }
    }

    private class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.text_card_title)
        val entriesText: TextView = itemView.findViewById(R.id.text_mood_entries)
        val avgScoreText: TextView = itemView.findViewById(R.id.text_avg_mood_score)
        val bestMoodText: TextView = itemView.findViewById(R.id.text_best_mood)
        val trendText: TextView = itemView.findViewById(R.id.text_mood_trend)
        val moodDistributionLayout: LinearLayout =
            itemView.findViewById(R.id.layout_mood_distribution)

        fun bind(card: AnalyticsCard) {
            val stats = card.data as MoodStats
            titleText.text = card.title
            entriesText.text = "${stats.totalEntries} entries"
            avgScoreText.text = String.format(Locale.getDefault(), "%.1f/5.0", stats.averageScore)
            bestMoodText.text = stats.bestMood
            trendText.text = stats.moodTrend

            // Show mood distribution
            moodDistributionLayout.removeAllViews()
            for ((mood, count) in stats.moodDistribution) {
                val moodView = TextView(itemView.context).apply {
                    text = "$mood: $count times"
                    textSize = 12f
                    setPadding(8, 4, 8, 4)
                }
                moodDistributionLayout.addView(moodView)
            }
        }
    }

    private class HydrationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.text_card_title)
        val avgIntakeText: TextView = itemView.findViewById(R.id.text_avg_intake)
        val goalAchievementText: TextView = itemView.findViewById(R.id.text_goal_achievement)
        val bestDayText: TextView = itemView.findViewById(R.id.text_best_day)
        val dailyAveragesLayout: LinearLayout = itemView.findViewById(R.id.layout_daily_averages)

        fun bind(card: AnalyticsCard) {
            val stats = card.data as HydrationStats
            titleText.text = card.title
            avgIntakeText.text = "${stats.averageDailyIntake} ml/day"
            goalAchievementText.text =
                "${stats.goalAchievementDays}/${stats.totalDays} days (${stats.goalAchievementRate}%)"
            bestDayText.text = stats.bestDay

            // Show daily averages
            dailyAveragesLayout.removeAllViews()
            for (avg in stats.dailyAverages) {
                val dayView = TextView(itemView.context).apply {
                    text = "${avg.day}: ${avg.value}ml"
                    textSize = 12f
                    setPadding(8, 4, 8, 4)
                }
                dailyAveragesLayout.addView(dayView)
            }
        }
    }

    private fun setupBottomNavigation() {
        // Home navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this@AnalyticsActivity, MainActivity::class.java))
        }

        // Habits navigation
        findViewById<View>(R.id.nav_habits).setOnClickListener {
            startActivity(Intent(this@AnalyticsActivity, HabitsActivity::class.java))
        }

        // Mood Journal navigation
        findViewById<View>(R.id.nav_mood).setOnClickListener {
            startActivity(Intent(this@AnalyticsActivity, MoodJournalActivity::class.java))
        }

        // Hydration navigation
        findViewById<View>(R.id.nav_hydration).setOnClickListener {
            startActivity(Intent(this@AnalyticsActivity, HydrationActivity::class.java))
        }

        // Settings navigation
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            startActivity(Intent(this@AnalyticsActivity, WidgetSettingsActivity::class.java))
        }
    }
}