package com.aps.imagerecognition.control

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aps.imagerecognition.R

class SurfaceView(
    private val context: Activity
): SurfaceHolder.Callback {

    private val TAG = "Log Image Recognition"
    private val PERMISSION_CODE = 1030
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val surfaceView: SurfaceView = context.findViewById(R.id.cameraView)
    private val surfaceHolder: SurfaceHolder = surfaceView.holder
    private var camera: Camera? = null
    private lateinit var holder: SurfaceHolder

    init {
        log("Start UtilsCamera")
        surfaceHolder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        this.holder = holder
        getPermission()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Configure o que for necessário ao mudar a superfície
        camera?.stopPreview()
        try {
            camera?.setPreviewDisplay(holder)
            camera?.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        closeCamera()
    }

    private fun startCamera() {
        try {
            // Abrir a câmera
            camera = Camera.open()
            camera?.setPreviewDisplay(holder)
            camera?.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun closeCamera(){
        // Libere a câmera quando a superfície for destruída
        camera?.stopPreview()
        camera?.release()
        camera = null
    }

    private fun checkPermissions() = REQUIRED_PERMISSIONS.all {
        log("Check Permissions")
        ContextCompat.checkSelfPermission(
            context, it) == PackageManager.PERMISSION_GRANTED
    }
    private fun getPermission() {
        if (checkPermissions()) {
            log("Permission: ${checkPermissions()}")
            startCamera()
        } else {
            log("Permission requested")
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CODE
            )
        }
    }

    private fun dialog(){
        log("Start Dialog")
        val dialog = AlertDialog.Builder(context, R.style.DialogTheme)
        dialog.setTitle("Permissão")
        dialog.setMessage(
            "A permissão de utilização da camêra do dispositivo" +
                    "é fundamental para o funcionament da aplicação!"
        )
        dialog.setPositiveButton("Permitir"){v, _ ->
            log("Requesting Permission")
            getPermission()
            v.dismiss()
        }.setNegativeButton("Encerrar"){ _, _ ->
            log("finish application")
            context.finish()
        }
        context.runOnUiThread{
            dialog.create().show()
        }
    }
    private fun log(s: String) {
        Log.d(TAG, s)
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            log("Permission: ${checkPermissions()}")
            dialog()
        }
    }
    fun handleOnResume() {
        startCamera()
    }
    fun handleOnPause() {
        log("Camera feed Disabled")
        closeCamera()
    }
}