package com.example.habitnow

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class MoodJournalActivity : AppCompatActivity() {

    private val entries = mutableListOf<MoodEntry>()
    private lateinit var adapter: MoodAdapter //// RecyclerView for mood list
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var calendarView: RecyclerView
    private lateinit var calendarAdapter: CalendarAdapter
    private var currentMonth = Calendar.getInstance()
    private var selectedDate = Date()
    private lateinit var filterDayCheck: CheckBox

    // Emoji selection interface and adapter
    private fun interface EmojiSelectionListener {
        fun onEmojiSelected(index: Int)
    }

    private class EmojiSelectionAdapter(
        private val emojis: List<MoodEmoji>,
        private val listener: EmojiSelectionListener
    ) : RecyclerView.Adapter<EmojiSelectionAdapter.EmojiViewHolder>() {

        private var selectedPosition = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_mood_emoji, parent, false)
            return EmojiViewHolder(view)
        }

        override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
            val emoji = emojis[position]
            holder.emojiText.text = emoji.emoji
            holder.emojiName.text = emoji.name

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
                listener.onEmojiSelected(selectedPosition)
            }
        }

        override fun getItemCount(): Int = emojis.size

        fun setSelectedPosition(position: Int) {
            selectedPosition = position
            notifyDataSetChanged()
        }

        class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val emojiText: TextView = itemView.findViewById(R.id.text_emoji)
            val emojiName: TextView = itemView.findViewById(R.id.text_emoji_name)
        }
    }

    private data class MoodEmoji(
        val emoji: String,
        val name: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_journal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mood_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("mood_data", MODE_PRIVATE)

        // Mood list
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_moods)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MoodAdapter(entries, object : MoodListener {
            override fun onEdit(position: Int) {
                showMoodDialog(position)
            }

            override fun onDelete(position: Int) {
                entries.removeAt(position)
                adapter.notifyItemRemoved(position)
                calendarAdapter.updateEmojis(mapDateToEmoji())
                saveMoodData()
            }
        })
        recyclerView.adapter = adapter

        // Calendar setup
        calendarView = findViewById(R.id.recycler_calendar)
        setupCalendar()

        filterDayCheck = findViewById(R.id.checkbox_filter_day)
        filterDayCheck.setOnCheckedChangeListener { _, _ -> refreshListForFilter() }

        // FAB for adding moods
        val fab = findViewById<FloatingActionButton>(R.id.fab_log_mood)
        fab.setOnClickListener { showMoodDialog(-1) }

        BaseNavigation.wireTopNav(this, findViewById(R.id.top_nav_include))
        setupBottomNavigation()
    }

    private fun getAvailableMoods(): List<MoodEmoji> {
        return listOf(
            MoodEmoji("😊", "Happy"),
            MoodEmoji("😍", "Love"),
            MoodEmoji("😎", "Cool"),
            MoodEmoji("🤗", "Excited"),
            MoodEmoji("😌", "Calm"),
            MoodEmoji("🙂", "Content"),
            MoodEmoji("😐", "Neutral"),
            MoodEmoji("😔", "Sad"),
            MoodEmoji("😟", "Worried"),
            MoodEmoji("😩", "Frustrated"),
            MoodEmoji("😴", "Tired"),
            MoodEmoji("🤒", "Sick")
        )
    }

    private fun showMoodDialog(editPosition: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mood_entry, null, false)
        val dateText = dialogView.findViewById<TextView>(R.id.text_selected_date)
        val selectedEmojiView = dialogView.findViewById<TextView>(R.id.text_selected_emoji)
        val emojiNameText = dialogView.findViewById<TextView>(R.id.text_emoji_name)
        val emojiRecycler = dialogView.findViewById<RecyclerView>(R.id.recycler_emojis)
        val inputNote = dialogView.findViewById<EditText>(R.id.input_note)

        // Set selected date
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateText.text = sdf.format(selectedDate)

        // Setup emoji selection
        val emojis = getAvailableMoods()
        var selectedEmojiIndex = 0

        val emojiAdapter = EmojiSelectionAdapter(emojis) { index ->
            selectedEmojiIndex = index
            selectedEmojiView.text = emojis[index].emoji
            emojiNameText.text = emojis[index].name
        }

        emojiRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        emojiRecycler.adapter = emojiAdapter

        // Set default emoji or existing emoji for edit
        if (editPosition >= 0) {
            val e = entries[editPosition]
            inputNote.setText(e.note)

            // Find current emoji index
            for (i in emojis.indices) {
                if (emojis[i].emoji == e.emoji) {
                    selectedEmojiIndex = i
                    emojiAdapter.setSelectedPosition(i)
                    break
                }
            }
        }

        // Set initial emoji display
        selectedEmojiView.text = emojis[selectedEmojiIndex].emoji
        emojiNameText.text = emojis[selectedEmojiIndex].name

        AlertDialog.Builder(this)
            .setTitle(if (editPosition >= 0) "Edit Mood" else "Log Mood")
            .setView(dialogView)
            .setPositiveButton(if (editPosition >= 0) "Save" else "Log") { _, _ ->
                val emoji = emojis[selectedEmojiIndex].emoji
                val note = inputNote.text.toString().trim()

                if (editPosition >= 0) {
                    val e = entries[editPosition]
                    e.emoji = emoji
                    e.note = note
                } else {
                    val now = selectedDate
                    entries.add(0, MoodEntry(now, emoji, note))
                }

                refreshListForFilter()
                calendarAdapter.updateEmojis(mapDateToEmoji())
                saveMoodData()
            }
            .setNegativeButton("Cancel", null)
            .show()
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

        calendarAdapter.submit(cells, mapDateToEmoji())
    }

    private fun mapDateToEmoji(): Map<String, String> {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val map = mutableMapOf<String, String>()
        for (e in entries) {
            map[keyFmt.format(e.timestamp)] = e.emoji
        }
        return map
    }

    private fun refreshListForFilter() {
        if (filterDayCheck.isChecked) {
            val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val selKey = keyFmt.format(selectedDate)
            val filtered = mutableListOf<MoodEntry>()
            for (e in entries) {
                if (keyFmt.format(e.timestamp) == selKey) {
                    filtered.add(e)
                }
            }
            adapter.setItems(filtered)
        } else {
            adapter.setItems(ArrayList(entries))
        }
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

    private fun showDateOptionsDialog(date: Date) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val dateStr = sdf.format(date)

        val options = arrayOf("Log Mood for $dateStr", "View Moods for $dateStr")
        AlertDialog.Builder(this)
            .setTitle("Date: $dateStr")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showMoodDialog(-1)
                } else {
                    filterDayCheck.isChecked = true
                    refreshListForFilter()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> refreshListForFilter() }
            .show()
    }

    // ────────── Data classes ──────────

    private data class MoodEntry(
        val timestamp: Date,
        var emoji: String,
        var note: String
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

            fun invalid(dayNumber: Int): DayCell {
                return DayCell(
                    isInvalid = true,
                    isEmpty = false,
                    dayNumber = dayNumber
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

    private fun interface DayClickListener {
        fun onDayClick(dayCell: DayCell)
    }

    private class CalendarAdapter(
        private val cells: MutableList<DayCell>,
        private val listener: DayClickListener
    ) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

        private var dateToEmoji: Map<String, String> = HashMap()

        fun submit(newCells: List<DayCell>, dateToEmoji: Map<String, String>) {
            this.cells.clear()
            this.cells.addAll(newCells)
            this.dateToEmoji = dateToEmoji
            notifyDataSetChanged()
        }

        fun updateEmojis(dateToEmoji: Map<String, String>) {
            this.dateToEmoji = dateToEmoji
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
                holder.day.alpha = 0.3f
                holder.emoji.text = ""
                holder.itemView.setOnClickListener(null)
            } else if (cell.isInvalid) {
                holder.day.text = ""
                holder.day.alpha = 0.4f
                holder.emoji.text = ""
                holder.itemView.setOnClickListener(null)
            } else {
                holder.day.text = cell.dayNumber.toString()
                holder.day.alpha = 1f
                val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val key = keyFmt.format(cell.date)
                holder.emoji.text = dateToEmoji.getOrDefault(key, "")
                holder.itemView.setOnClickListener { listener.onDayClick(cell) }
            }

            // Selected state highlight
            try {
                val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val context = holder.itemView.context as MoodJournalActivity
                val sel = keyFmt.format(context.selectedDate)
                val cur = if (cell.date != null) keyFmt.format(cell.date) else ""
                if (!cell.isEmpty && !cell.isInvalid && sel == cur) {
                    holder.day.setBackgroundResource(R.drawable.bg_day_selected)
                } else {
                    holder.day.setBackgroundResource(0)
                }
            } catch (ignored: Exception) {
            }
        }

        override fun getItemCount(): Int = cells.size

        class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val day: TextView = itemView.findViewById(R.id.text_day)
            val emoji: TextView = itemView.findViewById(R.id.text_day_emoji)
        }
    }

    private interface MoodListener {
        fun onEdit(position: Int)
        fun onDelete(position: Int)
    }

    private class MoodAdapter(
        private var entries: List<MoodEntry>,
        private val listener: MoodListener
    ) : RecyclerView.Adapter<MoodAdapter.MoodViewHolder>() {

        fun setItems(newItems: List<MoodEntry>) {
            this.entries = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mood_entry, parent, false)
            return MoodViewHolder(view)
        }

        override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
            val e = entries[position]
            val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
            val tf = DateFormat.getTimeInstance(DateFormat.SHORT)

            holder.textEmoji.text = e.emoji
            holder.textNote.text = if (e.note.isEmpty()) "" else e.note
            holder.textDate.text = "${df.format(e.timestamp)} ${tf.format(e.timestamp)}"
            holder.buttonEdit.setOnClickListener { listener.onEdit(holder.adapterPosition) }
            holder.buttonDelete.setOnClickListener { listener.onDelete(holder.adapterPosition) }
        }

        override fun getItemCount(): Int = entries.size

        class MoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textEmoji: TextView = itemView.findViewById(R.id.text_emoji)
            val textNote: TextView = itemView.findViewById(R.id.text_note)
            val textDate: TextView = itemView.findViewById(R.id.text_date)
            val buttonEdit: View = itemView.findViewById(R.id.button_edit)
            val buttonDelete: View = itemView.findViewById(R.id.button_delete)
        }
    }

    private fun setupBottomNavigation() {
        // Home navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this@MoodJournalActivity, MainActivity::class.java)
            startActivity(intent)
        }

        // Habits navigation
        findViewById<View>(R.id.nav_habits).setOnClickListener {
            val intent = Intent(this@MoodJournalActivity, HabitsActivity::class.java)
            startActivity(intent)
        }

        // Mood Journal navigation (current page - do nothing)
        findViewById<View>(R.id.nav_mood).setOnClickListener {
            // Already on mood journal page
        }

        // Hydration navigation
        findViewById<View>(R.id.nav_hydration).setOnClickListener {
            val intent = Intent(this@MoodJournalActivity, HydrationActivity::class.java)
            startActivity(intent)
        }

        // Settings navigation
        findViewById<View>(R.id.nav_settings).setOnClickListener {
            val intent = Intent(this@MoodJournalActivity, WidgetSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun saveMoodData() {
        val moodPrefs = getSharedPreferences("mood_data", MODE_PRIVATE)
        val editor = moodPrefs.edit()

        // Clear existing data
        editor.clear()

        // Save each mood entry with date keys
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (entry in entries) {
            val dateKey = sdf.format(entry.timestamp)
            editor.putString("${dateKey}_emoji", entry.emoji)
            editor.putString("${dateKey}_name", getMoodName(entry.emoji))
        }

        editor.apply()
    }

    private fun getMoodName(emoji: String): String {
        return when (emoji) {
            "😊" -> "Happy"
            "😍" -> "Love"
            "😎" -> "Cool"
            "🤗" -> "Excited"
            "😌" -> "Calm"
            "🙂" -> "Content"
            "😐" -> "Neutral"
            "😔" -> "Sad"
            "😟" -> "Worried"
            "😩" -> "Frustrated"
            "😴" -> "Tired"
            "🤒" -> "Sick"
            else -> "Unknown"
        }
    }
}

//onCreate(),Activity Initialization
//Initialize everything
//Sets up UI & event handlers

//The calendar shows the days of the current
// month in a grid (7 columns × 6 rows = 42 cells).

//how RecyclerView shows data
//onCreateViewHolder = "build the empty container"

//onBindViewHolder = "fill the container with the correct
// data for this position"

//Add RecyclerView to your layout
//Create an Adapter and ViewHolder
//Create the layout for each item (item_layout.xml)
//Create Adapter
//Set up RecyclerView in Activity