package com.aps.imagerecognition.control

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aps.imagerecognition.R
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import com.aps.imagerecognition.control.EnumFilters.*

class CamImgReader(private val context: Activity) {

    private val TAG = "Log Image Recognition"
    private var tagFilter: EnumFilters? = null
    private val PERMISSION_CODE = 1030
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val cameraPreview = context.findViewById<TextureView>(R.id.cameraPreview)
    private lateinit var surface: Surface
    private lateinit var cameraId: String
    private var imageSize :Size
    private var cameraDevice: CameraDevice? = null
    private var captureSession : CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val threadBkg = HandlerThread("CameraBkg").apply { start() }
    private val handlerBkg = Handler(threadBkg.looper)

    init {
        log("Start CamImageReader")
        imageSize = getOptimalSize()
        initOpenCV()
        startTextureView()
    }
    //Define a melhor tamanho e proporçãoção com base no dispositivo
    private fun getOptimalSize(): Size {
        // Método para pegar o ID da câmera traseira
        cameraId = cameraManager.cameraIdList.first { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK
        }
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)

        // Obtém o tamanho da tela
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Escolhe o tamanho mais próximo
        return sizes?.minByOrNull { size ->
            val diffWidth = Math.abs(size.width - screenWidth)
            val diffHeight = Math.abs(size.height - screenHeight)
            diffWidth + diffHeight
        } ?: Size(1920, 1080) // tamanho padrão caso não encontre
    }

    //Define os parametros de retorno da imagem
    private fun startTextureView(){
        log("Start Camera Preview")
        cameraPreview.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {

                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, p1: Int, p2: Int) {
                    //cria um surface para o SurfaceTexture
                    log("TextureView Width: ${cameraPreview.width}, Height: ${cameraPreview.height}")
                    log("SurfaceTexture Width: ${p1}, Height: ${p2}")
                    surfaceTexture.setDefaultBufferSize(imageSize.width, imageSize.height)
                    surface = Surface(surfaceTexture)
                    log("Camera Preview Started")
                    getPermission()
                }
                override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, p1: Int, p2: Int) {}
                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }
                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}

            }
    }

    //Abre a câmera do dispositivo
    private fun startCamera() {
        log("Start Camera")

        //Configura o Imagerender para capturar os frames da câmera
        log("Setup Image Render")
        imageReader = ImageReader.newInstance(
            imageSize.width, imageSize.height,  ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val img = reader.acquireLatestImage()
            if (img != null) {
                val mat = matConvert(img)
                processFrame(mat)
                img.close()
            }
        }, handlerBkg)

        // Abre a câmera
        try {
            if (ActivityCompat.checkSelfPermission(context,Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED) {

                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback(){

                    // Callback quando a câmera é aberta
                    override fun onOpened(camera: CameraDevice) {
                        log("Camera Opened")
                        cameraDevice = camera
                        previewSession()
                    }
                    override fun onDisconnected(camera: CameraDevice) {
                        log("Camera Disconnected")
                        cameraDevice?.close()
                        cameraDevice = null
                    }
                    override fun onError(camera: CameraDevice, p1: Int) {
                        log("Camera Error")
                        cameraDevice?.close()
                        cameraDevice = null
                    }
                }, handlerBkg)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    // Converter o frame de Image para Mat (OpenCV)
    //Possibilitando o processamento das imagens
    private fun matConvert(image: Image): Mat {

        val planes = image.planes
        val yPlane = planes[0].buffer
        val uPlane = planes[1].buffer
        val vPlane = planes[2].buffer

        // Criar o Mat para a imagem YUV
        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()

        val yuvMat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        val yuvData = ByteArray(ySize + uSize + vSize)

        // Copiar os dados dos planos para o ByteArray
        yPlane.get(yuvData, 0, ySize)
        uPlane.get(yuvData, ySize, uSize)
        vPlane.get(yuvData, ySize + uSize, vSize)

        // Preencher o Mat com os dados da imagem
        yuvMat.put(0, 0, yuvData)

        // Converter para BGR (ou RGB)
        val rgbMat = Mat()
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2BGR_I420)

        val rotatedMAt = Mat()
        Core.rotate(rgbMat, rotatedMAt, Core.ROTATE_90_CLOCKWISE)

        return rotatedMAt
    }

    //Cria a sessão de captura da camera
    private fun previewSession() {

        log("Start Preview Session")
        val captureRequestBuilder =
            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(imageReader.surface)
        log("Creating Capture Session")
        cameraDevice?.createCaptureSession(listOf(imageReader.surface),
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder?.build()?.let {
                        session.setRepeatingRequest(it, null, handlerBkg)
                    }
                    log("Capture Session Created")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    log("Error: Created Session")
                }
            }, handlerBkg
        )
    }

    //processamentos de filtros
    private fun processFrame(mat: Mat){

        when (tagFilter){
            GRY -> updatePreview(convertToGray(mat))
            HEL -> updatePreview(applyHistogramEqualization(mat))
            HSV -> updatePreview(convertBGRToHSV(mat))
            CED -> updatePreview(applyCannyEdgeDetection(mat))
            BLF -> updatePreview(applyBilateralFilter(mat, 25, 145.0, 145.0))
            RGB -> updatePreview(mat)
            else -> updatePreview(mat)
        }
    }

    // Filtro de equalização do histograma
    private fun applyHistogramEqualization(mat: Mat): Mat {
        // Converter a imagem para escala de cinza
        val grayMat = convertToGray(mat)

        // Aplicar a equalização do histograma
        val equalizedMat = Mat()
        Imgproc.equalizeHist(grayMat, equalizedMat)

        return equalizedMat
    }

    // Método para converter BGR para HSV (Hue, Saturation, Value)
    //segmentação de cores e ajuste de brilho e saturação.
    private fun convertBGRToHSV(mat: Mat): Mat {
        // Converter BGR para HSV
        val hsvMat = Mat()
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_BGR2HSV)

        // Separar os canais HSV
        val channels = mutableListOf<Mat>()
        Core.split(hsvMat, channels)

        // Ajustar o canal Hue (Canal 0) - Mantém o mesmo
        val hueMat = channels[0]
        // Se necessário, você pode ajustar o Hue aqui, mas geralmente não é necessário para intensificar os tons

        // Ajustar o canal Saturation (Canal 1) - Aumentar saturação para intensificar as cores
        val saturationMat = channels[1]
        saturationMat.convertTo(saturationMat, -1, 13.2) // Aumenta a saturação em 20%

        // Ajustar o canal Value (Canal 2) - Aumentar o brilho para intensificar a luminosidade
        val valueMat = channels[2]
        valueMat.convertTo(valueMat, -1, 1.5) // Aumenta o brilho em 10%

        // Recombinar os canais
        Core.merge(channels, hsvMat)

        // Converter de volta para BGR
        val bgrMat = Mat()
        Imgproc.cvtColor(hsvMat, bgrMat, Imgproc.COLOR_HSV2BGR)

        // Liberar memória dos Mats temporários
        for (channel in channels) {
            channel.release()
        }
        hsvMat.release()

        return bgrMat
    }

    //Filtro escalas de Cinza
    private fun convertToGray(mat: Mat): Mat {
        val grayMat = Mat()
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)
        return grayMat
    }

    //Filtro de detecção de bordas
    //Canny Edge Detection
    private fun applyCannyEdgeDetection(mat: Mat): Mat {
        // Convertendo a imagem para escala de cinza
        val grayMat = convertToGray(mat)

        // Aplicando o blur para reduzir o ruído
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(5.0, 5.0), 0.0)

        // Aplicando o algoritmo Canny
        val edgesMat = Mat()
        val lowerThreshold = 100.0 // Limite inferior (ajustável)
        val upperThreshold = 200.0 // Limite superior (ajustável)
        Imgproc.Canny(blurredMat, edgesMat, lowerThreshold, upperThreshold)

        return edgesMat
    }

    //Filtro bilateral: suavização espacial e de cor preservando as bordas
    private fun applyBilateralFilter(inputMat: Mat, d: Int, sigmaColor: Double, sigmaSpace: Double): Mat {
        val outputMat = Mat()
        Imgproc.bilateralFilter(inputMat, outputMat, d, sigmaColor, sigmaSpace)
        return outputMat
    }


    //PRinta os frames processados
    private fun updatePreview(mat: Mat) {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        // Atualiza o SurfaceTexture com a nova imagem
        cameraPreview.lockCanvas()?.let {
            // Desenha o Bitmap no Canvas do TextureView
            it.drawBitmap(bitmap, 0f, 0f, null)
            cameraPreview.unlockCanvasAndPost(it)
        }
    }

    //Fechar a camera e liberar os recursos
    private fun closeCamera() {
        captureSession?.close()
        cameraDevice?.close()
        imageReader.close()
        surface.release()
        threadBkg.quitSafely()
        log("Camera Closed")
    }

    private fun initOpenCV() {
        if (OpenCVLoader.initLocal()){
            log("OpenCV Stancied")
        } else {
            log("OpenCV Fail")
        }
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

    fun setFilter(e: EnumFilters){
        tagFilter = e
    }
    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            log("Permission: ${checkPermissions()}")
            dialog()
        }
    }
    fun handleOnPause() {
        if (captureSession != null) {
            closeCamera()
        } else {
            log("FiltersCam Really Disabled")
        }
    }
}