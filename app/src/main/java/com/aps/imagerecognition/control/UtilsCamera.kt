package com.aps.imagerecognition.control

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aps.imagerecognition.R
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class UtilsCamera(
    private val context: Activity
): SurfaceHolder.Callback {
    //SurfaceView: Utilize o SurfaceHolder.Callback para monitorar o ciclo de vida do SurfaceView

    private val TAG = "Log Image Recognition"
    private val PERMISSION_CODE = 1030
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val surface: SurfaceView = context.findViewById(R.id.cameraView)
    private val surfaceHolder: SurfaceHolder = surface.holder
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var previewSize: Size
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread

    private lateinit var imageReader: ImageReader

    init {
        log("Start ApiCamera2")
        surfaceHolder.addCallback(this)
    }
    override fun surfaceCreated(p0: SurfaceHolder) {
        getPermission()
    }
    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
    override fun surfaceDestroyed(p0: SurfaceHolder) {
        closeCamera()
        stopBackgroundThread()
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        //Preview da Câmera:
        // No callback onOpened() da câmera, inicie a visualização da câmera
        // configurando a sessão de captura e vinculando-a ao SurfaceView.
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            setupImageReader()
            startCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice?.close()
            cameraDevice = null
        }
    }
    private fun setupCamera() {
        startBackgroundThread()
        log("Start Setup Camera")
        //Identifica a câmera traseira e configura as propriedades de captura.
        //Utilize o CameraManager para acessar a câmera.
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            previewSize = streamConfigurationMap?.getOutputSizes(SurfaceHolder::class.java)?.get(0) ?: Size(1920, 1080)

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        log("Configured Camera")
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            image?.let {
                processImage(it)
                it.close()
            }
        }, backgroundHandler)
    }
    private fun processImage(image: Image) {
        val planes = image.planes
        val buffer = planes[0].buffer

        // Converte o buffer em um array de bytes
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Converte o array de bytes para uma Mat do OpenCV
        val mat = Mat(previewSize.height, previewSize.width, CvType.CV_8UC1)
        mat.put(0, 0, bytes)

        // Aplica um filtro no Mat
        val filteredMat = Mat()
        Imgproc.cvtColor(mat, filteredMat, Imgproc.COLOR_YUV2GRAY_NV21)

        // Converte o Mat de volta para um Bitmap
        val bitmap = Bitmap.createBitmap(filteredMat.cols(), filteredMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(filteredMat, bitmap)

        // Atualiza o SurfaceView com o Bitmap filtrado
        context.runOnUiThread {
            val canvas = surfaceHolder.lockCanvas()
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }


    private fun startCameraPreview() {
        log("Start Camera Preview")
        try {
            val surface = surfaceHolder.surface
            val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder?.addTarget(surface)

            previewRequestBuilder?.addTarget(imageReader.surface)

            cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    previewRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    val previewRequest = previewRequestBuilder?.build()
                    captureSession?.setRepeatingRequest(previewRequest!!, null, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    // Falhou ao configurar a sessão de captura
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
    private fun closeCamera() {
        //Liberação de Recursos:
        // No método closeCamera(), pare a sessão de captura e libere os recursos da câmera.
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        log("Camera Closed")
    }

    private fun startBackgroundThread() {
        //Gerenciamento de Threads:
        // Como a API Camera2 faz uso intensivo de threads,
        // inicia-se uma thread em segundo plano para lidar com
        // as operações de câmera e evite travar a interface do usuário.
        log("Start Background Thread")
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread.start()
        backgroundHandler = Handler(backgroundThread.looper)
        log("Background Thread Started")
    }
    private fun stopBackgroundThread() {
        if (::backgroundThread.isInitialized) {
            backgroundThread.quit()
            try {
                backgroundThread.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        log("Stop Background Thread")
    }

    private fun getPermission() {
        if (checkPermissions()) {
            log("Permission: ${checkPermissions()}")
            setupCamera()
        } else {
            log("Permission requested")
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CODE
            )
        }
    }
    private fun checkPermissions() = REQUIRED_PERMISSIONS.all {
        log("Check Permissions")
        ContextCompat.checkSelfPermission(
            context, it) == PackageManager.PERMISSION_GRANTED
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
            setupCamera()
        } else {
            log("Permission: ${checkPermissions()}")
            dialog()
        }
    }
    fun handleOnPause() {
        log("Camera feed Disabled")
        closeCamera()
        stopBackgroundThread()
    }
}