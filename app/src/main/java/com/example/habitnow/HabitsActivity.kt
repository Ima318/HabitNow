package com.example.habitnow

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class HabitsActivity : AppCompatActivity() {

    private val habits = mutableListOf<Habit>()
    private lateinit var adapter: HabitsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    // Calendar related fields
    private lateinit var calendarView: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private var currentMonth = Calendar.getInstance()
    private var selectedDate = Date()
    private lateinit var filterDayCheck: CheckBox

    private interface IconSelectionListener {
        fun onIconSelected(index: Int)
    }

    private fun setupBottomNavigation() {
        // Home navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this@HabitsActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Habits navigation (current page - do nothing)
        findViewById<View>(R.id.nav_habits).setOnClickListener {
            // Already on habits page
        }

        // Mood Journal navigation
        findViewById<View>(R.id.nav_mood).setOnClickListener {
            val intent = Intent(this@HabitsActivity, MoodJournalActivity::class.java)
            startActivity(intent)
        }

        // Hydration navigation
        findViewById<View>(R.id.nav_hydration).setOnClickListener {
            val intent = Intent(this@HabitsActivity, HydrationActivity::class.java)
            startActivity(intent)
        }

        // Settings navigation
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this@HabitsActivity, WidgetSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private class IconSelectionAdapter(
        private val icons: List<HabitIcon>,
        private val listener: IconSelectionListener
    ) : RecyclerView.Adapter<IconSelectionAdapter.IconViewHolder>() {

        private var selectedPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_habit_icon, parent, false)
            return IconViewHolder(view)
        }

        override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
            val icon = icons[position]
            holder.iconImage.setImageResource(icon.iconResId)
            holder.iconName.text = icon.name

            // Highlight selected item
            if (position == selectedPosition) {
                holder.itemView.setBackgroundColor(0xFFE3F2FD.toInt()) // Light blue
            } else {
                holder.itemView.setBackgroundColor(0xFFFFFFFF.toInt()) // White
            }

            holder.itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                listener.onIconSelected(selectedPosition)
            }
        }

        override fun getItemCount(): Int = icons.size

        fun setSelectedPosition(position: Int) {
            selectedPosition = position
            notifyDataSetChanged()
        }

        class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val iconImage: ImageView = itemView.findViewById(R.id.image_icon)
            val iconName: TextView = itemView.findViewById(R.id.text_icon_name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply stored theme
        ThemeManager.getInstance(this).applyStoredTheme()
        setContentView(R.layout.activity_habits)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.habits_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Habits list
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_habits)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HabitsAdapter(habits, object : HabitListener {
            override fun onHabitToggled() {
                updateProgress()
                updateHabitsData()
                calendarAdapter.updateHabits(mapDateToHabitCount())
            }

            override fun onHabitLongPressed(position: Int) {
                showEditDeleteDialog(position)
            }
        })
        recyclerView.adapter = adapter

        // Calendar setup
        calendarView = findViewById(R.id.recycler_calendar)
        setupCalendar()

        // Filter checkbox
        filterDayCheck = findViewById(R.id.checkbox_filter_day)
        filterDayCheck.setOnCheckedChangeListener { _, _ -> refreshListForFilter() }

        bindMonth()

        // Debug: Print calendar setup
        Log.d("HabitsActivity", "Calendar RecyclerView: $calendarView")
        Log.d("HabitsActivity", "Calendar adapter item count: ${calendarAdapter.itemCount}")

        progressBar = findViewById(R.id.progress_habits)
        progressText = findViewById(R.id.text_progress)

        val fab = findViewById<FloatingActionButton>(R.id.fab_add_habit)
        fab.setOnClickListener {
            showAddHabitDialog()
        }

        seedSampleData()
        refreshListForFilter() // Ensure habits are displayed
        updateProgress()

        // Navigation setup
        BaseNavigation.wireTopNav(this, findViewById(R.id.top_nav_include))
        setupBottomNavigation()
    }

    private fun setupCalendar() {
        calendarView.layoutManager = GridLayoutManager(this, 7)
        calendarAdapter = CalendarAdapter(mutableListOf()) { dayCell ->
            selectedDate = dayCell.date
            showDateOptionsDialog(dayCell.date)
        }
        calendarView.adapter = calendarAdapter
        bindMonth()
    }

    private fun bindMonth() {
        val monthTitle = findViewById<TextView>(R.id.text_month)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthTitle.text = sdf.format(currentMonth.time)

        val cal = currentMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        // Get the day of week for the first day (Sunday = 1, Monday = 2, etc.)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<DayCell>()

        // Add empty cells for days before the first day of the month
        for (i in 1 until firstDayOfWeek) {
            cells.add(DayCell.empty())
        }

        // Add all days of the month
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            cells.add(DayCell.of(cal.time))
        }

        // Add empty cells to fill the grid (6 rows × 7 columns = 42 cells total)
        while (cells.size < 42) {
            cells.add(DayCell.empty())
        }

        // Debug: Log the number of cells created
        Log.d("HabitsActivity", "Created ${cells.size} calendar cells for $daysInMonth days")

        calendarAdapter.submit(cells, mapDateToHabitCount())
    }

    private fun mapDateToHabitCount(): Map<String, String> {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = mutableMapOf<String, String>()

        // Group habits by date and count completed ones
        val totalHabits = mutableMapOf<String, Int>()
        val completedHabits = mutableMapOf<String, Int>()

        for (habit in habits) {
            val dateKey = keyFmt.format(habit.date)
            totalHabits[dateKey] = totalHabits.getOrDefault(dateKey, 0) + 1
            if (habit.completed) {
                completedHabits[dateKey] = completedHabits.getOrDefault(dateKey, 0) + 1
            }
        }

        // Create display text showing completion ratio
        for (dateKey in totalHabits.keys) {
            val total = totalHabits[dateKey] ?: 0
            val completed = completedHabits.getOrDefault(dateKey, 0)
            if (total > 0) {
                map[dateKey] = "$completed/$total"
            }
        }

        return map
    }

    private fun refreshListForFilter() {
        Log.d("HabitsActivity", "refreshListForFilter called, total habits: ${habits.size}")

        if (filterDayCheck.isChecked) {
            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selKey = keyFmt.format(selectedDate)
            Log.d("HabitsActivity", "Filtering for selected day: $selKey")

            val filtered = mutableListOf<Habit>()
            for (habit in habits) {
                val habitDateKey = keyFmt.format(habit.date)
                Log.d("HabitsActivity", "Habit '${habit.name}' date: $habitDateKey")
                if (habitDateKey == selKey) {
                    filtered.add(habit)
                }
            }
            Log.d("HabitsActivity", "Filtered habits count: ${filtered.size}")
            adapter.setItems(filtered)
        } else {
            Log.d("HabitsActivity", "Showing all habits: ${habits.size}")
            adapter.setItems(ArrayList(habits))
        }
        updateProgress()
    }

    private fun seedSampleData() {
        val today = Date()
        habits.add(Habit("Drink water", R.drawable.ic_water, today))
        habits.add(Habit("Meditate", R.drawable.ic_meditation, today))
        habits.add(Habit("Exercise", R.drawable.ic_exercise, today))
        adapter.notifyDataSetChanged()
    }

    private fun updateProgress() {
        val currentHabits = adapter.getCurrentItems()
        val total = currentHabits.size
        var completed = 0
        for (h in currentHabits) {
            if (h.completed) completed++
        }
        val percent = if (total == 0) 0 else ((completed * 100f) / total).toInt()
        progressBar.max = 100
        progressBar.progress = percent
        progressText.text = "$completed/$total done today"

        getSharedPreferences(HabitProgressWidgetProvider.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(HabitProgressWidgetProvider.KEY_COMPLETED, completed)
            .putInt(HabitProgressWidgetProvider.KEY_TOTAL, total)
            .apply()
        HabitProgressWidgetProvider.requestUpdateAll(this)
    }

    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_habit, null, false)
        val input = dialogView.findViewById<EditText>(R.id.input_habit_name)
        val dateText = dialogView.findViewById<TextView>(R.id.text_selected_date)
        val selectedIconView = dialogView.findViewById<ImageView>(R.id.image_selected_icon)
        val iconNameText = dialogView.findViewById<TextView>(R.id.text_icon_name)
        val iconRecycler = dialogView.findViewById<RecyclerView>(R.id.recycler_icons)

        // Set selected date
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateText.text = sdf.format(selectedDate)

        // Setup icon selection
        val icons = getAvailableIcons()
        var selectedIconIndex = 0 // Use var instead of final array

        val iconAdapter = IconSelectionAdapter(icons, object : IconSelectionListener {
            override fun onIconSelected(index: Int) {
                selectedIconIndex = index
                selectedIconView.setImageResource(icons[index].iconResId)
                iconNameText.text = icons[index].name
            }
        })

        iconRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        iconRecycler.adapter = iconAdapter

        // Set default icon
        selectedIconView.setImageResource(icons[0].iconResId)
        iconNameText.text = icons[0].name

        AlertDialog.Builder(this)
            .setTitle("Add Habit")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    habits.add(Habit(name, icons[selectedIconIndex].iconResId, selectedDate))
                    refreshListForFilter()
                    updateHabitsData()
                    calendarAdapter.updateHabits(mapDateToHabitCount())
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDeleteDialog(position: Int) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                if (which == 0) {
                    showEditHabitDialog(position)
                } else {
                    val currentHabits = adapter.getCurrentItems()
                    if (position < currentHabits.size) {
                        val habitToRemove = currentHabits[position]
                        habits.remove(habitToRemove)
                        refreshListForFilter()
                        updateHabitsData()
                        calendarAdapter.updateHabits(mapDateToHabitCount())
                    }
                }
            }
            .show()
    }

    private fun showEditHabitDialog(position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_habit, null, false)
        val input = dialogView.findViewById<EditText>(R.id.input_habit_name)
        val dateText = dialogView.findViewById<TextView>(R.id.text_selected_date)
        val selectedIconView = dialogView.findViewById<ImageView>(R.id.image_selected_icon)
        val iconNameText = dialogView.findViewById<TextView>(R.id.text_icon_name)
        val iconRecycler = dialogView.findViewById<RecyclerView>(R.id.recycler_icons)

        val icons = getAvailableIcons()
        val currentHabits = adapter.getCurrentItems()

        if (position < currentHabits.size) {
            val habit = currentHabits[position]
            input.setText(habit.name)

            // Set date
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateText.text = sdf.format(habit.date)

            // Find current icon index
            var selectedIconIndex = 0
            for (i in icons.indices) {
                if (icons[i].iconResId == habit.iconResId) {
                    selectedIconIndex = i
                    break
                }
            }

            // Setup icon selection
            val iconAdapter = IconSelectionAdapter(icons, object : IconSelectionListener {
                override fun onIconSelected(index: Int) {
                    selectedIconIndex = index
                    selectedIconView.setImageResource(icons[index].iconResId)
                    iconNameText.text = icons[index].name
                }
            })

            iconRecycler.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            iconRecycler.adapter = iconAdapter
            iconAdapter.setSelectedPosition(selectedIconIndex)

            // Set current icon
            selectedIconView.setImageResource(icons[selectedIconIndex].iconResId)
            iconNameText.text = icons[selectedIconIndex].name

            AlertDialog.Builder(this)
                .setTitle("Edit Habit")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val name = input.text.toString().trim()
                    if (name.isNotEmpty()) {
                        habit.name = name
                        habit.iconResId = icons[selectedIconIndex].iconResId
                        adapter.notifyItemChanged(position)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun getAvailableIcons(): List<HabitIcon> {
        return listOf(
            HabitIcon(R.drawable.ic_water, "Water"),
            HabitIcon(R.drawable.ic_exercise, "Exercise"),
            HabitIcon(R.drawable.ic_meditation, "Meditation"),
            HabitIcon(R.drawable.ic_reading, "Reading"),
            HabitIcon(R.drawable.ic_sleep, "Sleep"),
            HabitIcon(R.drawable.ic_food, "Healthy Food"),
            HabitIcon(R.drawable.watern, "Hydration"),
            HabitIcon(R.drawable.habitn, "General Habit"),
            HabitIcon(R.drawable.moodn, "Mood"),
            HabitIcon(R.drawable.settings, "Settings")
        )
    }

    private fun showDateOptionsDialog(date: Date) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val options = arrayOf(
            "Add Habit for ${sdf.format(date)}",
            "View Habits for ${sdf.format(date)}"
        )
        AlertDialog.Builder(this)
            .setTitle("Date: ${sdf.format(date)}")
            .setItems(options) { _, which ->
                if (which == 0) {
                    // Add habit for this date
                    selectedDate = date
                    showAddHabitDialog()
                } else {
                    // Filter to show habits for this date
                    filterDayCheck.isChecked = true
                    selectedDate = date
                    refreshListForFilter()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> refreshListForFilter() }
            .show()
    }

    // ────────── Data classes ──────────

    private data class HabitIcon(
        val iconResId: Int,
        val name: String
    )

    private data class Habit(
        var name: String,
        var iconResId: Int,
        val date: Date,
        var completed: Boolean = false
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
                    isInvalid = true,
                    isEmpty = true,
                    dayNumber = 0
                )
            }

            fun invalid(dayNumber: Int): DayCell {
                return DayCell(
                    isInvalid = true,
                    isEmpty = false,
                    dayNumber = dayNumber
                )
            }
        }
    }

    private fun interface DayClickListener {
        fun onDayClick(dayCell: DayCell)
    }

    private class CalendarAdapter(
        private val cells: MutableList<DayCell>,
        private val listener: DayClickListener
    ) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

        private var dateToHabitCount: Map<String, String> = HashMap()

        fun submit(newCells: List<DayCell>, dateToHabitCount: Map<String, String>) {
            this.cells.clear()
            this.cells.addAll(newCells)
            this.dateToHabitCount = dateToHabitCount
            notifyDataSetChanged()
        }

        fun updateHabits(dateToHabitCount: Map<String, String>) {
            this.dateToHabitCount = dateToHabitCount
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
            return DayViewHolder(v)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val cell = cells[position]

            // Always set some text to make sure TextView is working
            if (cell.isEmpty) {
                holder.day.text = ""
                holder.emoji.text = ""
                holder.day.alpha = 0.3f
                holder.itemView.setOnClickListener(null)
            } else {
                // Show the day number - this should always work
                holder.day.text = cell.dayNumber.toString()
                holder.day.alpha = 1.0f

                // Add habit count if available
                if (!cell.isInvalid) {
                    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val key = keyFmt.format(cell.date)
                    holder.emoji.text = dateToHabitCount.getOrDefault(key, "")

                    // Make it clickable
                    holder.itemView.setOnClickListener {
                        Log.d("CalendarAdapter", "Day clicked: ${cell.dayNumber}")
                        listener.onDayClick(cell)
                    }

                    // Highlight if selected
                    try {
                        val context = holder.itemView.context as HabitsActivity
                        val sel = keyFmt.format(context.selectedDate)
                        val cur = keyFmt.format(cell.date)
                        if (sel == cur) {
                            holder.itemView.setBackgroundColor(0xFFE0E0E0.toInt()) // Light gray background
                        } else {
                            holder.itemView.setBackgroundColor(0xFFFFFFFF.toInt()) // White background
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

            // Debug log
            Log.d("CalendarAdapter", "Set day $position to: '${holder.day.text}'")
        }

        override fun getItemCount(): Int = cells.size

        class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val day: TextView = itemView.findViewById(R.id.text_day)
            val emoji: TextView = itemView.findViewById(R.id.text_day_emoji)
        }
    }

    private interface HabitListener {
        fun onHabitToggled()
        fun onHabitLongPressed(position: Int)
    }

    private class HabitsAdapter(
        private var habits: List<Habit>,
        private val listener: HabitListener
    ) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

        fun setItems(newItems: List<Habit>) {
            this.habits = newItems
            notifyDataSetChanged()
        }

        fun getCurrentItems(): List<Habit> = habits

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
            return HabitViewHolder(view)
        }

        override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
            val habit = habits[position]
            Log.d("HabitsAdapter", "Binding habit $position: ${habit.name}")
            holder.name.text = habit.name
            holder.icon.setImageResource(habit.iconResId)
            holder.check.isChecked = habit.completed
            holder.check.setOnCheckedChangeListener { _, isChecked ->
                habit.completed = isChecked
                listener.onHabitToggled()
            }
            holder.itemView.setOnLongClickListener {
                listener.onHabitLongPressed(holder.adapterPosition)
                true
            }
        }

        override fun getItemCount(): Int {
            Log.d("HabitsAdapter", "getItemCount: ${habits.size}")
            return habits.size
        }

        class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.text_habit_name)
            val icon: ImageView = itemView.findViewById(R.id.image_habit_icon)
            val check: CheckBox = itemView.findViewById(R.id.checkbox_habit)
        }
    }

    private fun updateHabitsData() {
        val habitsPrefs = getSharedPreferences("habits_data", MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayKey = dateFormat.format(Date())

        val currentHabits = adapter.getCurrentItems()
        val totalHabits = currentHabits.size
        var completedHabits = 0

        for (habit in currentHabits) {
            if (habit.completed) {
                completedHabits++
            }
        }

        val editor = habitsPrefs.edit()
        editor.putInt("${todayKey}_total", totalHabits)
        editor.putInt("${todayKey}_completed", completedHabits)
        editor.apply()

        // Also update widget
        getSharedPreferences(HabitProgressWidgetProvider.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(HabitProgressWidgetProvider.KEY_COMPLETED, completedHabits)
            .putInt(HabitProgressWidgetProvider.KEY_TOTAL, totalHabits)
            .apply()
        HabitProgressWidgetProvider.requestUpdateAll(this)
    }
}