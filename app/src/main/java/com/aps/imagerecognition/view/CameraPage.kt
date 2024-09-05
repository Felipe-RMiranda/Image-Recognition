package com.aps.imagerecognition.view

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aps.imagerecognition.R
import com.aps.imagerecognition.control.ApiCamera2
import com.aps.imagerecognition.control.SurfaceView
import com.aps.imagerecognition.control.UtilsCamera
import org.opencv.android.JavaCameraView

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

//        utilsCamera = UtilsCamera(this)
//        surfaceView = SurfaceView(this)
        apiCamera2 = ApiCamera2(this)

        btnMenu = findViewById(R.id.btnMenu)
        btnClick = findViewById(R.id.cameraClick)
        btnClick.setOnClickListener{
            Toast.makeText(this, "Filtros desabilitados", Toast.LENGTH_LONG).show()
//            utilsCamera.enableFilter()
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
//        utilsCamera.handlePermissionsResult(requestCode, grantResults)
//        surfaceView.handlePermissionsResult(requestCode, grantResults)
        apiCamera2.handlePermissionsResult(requestCode, grantResults)
    }

    override fun onResume() {
        super.onResume()
//        surfaceView.handleOnResume()
    }
    override fun onPause() {
        super.onPause()
        utilsCamera.handleOnPause()
//        surfaceView.handleOnPause()
//        apiCamera2.handleOnPause()
    }
    override fun onDestroy() {
        super.onDestroy()
//        surfaceView.handleOnPause()
    }

    private val TAG = "Log Image Recognition"
    private lateinit var sharPref: SharedPreferences
    private lateinit var utilsCamera: UtilsCamera
    private lateinit var surfaceView: SurfaceView
    private lateinit var apiCamera2: ApiCamera2
    private lateinit var btnMenu: ImageView
    private lateinit var btnClick: ImageView
}