package com.example.newmad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText

class AddHabitActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode" // add | edit
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_HABIT_DESC = "habit_desc"
        const val EXTRA_HABIT_COMPLETED = "habit_completed"
        const val MODE_ADD = "add"
        const val MODE_EDIT = "edit"
    }

    private var mode: String = MODE_ADD
    private var editingHabitId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        val toolbar = findViewById<MaterialToolbar>(R.id.add_habit_toolbar)
        val nameInput = findViewById<TextInputEditText>(R.id.input_habit_name)
        val descInput = findViewById<TextInputEditText>(R.id.input_habit_desc)
        val switchCompleted = findViewById<MaterialSwitch>(R.id.switch_completed)
        val saveFab = findViewById<ExtendedFloatingActionButton>(R.id.btn_save_habit)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ADD
        if (mode == MODE_EDIT) {
            toolbar.title = "Edit Habit"
            editingHabitId = intent.getStringExtra(EXTRA_HABIT_ID)
            nameInput.setText(intent.getStringExtra(EXTRA_HABIT_NAME) ?: "")
            descInput.setText(intent.getStringExtra(EXTRA_HABIT_DESC) ?: "")
            switchCompleted.isChecked = intent.getBooleanExtra(EXTRA_HABIT_COMPLETED, false)
        } else {
            toolbar.title = "New Habit"
        }

        toolbar.setNavigationOnClickListener { finish() }

        saveFab.setOnClickListener {
            val name = nameInput.text?.toString()?.trim() ?: ""
            val desc = descInput.text?.toString()?.trim().orEmpty()
            val completed = switchCompleted.isChecked
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val data = Intent().apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_HABIT_ID, editingHabitId)
                putExtra(EXTRA_HABIT_NAME, name)
                putExtra(EXTRA_HABIT_DESC, if (desc.isEmpty()) null else desc)
                putExtra(EXTRA_HABIT_COMPLETED, completed)
            }
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }
}
