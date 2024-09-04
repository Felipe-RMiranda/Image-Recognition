package com.aps.imagerecognition.view

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat.Surface
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aps.imagerecognition.R
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraGLSurfaceView
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class CameraPage : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {


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

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE)
        }
        btnMenu = findViewById(R.id.btnMenu)
        btnClick = findViewById(R.id.cameraClick)
        cameraView = findViewById(R.id.cameraView)
        cameraView.visibility = SurfaceView.VISIBLE
        cameraView.setCvCameraViewListener(this)

        btnClick.setOnClickListener{
            changeFilter()
        }
    }
    private fun changeFilter() {
        isFilterActive = !isFilterActive // Alternar o estado do filtro
        log("Filter active: $isFilterActive")
    }

    private fun dialog(){
        log("Start Dialog")
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Permissões necessárias")
        dialog.setMessage("As funcionalidades da aplicação estão disponíveis, apenas mediante a concessão de permisões!")
        dialog.setPositiveButton("Permitir"){v, _ ->
            log("Requesting Permissions")
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE)
            v.dismiss()
        }.setNegativeButton("Sair"){ _, _ ->
            finish()
        }
        runOnUiThread{
            dialog.create().show()
        }
    }

    private fun getIntro(): Boolean {
        return sharPref.getBoolean("intro_show", false)
    }

    private fun log(s: String) {
        Log.d(TAG, s)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    private fun startCamera(){
        cameraView.visibility = SurfaceView.VISIBLE
        cameraView.enableView()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                dialog()
            }
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        matInput = Mat()
        matOutput = Mat()
    }

    // Liberar a Matriz ao parar a visualização
    override fun onCameraViewStopped() {
        matInput.release()
        matOutput.release()
    }

    // Processar os frames da câmera
    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        matInput = inputFrame.rgba()

        // Aplicar filtro somente se estiver ativo
        if (isFilterActive) {
            Imgproc.cvtColor(matInput, matOutput, Imgproc.COLOR_RGBA2GRAY)
            return matOutput
        } else {
            return matInput
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Falha ao inicializar OpenCV")
        } else {
            cameraView.enableView()
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }


    private val TAG = "Log Image Recognition"
    private val REQUEST_CODE = 1003
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private lateinit var sharPref: SharedPreferences
    private lateinit var btnMenu: ImageView
    private lateinit var btnClick: ImageView
    private lateinit var cameraView: JavaCameraView
    private lateinit var matInput: Mat
    private lateinit var matOutput: Mat
    private var isFilterActive: Boolean = true
}