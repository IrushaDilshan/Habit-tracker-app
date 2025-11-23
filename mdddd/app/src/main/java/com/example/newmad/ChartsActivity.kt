package com.example.newmad

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChartsActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var barChart: BarChart
    private lateinit var tvMoodPeriod: TextView
    private lateinit var tvHabitPeriod: TextView
    private lateinit var moodCard: LinearLayout
    private lateinit var habitCard: LinearLayout
    private lateinit var groupPeriod: MaterialButtonToggleGroup
    private lateinit var groupSegment: MaterialButtonToggleGroup
    private lateinit var moodManager: MoodManager
    private lateinit var habitHistoryManager: HabitHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        NavigationUtil.setupBottomNav(this, R.id.menu_charts)

        // Managers for real data
        moodManager = MoodManager(this)
        habitHistoryManager = HabitHistoryManager(this)

        // Top bar buttons
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_share).setOnClickListener { /* TODO share */ }
        findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener { refreshCurrent() }

        // Views
        lineChart = findViewById(R.id.line_chart)
        barChart = findViewById(R.id.bar_chart)
        tvMoodPeriod = findViewById(R.id.tv_mood_period)
        tvHabitPeriod = findViewById(R.id.tv_habit_period)
        moodCard = findViewById(R.id.mood_card)
        habitCard = findViewById(R.id.habit_card)
        groupPeriod = findViewById(R.id.group_period)
        groupSegment = findViewById(R.id.group_segment)

        initCharts()

        // Default selection already set in XML (weekly & mood)
        applyPeriod("weekly")
        showSegment(isMood = true)

        groupPeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_period_weekly -> applyPeriod("weekly")
                R.id.btn_period_monthly -> applyPeriod("monthly")
            }
        }

        groupSegment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_segment_mood -> showSegment(isMood = true)
                R.id.btn_segment_habit -> showSegment(isMood = false)
            }
        }
    }

    private fun refreshCurrent() {
        val period = currentPeriod()
        applyPeriod(period)
    }

    private fun currentPeriod(): String =
        when (groupPeriod.checkedButtonId) {
            R.id.btn_period_monthly -> "monthly"
            else -> "weekly"
        }

    private fun showSegment(isMood: Boolean) {
        moodCard.visibility = if (isMood) View.VISIBLE else View.GONE
        habitCard.visibility = if (isMood) View.GONE else View.VISIBLE
    }

    private fun applyPeriod(period: String) {
        tvMoodPeriod.text = period.capitalizeFirst()
        tvHabitPeriod.text = period.capitalizeFirst()
        val (moodData, habitData, labels) = buildDataAndLabels(period)
        displayMoodChart(lineChart, moodData, period, labels)
        displayHabitChart(barChart, habitData, period, labels)
        updateMoodSummaries(period)
        updateHabitSummaries(period)
    }

    // Build data lists for charts + labels (oldest -> newest)
    private fun buildDataAndLabels(period: String): Triple<List<Entry>, List<BarEntry>, List<String>> {
        val mood = fetchMoodAnalytics(period)
        val habit = fetchHabitAnalytics(period)
        val labels = buildLabels(period)
        return Triple(mood, habit, labels)
    }

    private fun buildLabels(period: String): List<String> {
        return if (period == "weekly") {
            val today = java.time.LocalDate.now()
            val days = (6 downTo 0).map { today.minusDays(it.toLong()) } // oldest first
            val fmt = java.time.format.DateTimeFormatter.ofPattern("E")
            days.map { d -> fmt.format(d).take(3) }
        } else {
            (1..30).map { it.toString() }
        }
    }

    private fun fetchMoodAnalytics(period: String): List<Entry> {
        val points = if (period == "weekly") 7 else 30
        val today = java.time.LocalDate.now()
        val entries = ArrayList<Entry>(points)
        for (i in 0 until points) {
            val daysAgo = (points - 1 - i).toLong() // index 0 oldest
            val day = today.minusDays(daysAgo)
            val avg = moodManager.getAverageMoodScoreForDate(day) // 0..5
            entries.add(Entry(i.toFloat(), avg))
        }
        return entries
    }

    private fun fetchHabitAnalytics(period: String): List<BarEntry> {
        val points = if (period == "weekly") 7 else 30
        val lastDays = habitHistoryManager.getLastNDaysPercents(points) // oldest first
        return lastDays.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }
    }

    // Mood summary population
    private fun updateMoodSummaries(period: String) {
        val mostFreqEmojiView = findViewById<TextView>(R.id.tv_most_freq_emoji)
        val mostFreqLabelView = findViewById<TextView>(R.id.tv_most_freq_label)
        val mostFreqCountView = findViewById<TextView>(R.id.tv_most_freq_count)
        val streakView = findViewById<TextView>(R.id.tv_positive_streak)
        val avgEmojiView = findViewById<TextView>(R.id.tv_weekly_avg_emoji)
        val avgLabelView = findViewById<TextView>(R.id.tv_weekly_avg_label)
        val avgScoreView = findViewById<TextView>(R.id.tv_weekly_avg_score)

        val days = if (period == "weekly") 7 else 30
        val today = java.time.LocalDate.now()

        // Count moods
        data class MoodCount(var count: Int, val emoji: String)
        val counts = mutableMapOf<String, MoodCount>()
        var totalScore = 0f
        var scoreDays = 0
        for (i in 0 until days) {
            val day = today.minusDays(i.toLong())
            val entries = moodManager.getMoodEntriesForDate(day)
            if (entries.isNotEmpty()) {
                // accumulate per-entry counts
                for (e in entries) {
                    val mc = counts[e.label]
                    if (mc == null) counts[e.label] = MoodCount(1, e.emoji) else mc.count += 1
                }
            }
            val dailyAvg = moodManager.getAverageMoodScoreForDate(day)
            if (dailyAvg > 0f) {
                totalScore += dailyAvg
                scoreDays++
            }
        }

        if (counts.isNotEmpty()) {
            val (label, mc) = counts.maxByOrNull { it.value.count }!!
            mostFreqEmojiView.text = mc.emoji
            mostFreqLabelView.text = label
            mostFreqCountView.text = "${mc.count} times"
        } else {
            mostFreqEmojiView.text = "–"
            mostFreqLabelView.text = "No Data"
            mostFreqCountView.text = "0"
        }

        // Positive streak: consecutive recent days (starting today backwards) whose average mood >= 3.0
        val threshold = 3.0f
        var streak = 0
        run {
            for (i in 0 until days) {
                val day = today.minusDays(i.toLong())
                val avg = moodManager.getAverageMoodScoreForDate(day)
                if (avg >= threshold && avg > 0f) streak++ else break
            }
        }
        streakView.text = if (streak > 0) "$streak Day${if (streak == 1) "" else "s"}" else "0"

        val overallAvg = if (scoreDays == 0) 0f else totalScore / scoreDays
        val (descLabel, descEmoji) = moodDescriptor(overallAvg)
        avgEmojiView.text = descEmoji
        avgLabelView.text = descLabel
        avgScoreView.text = String.format(Locale.getDefault(), "Average mood score: %.1f/5.0", overallAvg)
    }

    private fun moodDescriptor(score: Float): Pair<String, String> {
        return when {
            score >= 4.5f -> "Excellent" to "🤩"
            score >= 4.0f -> "Great" to "😄"
            score >= 3.0f -> "Good" to "🙂"
            score >= 2.0f -> "Low" to "😕"
            score > 0f -> "Very Low" to "😞"
            else -> "No Data" to "–"
        }
    }

    // Habit summaries (limited by stored aggregate data)
    private fun updateHabitSummaries(period: String) {
        val overallPercentView = findViewById<TextView>(R.id.tv_overall_progress_percent)
        val bestHabitNameView = findViewById<TextView>(R.id.tv_best_habit_name)
        val bestHabitPercentView = findViewById<TextView>(R.id.tv_best_habit_percent)

        val points = if (period == "weekly") 7 else 30
        val percents = habitHistoryManager.getLastNDaysPercents(points).map { it.second }
        val avg = if (percents.isEmpty()) 0.0 else percents.average()
        overallPercentView.text = String.format(Locale.getDefault(), "%.1f%%", avg)

        // Best habit cannot be derived historically (no per-habit history). We'll approximate using today's habits.
        val todayHabits = loadHabitsToday()
        if (todayHabits.isNotEmpty()) {
            val completed = todayHabits.filter { it.completed }
            val best = completed.firstOrNull() ?: todayHabits.first()
            bestHabitNameView.text = best.name
            bestHabitPercentView.text = if (best.completed) "100%" else "0%"
        } else {
            bestHabitNameView.text = "No Habits"
            bestHabitPercentView.text = "0%"
        }
    }

    private fun loadHabitsToday(): List<Habit> {
        val prefs = Prefs.habits(this)
        val json = prefs.getString(Prefs.KEY_HABITS, null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<Habit>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                try { list.add(Habit.fromJson(obj)) } catch (_: Exception) {}
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    private fun initCharts() {
        // Line chart styling
        lineChart.apply {
            setNoDataText("No mood data")
            description.isEnabled = false
            legend.apply { form = Legend.LegendForm.CIRCLE; textColor = Color.parseColor("#444444") }
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = Color.parseColor("#666666")
                axisLineColor = Color.TRANSPARENT
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
                axisMinimum = 0f
                axisMaximum = 5f
                granularity = 1f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#666666")
                setDrawGridLines(false)
                axisLineColor = Color.TRANSPARENT
                granularity = 1f
                // Placeholder formatter; real labels applied in applyPeriod()
                valueFormatter = ListAxisFormatter(emptyList())
            }
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            animateX(600)
        }

        // Bar chart styling (show percentage 0..100)
        barChart.apply {
            setNoDataText("No habit data")
            description.isEnabled = false
            legend.apply { form = Legend.LegendForm.SQUARE; textColor = Color.parseColor("#444444") }
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = Color.parseColor("#666666")
                axisLineColor = Color.TRANSPARENT
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#666666")
                setDrawGridLines(false)
                axisLineColor = Color.TRANSPARENT
                granularity = 1f
                valueFormatter = ListAxisFormatter(emptyList())
            }
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            animateY(600)
        }
    }

    private fun displayMoodChart(chart: LineChart, data: List<Entry>, period: String, labels: List<String>) {
        val dataSet = LineDataSet(data, if (period == "weekly") "Weekly Mood" else "Monthly Mood").apply {
            color = Color.parseColor("#8A3FFC")
            lineWidth = 2.2f
            setCircleColor(Color.parseColor("#8A3FFC"))
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextColor = Color.parseColor("#444444")
            valueTextSize = 9f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#8A3FFC")
            fillAlpha = 40
            highLightColor = Color.parseColor("#FF9800")
        }
        chart.data = LineData(dataSet)
        chart.xAxis.valueFormatter = ListAxisFormatter(labels)
        chart.invalidate()
    }

    private fun displayHabitChart(chart: BarChart, data: List<BarEntry>, period: String, labels: List<String>) {
        val dataSet = BarDataSet(data, if (period == "weekly") "Habit Completion %" else "Monthly Habit Completion %").apply {
            color = Color.parseColor("#03DAC5")
            valueTextColor = Color.parseColor("#444444")
            valueTextSize = 9f
            setDrawValues(false)
            highLightAlpha = 60
        }
        chart.data = BarData(dataSet).apply { barWidth = 0.4f }
        chart.xAxis.valueFormatter = ListAxisFormatter(labels)
        if (period == "monthly") chart.setVisibleXRangeMaximum(10f) else chart.setVisibleXRangeMaximum(7f)
        chart.invalidate()
    }

    // Remove old DayAxisFormatter and replace with ListAxisFormatter
    private class ListAxisFormatter(private val labels: List<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val i = value.toInt()
            return if (i in labels.indices) labels[i] else ""
        }
    }
}

private fun String.capitalizeFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }