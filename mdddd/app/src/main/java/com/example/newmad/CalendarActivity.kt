package com.example.newmad

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var monthTitle: TextView
    private lateinit var prevBtn: ImageView
    private lateinit var nextBtn: ImageView
    private lateinit var adapter: CalendarAdapter
    private lateinit var moodManager: MoodManager

    private var currentYearMonth: YearMonth = YearMonth.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        recycler = findViewById(R.id.calendar_grid)
        monthTitle = findViewById(R.id.tv_month)
        prevBtn = findViewById(R.id.iv_prev_month)
        nextBtn = findViewById(R.id.iv_next_month)

        moodManager = MoodManager(this) // Initialize MoodManager

        recycler.layoutManager = GridLayoutManager(this, 7)
        adapter = CalendarAdapter(mutableListOf()) { day ->
            if (day <= 0) return@CalendarAdapter
            Toast.makeText(this, "Selected: $day ${monthTitle.text}", Toast.LENGTH_SHORT).show()
        }
        recycler.adapter = adapter

        prevBtn.setOnClickListener {
            currentYearMonth = currentYearMonth.minusMonths(1)
            renderMonth()
        }
        nextBtn.setOnClickListener {
            currentYearMonth = currentYearMonth.plusMonths(1)
            renderMonth()
        }

        renderMonth()
        NavigationUtil.setupBottomNav(this, R.id.menu_calendar)
    }

    private fun renderMonth() {
        monthTitle.text = currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        val daysList = buildDays(currentYearMonth)
        val moods = fetchMoodEmojisForMonth(currentYearMonth) //shered prefs
        val today = LocalDate.now()
        val selected = if (today.year == currentYearMonth.year && today.month == currentYearMonth.month) today.dayOfMonth else -1
        adapter.update(daysList, moods, selected)
    }

    private fun buildDays(ym: YearMonth): List<Int> {
        val firstDow = LocalDate.of(ym.year, ym.month, 1).dayOfWeek.value // 1 Mon .. 7 Sun
        val leading = firstDow - 1 // number of blanks before first day
        val daysInMonth = ym.lengthOfMonth()
        val days = mutableListOf<Int>()
        for (i in 0 until leading) days.add(0)
        for (d in 1..daysInMonth) days.add(d)
        while (days.size % 7 != 0) days.add(0)
        return days
    }

    // Use MoodManager to find representative mood emoji for each day of month
    private fun fetchMoodEmojisForMonth(ym: YearMonth): Map<Int, String> {
        return moodManager.getFirstEmojiPerDay(ym.year, ym.monthValue)
    }
}
