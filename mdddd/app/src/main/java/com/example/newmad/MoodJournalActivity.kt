package com.example.newmad

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.card.MaterialCardView
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class MoodJournalActivity : AppCompatActivity() {
    private val emojiList = listOf(
        MoodEmoji("Happy", "😊"), MoodEmoji("Sad", "😢"), MoodEmoji("Angry", "😠"), MoodEmoji("Content", "🙂"),
        MoodEmoji("Tired", "😴"), MoodEmoji("Anxious", "😰"), MoodEmoji("Cool", "😎"), MoodEmoji("Overwhelmed", "😣"),
        MoodEmoji("Thoughtful", "🤔"), MoodEmoji("Excited", "😍"), MoodEmoji("Peaceful", "😊"), MoodEmoji("Grateful", "🙏"),
    )

    private lateinit var moodAdapter: MoodHistoryAdapter
    private lateinit var moodManager: MoodManager
    private lateinit var moodHistory: MutableList<MoodEntry>
    private lateinit var historyRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood_journal)
        // Initialize MoodManager, which internally uses SharedPreferences
        moodManager = MoodManager(this)

        // Load today's moods from SharedPreferences
        moodHistory = moodManager.getMoodEntriesForToday().toMutableList()

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        findViewById<TextView>(R.id.mood_journal_date).text = dateFormat.format(Date())

        // Emoji RecyclerView (modern grid)
        val emojiRecycler = findViewById<RecyclerView>(R.id.mood_emoji_grid)
        if (emojiRecycler.layoutManager == null) {
            emojiRecycler.layoutManager = GridLayoutManager(this, 3)
        }
        if (emojiRecycler.itemDecorationCount == 0) {
            emojiRecycler.addItemDecoration(GridSpacingDecoration(3, dpToPx(8), includeEdge = false))
        }
        // Emoji click handling
        emojiRecycler.setHasFixedSize(true)
        emojiRecycler.isNestedScrollingEnabled = false
        emojiRecycler.adapter = EmojiGridAdapter(emojiList, highlightOnClick = true) { emoji ->
            logMood(emoji)
        }

        // Mood history
        historyRecycler = findViewById(R.id.mood_history_list)
        moodAdapter = MoodHistoryAdapter(this, moodHistory)
        historyRecycler.layoutManager = LinearLayoutManager(this)
        historyRecycler.adapter = moodAdapter
        historyRecycler.isNestedScrollingEnabled = false
        historyRecycler.setHasFixedSize(true)

        // Quick mood entry
        findViewById<FloatingActionButton>(R.id.mood_quick_entry_button).setOnClickListener {
            showQuickMoodDialog()
        }

        // Share today's mood summary
        findViewById<FloatingActionButton>(R.id.mood_share_button).setOnClickListener {
            shareTodaySummary()
        }

        // Bottom navigation: centralized
        NavigationUtil.setupBottomNav(this, R.id.menu_mood)
    }

    private fun logMood(emoji: MoodEmoji) {
        val entry = MoodEntry(emoji.emoji, emoji.label, System.currentTimeMillis(), "")
        moodHistory.add(0, entry)
        moodManager.saveMoodEntriesForToday(moodHistory)
        if (this::moodAdapter.isInitialized) {
            moodAdapter.notifyItemInserted(0)
        }
        if (this::historyRecycler.isInitialized) {
            historyRecycler.scrollToPosition(0)
        }
        Toast.makeText(this, "Mood logged: ${emoji.label}", Toast.LENGTH_SHORT).show()
    }

    private fun shareTodaySummary() {
        val entries = moodManager.getMoodEntriesForToday()
        if (entries.isEmpty()) {
            Toast.makeText(this, "No moods logged today to share", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        val df = SimpleDateFormat("h:mm a", Locale.getDefault())
        sb.append("Mood summary for ").append(SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())).append("\n\n")
        for (e in entries) {
            sb.append(e.emoji).append(" ").append(e.label).append(" — ").append(df.format(Date(e.timestamp))).append("\n")
        }

        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(Intent.EXTRA_SUBJECT, "My mood summary")
        send.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(send, "Share mood summary"))
    }

    private fun showQuickMoodDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quick_mood_entry, null)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.quick_mood_emoji_list)
        recycler.layoutManager = GridLayoutManager(this, 3)
        recycler.setHasFixedSize(true)
        recycler.isNestedScrollingEnabled = false
        if (recycler.itemDecorationCount == 0) {
            recycler.addItemDecoration(GridSpacingDecoration(3, dpToPx(8), includeEdge = false))
        }
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        recycler.adapter = EmojiGridAdapter(emojiList, highlightOnClick = false) { emoji ->
            logMood(emoji)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).roundToInt()
}

// Mood emoji data
data class MoodEmoji(val label: String, val emoji: String)

// Mood entry data
data class MoodEntry(val emoji: String, val label: String, val timestamp: Long, val note: String)

// Mood manager for SharedPreferences
class MoodManager(context: Context) {
    private val prefs = Prefs.mood(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun todayKey() = dateFormat.format(Date())

    fun getMoodEntriesForToday(): List<MoodEntry> {
        val json = prefs.getString(todayKey(), null) ?: return mutableListOf()
        val arr = JSONArray(json)
        val list = mutableListOf<MoodEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(MoodEntry(
                obj.getString("emoji"),
                obj.getString("label"),
                obj.getLong("timestamp"),
                obj.optString("note", "")
            ))
        }
        return list
    }

