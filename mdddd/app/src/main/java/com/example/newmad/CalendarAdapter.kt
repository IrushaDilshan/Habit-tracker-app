package com.example.newmad
// Adapter for displaying a calendar month in a RecyclerView grid
// Each day can show an emoji or image representing the user's mood
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class CalendarAdapter(
    private val days: MutableList<Int>, // 0 for empty cell, otherwise day number
    private var moods: Map<Int, String> = emptyMap(),
    private var selectedDay: Int = -1,
    private val onDayClick: (day: Int) -> Unit = {}
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    // Map emojis to optional drawable resources (adjust mapping to real assets)
    private val moodImageMap: Map<String, Int> = mapOf(
        "😊" to R.drawable.o1,
        "😄" to R.drawable.o2,
        "😐" to R.drawable.o3
    )

    inner class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tv_day)
        val tvMood: TextView = view.findViewById(R.id.tv_mood)
        val ivMood: ImageView? = view.findViewById(R.id.iv_mood)
        val bg: MaterialCardView? = view.findViewById(R.id.day_bg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(v)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        if (day == 0) {
            holder.tvDay.text = ""
            holder.tvMood.visibility = View.GONE
            holder.ivMood?.visibility = View.GONE
            holder.bg?.visibility = View.GONE
            holder.itemView.isClickable = false
        } else {
            holder.tvDay.text = day.toString()
            val mood = moods[day]
            if (mood != null) {
                val imageRes = moodImageMap[mood]
                if (imageRes != null && holder.ivMood != null) {
                    holder.ivMood.setImageResource(imageRes)
                    holder.ivMood.visibility = View.VISIBLE
                    holder.tvMood.visibility = View.GONE
                } else {
                    holder.tvMood.text = mood
                    holder.tvMood.visibility = View.VISIBLE
                    holder.ivMood?.visibility = View.GONE
                }
            } else {
                holder.tvMood.visibility = View.GONE
                holder.ivMood?.visibility = View.GONE
            }
            holder.itemView.isClickable = true

            val ctx = holder.itemView.context
            val blue = colorFromHex("#6A8AFF")
            val mint = colorFromHex("#DFF7EE")

            if (day == selectedDay) {
                holder.bg?.visibility = View.VISIBLE
                holder.bg?.setCardBackgroundColor(blue)
                holder.tvDay.setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            } else if (mood != null) {
                holder.bg?.visibility = View.VISIBLE
                holder.bg?.setCardBackgroundColor(mint)
                holder.tvDay.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
            } else {
                holder.bg?.visibility = View.GONE
                holder.tvDay.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))
            }

            holder.itemView.setOnClickListener {
                val prevSelected = selectedDay
                selectedDay = day
                if (prevSelected != -1) {
                    val prevPos = days.indexOf(prevSelected)
                    if (prevPos >= 0) notifyItemChanged(prevPos)
                }
                notifyItemChanged(position)
                onDayClick(day)
            }
        }
    }

    override fun getItemCount(): Int = days.size

    fun setSelected(day: Int) {
        val prevSelected = selectedDay
        selectedDay = day
        if (prevSelected != -1) {
            val prevPos = days.indexOf(prevSelected)
            if (prevPos >= 0) notifyItemChanged(prevPos)
        }
        val newPos = days.indexOf(day)
        if (newPos >= 0) notifyItemChanged(newPos)
    }

    fun update(newDays: List<Int>, newMoods: Map<Int, String>, newSelectedDay: Int) {
        days.clear()
        days.addAll(newDays)
        moods = newMoods
        selectedDay = newSelectedDay
        notifyDataSetChanged()
    }

    private fun colorFromHex(hex: String): Int = android.graphics.Color.parseColor(hex)
}
