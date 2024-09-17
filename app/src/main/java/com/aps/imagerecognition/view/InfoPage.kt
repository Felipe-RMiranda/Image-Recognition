package com.aps.imagerecognition.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aps.imagerecognition.R

class InfoPage : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        log("Start InfoPAge")
        setContentView(R.layout.info_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        window.decorView.windowInsetsController?.apply {
            hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val sharPref = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        val editor = sharPref.edit()
        editor.putBoolean("intro_show", true)
        editor.apply()
        log("Saved stage InfoPage: ${sharPref.getBoolean("intro_page", false)}")

        val btnStart: Button = findViewById(R.id.btnStart)
        btnStart.setOnClickListener{
            log("Button click")
            val intent = Intent(this, CameraPage::class.java)
            startActivity(intent)
            log("finish InfoPage")
            finish()
        }
    }
    private fun log(s: String) {
        Log.d(TAG, s)
    }
    private val TAG = "Log Image Recognition"
}