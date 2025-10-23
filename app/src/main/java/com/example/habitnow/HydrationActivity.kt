package com.example.habitnow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.java

class HydrationActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var intervalSpinner: Spinner
    private lateinit var dailyGoalSpinner: Spinner
    private lateinit var notificationsSwitch: Switch
    private lateinit var progressBar: ProgressBar
    private lateinit var intakeAmountText: TextView
    private lateinit var percentageText: TextView
    private lateinit var btn250ml: Button
    private lateinit var btn500ml: Button
    private lateinit var btn750ml: Button
    private lateinit var btn1000ml: Button
    private lateinit var btnAddCustom: Button
    private lateinit var btnSuggest100: Button
    private lateinit var btnSuggest200: Button
    private lateinit var btnSuggest300: Button
    private lateinit var btnSuggest600: Button
    private lateinit var editCustomVolume: EditText

    // Calendar
    private lateinit var calendarView: RecyclerView
    private lateinit var intakeHistoryRecycler: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var historyAdapter: IntakeHistoryAdapter
    private var currentMonth = Calendar.getInstance()
    private var selectedDate = Date()

    // Data
    private var dailyGoalMl = 2000
    private val dailyIntakes = mutableMapOf<String, MutableList<WaterIntake>>()
    private var todayIntakes = mutableListOf<WaterIntake>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hydration)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hydration_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//Calls helper setup functions:
        initializeViews()
        setupSpinners()
        setupVolumeButtons()
        setupCalendar()
        setupIntakeHistory()
        loadTodayData()
        updateProgress()
