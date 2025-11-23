package com.example.newmad

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

interface HabitActionListener {
    fun onEdit(habit: Habit, position: Int)
    fun onDataChanged(habits: List<Habit>)
}

class HabitAdapter(
    private val habits: MutableList<Habit>,
    private val listener: HabitActionListener? = null
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    private val TAG = "HabitAdapter"

    init {
        // Ensure RecyclerView uses stable IDs (we override getItemId)
        setHasStableIds(true)
    }

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameView: TextView = itemView.findViewById(R.id.item_habit_name)
        val descView: TextView = itemView.findViewById(R.id.item_habit_desc)
        val checkBox: CheckBox = itemView.findViewById(R.id.item_habit_check)
        val editIcon: ImageView = itemView.findViewById(R.id.item_habit_edit)
        val deleteIcon: ImageView = itemView.findViewById(R.id.item_habit_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.nameView.text = habit.name
        holder.descView.text = habit.description ?: ""

        // Avoid triggering listener during bind
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = habit.completed
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // Use bindingAdapterPosition to ensure we update the correct item when views are recycled
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val item = habits[pos]
                item.completed = isChecked
                // Rebind the row to ensure any UI depending on the item state updates reliably
                notifyItemChanged(pos)
                Log.d(TAG, "Toggled completion: pos=$pos id=${item.id} checked=$isChecked")
                listener?.onDataChanged(habits)
            } else {
                Log.w(TAG, "Toggled completion but bindingAdapterPosition was NO_POSITION")
            }
        }

        holder.deleteIcon.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                val removed = habits.removeAt(pos)
                notifyItemRemoved(pos)
                Log.d(TAG, "Removed habit: pos=$pos id=${removed.id}")
                listener?.onDataChanged(habits)
            }
        }

        holder.editIcon.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                listener?.onEdit(habits[pos], pos)
            } else {
                Toast.makeText(holder.itemView.context, "Unable to edit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemId(position: Int): Long {
        // Use the habit UUID hash as a stable id
        return habits.getOrNull(position)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
    }

    override fun getItemCount(): Int = habits.size

    fun addHabit(habit: Habit) {
        habits.add(habit)
        notifyItemInserted(habits.size - 1)
        Log.d(TAG, "Added habit: id=${habit.id}")
        listener?.onDataChanged(habits)
    }

    fun updateHabitAt(position: Int, habit: Habit) {
        if (position in 0 until habits.size) {
            habits[position] = habit
            notifyItemChanged(position)
            Log.d(TAG, "Updated habit at pos=$position id=${habit.id}")
            listener?.onDataChanged(habits)
        }
    }

    fun setAll(newHabits: List<Habit>) {
        habits.clear()
        habits.addAll(newHabits)
        notifyDataSetChanged()
        Log.d(TAG, "setAll called; total=${habits.size}")
        listener?.onDataChanged(habits)
    }
}
