package com.aps.imagerecognition.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aps.imagerecognition.R
import com.aps.imagerecognition.control.EnumFilters.*
import com.aps.imagerecognition.control.CamImgReader

class CameraPage : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        log("Start MainActivity")
        sharPref = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        if (getIntro()) {
            log("Set View")
            setContentView(R.layout.camera_page)
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        camImg = CamImgReader(this)

        gry = findViewById(R.id.gry)
        hel = findViewById(R.id.hel)
        hsv = findViewById(R.id.hsv)
        ced = findViewById(R.id.ced)
        blf = findViewById(R.id.blf)
        rgb = findViewById(R.id.rgb)

        val filterMap = mapOf(
            gry to GRY,
            hel to HEL,
            hsv to HSV,
            ced to CED,
            blf to BLF,
            rgb to RGB
        )

        filterMap.forEach { (button, filter) ->
            button.setOnClickListener {

                camImg.setFilter(filter)
                selectedButton?.setImageResource(R.drawable.camera)
                button.setImageResource(R.drawable.set_filter)
                selectedButton = button
            }
        }
    }

    private fun getIntro(): Boolean {
        log("sharPref: ${sharPref.getBoolean("intro_show", false)}")
        return sharPref.getBoolean("intro_show", false)
    }
    private fun log(s: String) {
        Log.d(TAG, s)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camImg.handlePermissionsResult(requestCode, grantResults)
    }

    override fun onResume(){
        super.onResume()
        camImg.resume()
    }
    override fun onPause() {
        super.onPause()
        camImg.handleOnPause()
    }
    override fun onDestroy() {
        super.onDestroy()
        camImg.handleOnPause()
    }

    private val TAG = "Log Image Recognition"
    private lateinit var sharPref: SharedPreferences
    private lateinit var camImg: CamImgReader
    private var selectedButton: ImageView? = null
    private lateinit var gry: ImageView
    private lateinit var hel: ImageView
    private lateinit var hsv: ImageView
    private lateinit var ced: ImageView
    private lateinit var blf: ImageView
    private lateinit var rgb: ImageView
}