//Notification test button
        val testNotificationButton = findViewById<Button>(R.id.btn_test_notification)
        testNotificationButton.setOnClickListener {
            val notificationHelper = NotificationHelper(this)
            notificationHelper.createNotificationChannel()
            notificationHelper.sendHydrationTestNotification()
        }

        // Set up notification switch listener
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val notificationHelper = NotificationHelper(this)
                notificationHelper.createNotificationChannel()
                Toast.makeText(this, "Hydration notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Hydration notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        BaseNavigation.wireTopNav(this, findViewById(R.id.top_nav_include))
        setupBottomNavigation()
    }

    //Links all buttons, text views, progress bar, etc. from XML to code variables.
    private fun initializeViews() {
        intervalSpinner = findViewById(R.id.spinner_interval)
        dailyGoalSpinner = findViewById(R.id.spinner_daily_goal)
        notificationsSwitch = findViewById(R.id.switch_notifications)
        progressBar = findViewById(R.id.progress_hydration)
        intakeAmountText = findViewById(R.id.text_intake_amount)
        percentageText = findViewById(R.id.text_percentage)

        btn250ml = findViewById(R.id.btn_250ml)
        btn500ml = findViewById(R.id.btn_500ml)
        btn750ml = findViewById(R.id.btn_750ml)
        btn1000ml = findViewById(R.id.btn_1000ml)
        btnSuggest100 = findViewById(R.id.btn_suggest_100)
        btnSuggest200 = findViewById(R.id.btn_suggest_200)
        btnSuggest300 = findViewById(R.id.btn_suggest_300)
        btnSuggest600 = findViewById(R.id.btn_suggest_600)
        btnAddCustom = findViewById(R.id.btn_add_custom)
        editCustomVolume = findViewById(R.id.edit_custom_volume)

        calendarView = findViewById(R.id.recycler_calendar)
        intakeHistoryRecycler = findViewById(R.id.recycler_intake_history)
    }

    private fun setupSpinners() {
        // Daily goal spinner
        val goals = arrayOf("1500 ml", "2000 ml", "2500 ml", "3000 ml", "3500 ml")
        val goalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, goals)
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dailyGoalSpinner.adapter = goalAdapter
        dailyGoalSpinner.setSelection(1) // Default to 2000ml

        dailyGoalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                dailyGoalMl = when (position) {
                    0 -> 1500
                    1 -> 2000
                    2 -> 2500
                    3 -> 3000
                    4 -> 3500
                    else -> 2000
                }
                updateProgress()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Interval spinner
        val intervals = arrayOf("30 minutes", "1 hour", "2 hours", "3 hours")
        val intervalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        intervalSpinner.adapter = intervalAdapter
    }

    private fun setupVolumeButtons() {
        btn250ml.setOnClickListener { addWaterIntake(250) }
        btn500ml.setOnClickListener { addWaterIntake(500) }
        btn750ml.setOnClickListener { addWaterIntake(750) }
        btn1000ml.setOnClickListener { addWaterIntake(1000) }
        btnSuggest100.setOnClickListener { addWaterIntake(100) }
        btnSuggest200.setOnClickListener { addWaterIntake(200) }
        btnSuggest300.setOnClickListener { addWaterIntake(300) }
        btnSuggest600.setOnClickListener { addWaterIntake(600) }

        btnAddCustom.setOnClickListener {
            val volumeText = editCustomVolume.text.toString().trim()

            if (volumeText.isEmpty()) {
                editCustomVolume.error = "Please enter a volume"
                editCustomVolume.requestFocus()
                return@setOnClickListener
            }

            try {
                val customVolume = volumeText.toInt()

                if (customVolume <= 0) {
                    editCustomVolume.error = "Volume must be greater than 0"
                    editCustomVolume.requestFocus()
                    return@setOnClickListener
                }

                if (customVolume > 10000) {
                    editCustomVolume.error = "Volume too large (max 10000ml)"
                    editCustomVolume.requestFocus()
                    return@setOnClickListener
                }

                addWaterIntake(customVolume)
                editCustomVolume.setText("")
                editCustomVolume.clearFocus()

                // Show success feedback
                Toast.makeText(
                    this,
                    "Added ${customVolume}ml to your daily intake!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: NumberFormatException) {
                editCustomVolume.error = "Please enter a valid number"
                editCustomVolume.requestFocus()
            }
        }
    }

    private fun setupCalendar() {
        calendarView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(mutableListOf()) { dayCell ->
            selectedDate = dayCell.date
            loadSelectedDateData()
        }
        calendarView.adapter = calendarAdapter

        bindMonth()
    }

    private fun setupIntakeHistory() {
        historyAdapter = IntakeHistoryAdapter(todayIntakes) { position ->
            // Delete intake
            if (position < todayIntakes.size) {
                todayIntakes.removeAt(position)
                saveTodayData()
                historyAdapter.notifyItemRemoved(position)
                updateProgress()
                calendarAdapter.updateIntakes(mapDateToIntake())
            }
        }
        intakeHistoryRecycler.layoutManager = LinearLayoutManager(this)
        intakeHistoryRecycler.adapter = historyAdapter
    }

    private fun addWaterIntake(volumeMl: Int) {
        val intake = WaterIntake(volumeMl, Date())
        todayIntakes.add(0, intake)//add this new drink to today's list
        saveTodayData()//save updated intake in SharedPreferences
        historyAdapter.notifyItemInserted(0)//update the RecyclerView history list
        updateProgress()//update the progress bar and percentage
        calendarAdapter.updateIntakes(mapDateToIntake())// update calendar view
    }

    private fun bindMonth() {
        val monthTitle = findViewById<TextView>(R.id.text_month)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthTitle.text = sdf.format(currentMonth.time)

        val cal = currentMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<DayCell>()

        // Add empty cells for proper alignment
        for (i in 1 until firstDayOfWeek) {
            cells.add(DayCell.empty())
        }

        // Add all days of the month
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cells.add(DayCell.of(cal.time))
        }

        // Fill to 42 cells (6 rows × 7 columns)
        while (cells.size < 42) {
            cells.add(DayCell.empty())
        }

        calendarAdapter.submit(cells, mapDateToIntake())
    }

    private fun mapDateToIntake(): Map<String, String> {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = mutableMapOf<String, String>()

        for ((dateKey, intakes) in dailyIntakes) {
            if (intakes.isNotEmpty()) {
                val totalMl = intakes.sumOf { it.volumeMl }
                val percentage = ((totalMl * 100f) / dailyGoalMl).toInt()
                map[dateKey] = "$percentage%"
            }
        }

        return map
    }

    private fun loadTodayData() {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = keyFmt.format(Date())

        todayIntakes = dailyIntakes[todayKey] ?: mutableListOf()
        if (!dailyIntakes.containsKey(todayKey)) {
            dailyIntakes[todayKey] = todayIntakes
        }

        historyAdapter.updateItems(todayIntakes)
    }

    //Updates UI when you click a day on the calendar:
    private fun loadSelectedDateData() {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedKey = keyFmt.format(selectedDate)

        val selectedIntakes = dailyIntakes[selectedKey] ?: mutableListOf()
        historyAdapter.updateItems(selectedIntakes)

        // Update progress for selected date
        val totalMl = selectedIntakes.sumOf { it.volumeMl }

        val percentage = ((totalMl * 100f) / dailyGoalMl).toInt()
        intakeAmountText.text = "$totalMl ml / $dailyGoalMl ml"
        percentageText.text = "$percentage%"
        progressBar.progress = percentage
    }

    private fun saveTodayData() {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = keyFmt.format(Date())
        dailyIntakes[todayKey] = ArrayList(todayIntakes)

        // Calculate total intake for today
        val totalIntake = todayIntakes.sumOf { it.volumeMl }

        // Save to SharedPreferences for real-time updates
        val hydrationPrefs = getSharedPreferences("hydration_data", MODE_PRIVATE)
        val editor = hydrationPrefs.edit()
        editor.putInt("${todayKey}_intake", totalIntake)
        editor.putInt("daily_goal", dailyGoalMl)
        editor.apply()
    }

    //Shows percentage of your goal reached.
    private fun updateProgress() {
        val totalMl = todayIntakes.sumOf { it.volumeMl }

        val previousPercentage = progressBar.progress
        val currentPercentage = minOf(100, ((totalMl * 100f) / dailyGoalMl).toInt())

        intakeAmountText.text = "$totalMl ml / $dailyGoalMl ml"
        percentageText.text = "$currentPercentage%"
        progressBar.progress = currentPercentage

        // Check if goal was just reached
        if (previousPercentage < 100 && currentPercentage >= 100 && notificationsSwitch.isChecked) {
            val notificationHelper = NotificationHelper(this)
            notificationHelper.createNotificationChannel()
            notificationHelper.sendGoalReachedNotification()
        }
    }

    // Data Classes
    private data class WaterIntake(
        val volumeMl: Int,
        val timestamp: Date
    )

    private data class DayCell(
        val isInvalid: Boolean = false,
        val isEmpty: Boolean = false,
        val date: Date = Date(),
        val dayNumber: Int = 0
    ) {
        companion object {
            fun of(date: Date): DayCell {
                val c = Calendar.getInstance()
                c.time = date
                return DayCell(
                    isInvalid = false,
                    isEmpty = false,
                    date = date,
                    dayNumber = c.get(Calendar.DAY_OF_MONTH)
                )
            }

            fun empty(): DayCell {
                return DayCell(
                    isInvalid = false,
                    isEmpty = true,
                    dayNumber = 0
                )
            }
        }
    }

    // Adapters
    private fun interface DayClickListener {
        fun onDayClick(dayCell: DayCell)
    }

    private class CalendarAdapter(
        private val cells: MutableList<DayCell>,
        private val listener: DayClickListener
    ) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

        private var dateToIntake: Map<String, String> = HashMap()

        fun submit(newCells: List<DayCell>, dateToIntake: Map<String, String>) {
            this.cells.clear()
            this.cells.addAll(newCells)
            this.dateToIntake = dateToIntake
            notifyDataSetChanged()
        }

        fun updateIntakes(dateToIntake: Map<String, String>) {
            this.dateToIntake = dateToIntake
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(v)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val cell = cells[position]

            if (cell.isEmpty) {
                holder.day.text = ""
                holder.emoji.text = ""
                holder.day.alpha = 0.3f
                holder.itemView.setOnClickListener(null)
            } else {
                holder.day.text = cell.dayNumber.toString()
                holder.day.alpha = 1f

                if (!cell.isInvalid) {
                    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val key = keyFmt.format(cell.date)
                    holder.emoji.text = dateToIntake.getOrDefault(key, "")
                    holder.itemView.setOnClickListener { listener.onDayClick(cell) }

                    // Highlight selected day
                    try {
                        val context = holder.itemView.context as HydrationActivity
                        val sel = keyFmt.format(context.selectedDate)
                        if (sel == key) {
                            holder.itemView.setBackgroundColor(0xFFE3F2FD.toInt())
                        } else {
                            holder.itemView.setBackgroundColor(0xFFFFFFFF.toInt())
                        }
                    } catch (e: Exception) {
                        holder.itemView.setBackgroundColor(0xFFFFFFFF.toInt())
                    }
                } else {
                    holder.emoji.text = ""
                    holder.itemView.setOnClickListener(null)
                    holder.itemView.setBackgroundColor(0xFFFFFFFF.toInt())
                }
            }
        }

        override fun getItemCount(): Int = cells.size

        class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val day: TextView = itemView.findViewById(R.id.text_day)
            val emoji: TextView = itemView.findViewById(R.id.text_day_emoji)
        }
    }

    private fun interface IntakeDeleteListener {
        fun onDeleteIntake(position: Int)
    }

    private class IntakeHistoryAdapter(
        private var intakes: List<WaterIntake>,
        private val listener: IntakeDeleteListener
    ) : RecyclerView.Adapter<IntakeHistoryAdapter.IntakeViewHolder>() {

        fun updateItems(newIntakes: List<WaterIntake>) {
            this.intakes = newIntakes
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntakeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_water_intake, parent, false)
            return IntakeViewHolder(view)
        }

        override fun onBindViewHolder(holder: IntakeViewHolder, position: Int) {
            val intake = intakes[position]
            holder.volumeText.text = "${intake.volumeMl} ml"

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            holder.timeText.text = timeFormat.format(intake.timestamp)

            holder.deleteButton.setOnClickListener { listener.onDeleteIntake(holder.adapterPosition) }
        }

        override fun getItemCount(): Int = intakes.size

        class IntakeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val volumeText: TextView = itemView.findViewById(R.id.text_volume)
            val timeText: TextView = itemView.findViewById(R.id.text_time)
            val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete)
        }
    }

    private fun setupBottomNavigation() {
        // Home navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this@HydrationActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Habits navigation
        findViewById<View>(R.id.nav_habits).setOnClickListener {
            val intent = Intent(this@HydrationActivity, HabitsActivity::class.java)
            startActivity(intent)
        }

        // Mood Journal navigation  
        findViewById<View>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this@HydrationActivity, MoodJournalActivity::class.java)
            startActivity(intent)
        }

        // Hydration navigation (current page - do nothing)
        findViewById<View>(R.id.nav_hydration).setOnClickListener {
            // Already on hydration page
        }

        // Settings navigation
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this@HydrationActivity, WidgetSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    inner class NotificationHelper(private val context: Context) {
        private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "hydration_channel",
                    "Hydration Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Hydration notifications"
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun sendHydrationTestNotification() {
            val intent = Intent(context, HydrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, "hydration_channel")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("💧 Hydration Reminder")
                .setContentText("Time to drink some water! Stay hydrated for better health.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            try {
                notificationManager.notify(1, builder.build())
                Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Please enable notifications in app settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        fun sendGoalReachedNotification() {
            sendNotification(
                " Daily Goal Reached!",
                "Congratulations! You've reached your daily hydration goal!"
            )
        }

        private fun sendNotification(title: String, message: String) {
            val intent = Intent(context, HydrationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val builder = NotificationCompat.Builder(context, "hydration_channel")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            try {
                notificationManager.notify(1, builder.build())
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Please enable notifications in app settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}