    fun saveMoodEntriesForToday(entries: List<MoodEntry>) {
        val arr = JSONArray()
        for (e in entries) {
            val obj = JSONObject()
            obj.put("emoji", e.emoji)
            obj.put("label", e.label)
            obj.put("timestamp", e.timestamp)
            obj.put("note", e.note)
            arr.put(obj)
        }
        prefs.edit().putString(todayKey(), arr.toString()).apply()
    }

    // New: get entries for a specific LocalDate
    fun getMoodEntriesForDate(day: java.time.LocalDate): List<MoodEntry> {
        val key = day.toString() // ISO yyyy-MM-dd
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<MoodEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(MoodEntry(
                    obj.getString("emoji"),
                    obj.getString("label"),
                    obj.getLong("timestamp"),
                    obj.optString("note", "")
                ))
            }
            list
        } catch (_: Exception) { emptyList() }
    }

    // Average mood score for a specific day based on mapping.
    fun getAverageMoodScoreForDate(day: java.time.LocalDate): Float {
        val entries = getMoodEntriesForDate(day)
        if (entries.isEmpty()) return 0f
        var sum = 0
        var count = 0
        for (e in entries) {
            val score = MOOD_SCORE[e.label] ?: MOOD_SCORE[e.emoji] // fallback by emoji if label changed
            if (score != null) { sum += score; count++ }
        }
        if (count == 0) return 0f
        return sum.toFloat() / count
    }

    // New: get first emoji logged for each day in given month (or most frequent if multiple)
    fun getFirstEmojiPerDay(year: Int, month: Int): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            val key = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day)
            val json = prefs.getString(key, null) ?: continue
            try {
                val arr = JSONArray(json)
                if (arr.length() == 0) continue
                // Option: choose first entry or compute most frequent emoji
                val freq = mutableMapOf<String, Int>()
                for (i in 0 until arr.length()) {
                    val emoji = arr.getJSONObject(i).getString("emoji")
                    freq[emoji] = (freq[emoji] ?: 0) + 1
                }
                val representative = freq.maxByOrNull { it.value }?.key ?: arr.getJSONObject(0).getString("emoji")
                result[day] = representative
            } catch (_: Exception) { }
        }
        return result
    }

    companion object {
        // Simple score scale 1..5
        private val MOOD_SCORE = mapOf(
            "Happy" to 5,
            "Excited" to 5,
            "Peaceful" to 4,
            "Content" to 4,
            "Grateful" to 4,
            "Cool" to 4,
            "Thoughtful" to 3,
            "Tired" to 2,
            "Sad" to 2,
            "Anxious" to 2,
            "Overwhelmed" to 1,
            "Angry" to 1
        )
    }
}

// Recycler adapter for emoji grid with optional selection highlight
private class EmojiGridAdapter(
    private val emojis: List<MoodEmoji>,
    private val highlightOnClick: Boolean = true,
    private val onClick: (MoodEmoji) -> Unit
) : RecyclerView.Adapter<EmojiGridAdapter.EmojiVH>() {

    inner class EmojiVH(view: View) : RecyclerView.ViewHolder(view) {
        private val emojiText: TextView = view.findViewById(R.id.emoji_text)
        private val emojiLabel: TextView = view.findViewById(R.id.emoji_label)
        private val card: MaterialCardView = view.findViewById(R.id.emoji_card)
        fun bind(item: MoodEmoji) {
            emojiText.text = item.emoji
            emojiLabel.text = item.label
            card.strokeWidth = 0
            card.setOnClickListener {
                if (highlightOnClick) animateSelection(card)
                onClick(item)
            }
        }
        private fun animateSelection(card: MaterialCardView) {
            val start = ContextCompat.getColor(card.context, android.R.color.transparent)
            val end = ContextCompat.getColor(card.context, R.color.purple_500)
            val animator = ValueAnimator.ofObject(ArgbEvaluator(), start, end, start)
            animator.duration = 450
            animator.addUpdateListener { v ->
                val color = v.animatedValue as Int
                card.strokeColor = color
                card.strokeWidth = if (color == start) 0 else card.resources.getDimensionPixelSize(R.dimen.stroke_width_emoji)
            }
            animator.start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood_emoji_cell, parent, false)
        return EmojiVH(v)
    }
    override fun onBindViewHolder(holder: EmojiVH, position: Int) = holder.bind(emojis[position])
    override fun getItemCount(): Int = emojis.size
}

// Spacing decoration for uniform grid spacing without per-item margins
private class GridSpacingDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}

// Mood history adapter
class MoodHistoryAdapter(private val context: Context, private val moods: List<MoodEntry>) : RecyclerView.Adapter<MoodHistoryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mood_history, parent, false)
        return MoodHistoryViewHolder(view)
    }
    override fun onBindViewHolder(holder: MoodHistoryViewHolder, position: Int) {
        holder.bind(moods[position])
    }
    override fun getItemCount() = moods.size
}

class MoodHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(entry: MoodEntry) {
        val emoji = itemView.findViewById<TextView>(R.id.history_emoji)
        val time = itemView.findViewById<TextView>(R.id.history_time)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
        emoji.text = entry.emoji
        time.text = timeFormat
    }
}