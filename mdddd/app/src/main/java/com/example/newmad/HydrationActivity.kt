package com.example.newmad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class HydrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hydration)

        // Setup toolbar back navigation
        findViewById<MaterialToolbar>(R.id.hydration_toolbar)?.apply {
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.hydration_fragment_container, HydrationFragment())
                .commit()
        }
    }
}
