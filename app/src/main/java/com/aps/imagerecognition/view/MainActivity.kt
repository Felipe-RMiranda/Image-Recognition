package com.aps.imagerecognition.view

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aps.imagerecognition.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        log("Start MainActivity")
        sharPref = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        if (getIntro()) {
            log("Set View")
            setContentView(R.layout.activity_main)
        } else {
            val intent = Intent(this, InfoPage::class.java)
            startActivity(intent)
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txt = findViewById(R.id.txt)
        dialog()
    }

    private fun dialog(){
        log("Start Dialog")
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Status da Aplicação")
        dialog.setMessage("Aplicação funcionando corretamente!")
        dialog.setPositiveButton("OK"){v, _ ->
            log("View Visible")
            txt.visibility = View.VISIBLE
            v.dismiss()
        }
        runOnUiThread{
            Handler().postDelayed({
                dialog.create().show()
            },1500)
        }

    }

    private fun getIntro(): Boolean {
        return sharPref.getBoolean("intro_show", false)
    }

    private fun log(s: String) {
        Log.d(TAG, s)
    }
    private val TAG = "Log Image Recognition"
    private lateinit var sharPref: SharedPreferences
    private lateinit var txt: TextView
}