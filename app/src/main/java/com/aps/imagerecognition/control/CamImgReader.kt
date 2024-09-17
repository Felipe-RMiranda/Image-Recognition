package com.aps.imagerecognition.control

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
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
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.TextView
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
    private var tagOrientaion: EnumFilters? = null
    private val PERMISSION_CODE = 1030
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val cameraPreview = context.findViewById<TextureView>(R.id.cameraPreview)
    private lateinit var surface: Surface
    private lateinit var cameraId: String
    private var imageSize: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private lateinit var threadBkg: HandlerThread
    private lateinit var handlerBkg: Handler

    init {
        init()
    }

    private fun init() {
        log("Start CamImageReader")
        threadBkg = HandlerThread("CameraBkg").apply { start() }
        handlerBkg = Handler(threadBkg.looper)
        getOptimalSize()
        initOpenCV()
        startTextureView()
    }

    //Define a melhor tamanho e proporçãoção com base no dispositivo
    private fun getOptimalSize() {
        // Método para pegar o ID da câmera traseira
        cameraId = cameraManager.cameraIdList.first { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            facing == CameraCharacteristics.LENS_FACING_BACK
        }
        // Defina o tamanho do buffer de acordo com a proporção desejada (9:16)
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        imageSize = Size(screenWidth, screenHeight)
    }

    //Define os parametros de retorno da imagem
    private fun startTextureView() {
        log("Start Texture View")
        cameraPreview.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {

                override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, p1: Int, p2: Int) {
                    //cria um surface para o SurfaceTexture
                    surfaceTexture.setDefaultBufferSize(imageSize!!.width, imageSize!!.height)
                    surface = Surface(surfaceTexture)

                    log("TextureView Width: ${cameraPreview.width}, Height: ${cameraPreview.height}")
                    log("SurfaceTexture Width: ${imageSize!!.width}, Height: ${imageSize!!.height}")
                    log("Camera Preview Started")
                    getPermission()
                }
                override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, p1: Int, p2: Int) {
                    surfaceTexture.setDefaultBufferSize(imageSize!!.width, imageSize!!.height)
                    surface = Surface(surfaceTexture)
                    log("SurfaceTexture Size Changed: Width: $p1, Height: $p2")
                    log("TextureView Width: ${cameraPreview.width}, Height: ${cameraPreview.height}")
                }
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
            imageSize!!.width, imageSize!!.height, ImageFormat.YUV_420_888, 2
        )
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
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {

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

        // Converter para RGB
        val rgbMat = Mat()
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV12)

        val rotatedMAt = Mat()
        Core.rotate(rgbMat, rotatedMAt, Core.ROTATE_90_CLOCKWISE)

        val rotatedMAt180 = Mat()
        Core.rotate(rgbMat, rotatedMAt180, Core.ROTATE_180)

        return when (tagOrientaion){
            PORTRAIT -> rotatedMAt
            LANDSCAPE -> rgbMat
            LANDSCAPEREVERSE -> rotatedMAt180
            else -> rotatedMAt
        }
    }

    //Cria a sessão de captura da camera
    private fun previewSession() {

        log("Start Preview Session")
        val captureRequestBuilder =
            cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder?.addTarget(imageReader.surface)
        log("Creating Capture Session")
        cameraDevice?.createCaptureSession(
            listOf(imageReader.surface),
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
    private fun processFrame(mat: Mat) {

        when (tagFilter) {
            GRY -> updatePreview(convertToGray(mat))
            HEL -> updatePreview(applyHistogramEqualization(mat))
            HSV -> updatePreview(convertRgbToHSV(mat, 5.2, 1.0))
            CED -> updatePreview(applyCannyEdgeDetection(mat))
            HSVeCED -> updatePreview(applyCannyAndHSV(mat))
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
    private fun convertRgbToHSV(mat: Mat, saturation:Double, brilho:Double): Mat {
        // Converter RGB para HSV
        val hsvMat = Mat()
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV)

        // Separar os canais HSV
        val channels = mutableListOf<Mat>()
        Core.split(hsvMat, channels)

        // Ajustar o canal Hue (Canal 0) - Mantém o mesmo
        val hueMat = channels[0]
        // Se necessário, você pode ajustar o Hue aqui, mas geralmente não é necessário para intensificar os tons

        // Ajustar o canal Saturation (Canal 1) - Aumentar saturação para intensificar as cores
        val saturationMat = channels[1]
        saturationMat.convertTo(saturationMat, -1, saturation) // Aumenta a saturação em 20%

        // Ajustar o canal Value (Canal 2) - Aumentar o brilho para intensificar a luminosidade
        val valueMat = channels[2]
        valueMat.convertTo(valueMat, -1, brilho) // Aumenta o brilho em 10%

        // Recombinar os canais
        Core.merge(channels, hsvMat)

        // Converter de volta para BGR
        val bgrMat = Mat()
        Imgproc.cvtColor(hsvMat, bgrMat, Imgproc.COLOR_HSV2RGB)

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
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(9.0, 9.0), 0.0)

        // Aplicando o algoritmo Canny
        val edgesMat = Mat()
        val lowerThreshold = 40.0 // Limite inferior (ajustável)
        val upperThreshold = 80.0 // Limite superior (ajustável)
        Imgproc.Canny(blurredMat, edgesMat, lowerThreshold, upperThreshold)

        // Criar uma imagem em Verde para destacar as bordas
        val redEdges = Mat.zeros(mat.size(), CvType.CV_8UC3)
        val redColor = Scalar(0.0, 255.0, 225.0) // VERDE em RGB
        redEdges.setTo(redColor, edgesMat)

        // Criar uma imagem em vermelho para destacar as bordas
        val blueEdges = Mat.zeros(mat.size(), CvType.CV_8UC3)
        val blueColor = Scalar(225.0, 0.0, 255.0) // Vermelho em RGB
        blueEdges.setTo(blueColor, edgesMat)

        // Calcular o deslocamento para criar o efeito anaglifo
        val imageWidth = mat.cols()
        val displacementX = (imageWidth * 0.002).toInt() // Deslocamento horizontal
        val displacementY = 0

        // Criar uma matriz de transformação para deslocamento
        val shiftMatrix = Mat(2, 3, CvType.CV_32F)
        shiftMatrix.put(0, 0, 1.0, 0.0, displacementX.toDouble(),
            0.0, 1.0, displacementY.toDouble())

        // Aplicar o deslocamento à imagem azul
        val displacedBlueEdges = Mat()
        Imgproc.warpAffine(blueEdges, displacedBlueEdges, shiftMatrix, blueEdges.size(), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(0.0, 0.0, 0.0))

        // Criar a imagem final para o efeito anaglifo
        val anaglyphMat = Mat.zeros(mat.size(), CvType.CV_8UC3)

        // Mesclar as bordas vermelhas e deslocadas azuis na imagem final
        Core.addWeighted(redEdges, 1.0, displacedBlueEdges, 1.0, 0.0, anaglyphMat)

        // Liberar memória
        redEdges.release()
        blueEdges.release()
        displacedBlueEdges.release()
        shiftMatrix.release()

        return anaglyphMat
    }

    private fun applyCannyAndHSV(mat: Mat): Mat {
        // Convertendo a imagem para HSV
        val hsv = convertRgbToHSV(mat, 5.2, 0.3)

        // Aplicando o blur para reduzir o ruído
        val blurredMat = Mat()
        Imgproc.GaussianBlur(convertToGray(mat), blurredMat, Size(9.0, 9.0), 0.0)

        // Aplicando o algoritmo Canny
        val edgesMat = Mat()
        val lowerThreshold = 40.0 // Limite inferior (ajustável)
        val upperThreshold = 80.0 // Limite superior (ajustável)
        Imgproc.Canny(blurredMat, edgesMat, lowerThreshold, upperThreshold)

        // Criar uma imagem em Verde para destacar as bordas
        val redEdges = Mat.zeros(mat.size(), CvType.CV_8UC3)
        val redColor = Scalar(100.0, 125.0, 0.0) // VERDE em RGB
        redEdges.setTo(redColor, edgesMat)

        // Criar uma imagem em azul para destacar as bordas
        val blueEdges = Mat.zeros(mat.size(), CvType.CV_8UC3)
        val blueColor = Scalar(225.0, 80.0, 225.0) // azul em RGB
        blueEdges.setTo(blueColor, edgesMat)

        // Calcular o deslocamento para criar o efeito anaglifo
        val imageWidth = mat.cols()
        val displacementX = (imageWidth * 0.003).toInt() // Deslocamento horizontal
        val displacementY = 0

        // Criar uma matriz de transformação para deslocamento
        val shiftMatrix = Mat(2, 3, CvType.CV_32F)
        shiftMatrix.put(0, 0, 1.0, 0.0, displacementX.toDouble(),
            0.0, 1.0, displacementY.toDouble())

        // Aplicar o deslocamento à imagem azul
        val displacedBlueEdges = Mat()
        Imgproc.warpAffine(blueEdges, displacedBlueEdges, shiftMatrix, blueEdges.size(), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(0.0, 0.0, 0.0))

        // Criar a imagem final para o efeito anaglifo
        val anaglyphMat = Mat.zeros(mat.size(), CvType.CV_8UC3)

        // Mesclar as bordas vermelhas e deslocadas azuis na imagem final
        Core.addWeighted(redEdges, 1.0, displacedBlueEdges, 1.0, 0.0, anaglyphMat)

        // Aplicar as bordas coloridas na imagem final
        val finalMat = Mat.zeros(hsv.size(), CvType.CV_8UC3)
        hsv.copyTo(finalMat) // Copia a imagem original para a imagem final
        Core.addWeighted(finalMat, 1.0, anaglyphMat, 1.0, 0.0, finalMat) // Mescla a imagem original com as bordas coloridas


        // Liberar memória
        redEdges.release()
        blueEdges.release()
        displacedBlueEdges.release()
        shiftMatrix.release()

        return finalMat
    }


    //PRinta os frames processados
    private fun updatePreview(mat: Mat) {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)

        //Calcula a diferença de tamanho do Bitmap e TextureView para centralizlos
        val xOffset = (imageSize!!.width - bitmap.width) / 2f
        val yOffset = (imageSize!!.height - bitmap.height) / 2f

        // Atualiza o SurfaceTexture com a nova imagem
        cameraPreview.lockCanvas()?.let {
            // Desenha o Bitmap no Canvas do TextureView
            it.drawBitmap(bitmap, xOffset, yOffset, null)
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
        if (OpenCVLoader.initLocal()) {
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
            if (!ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.CAMERA), PERMISSION_CODE)
            } else {
                dialog("Permissão para Acesso à Câmera","Para utilizar todas as funcionalidades deste aplicativo, é necessário conceder permissão para acessar a câmera. \n" +
                        "Esta permissão permite capturar fotos, e a realização de outras ações essenciais para melhorar sua experiência.",::openSettings)
            }
        }
    }

    private fun checkPermissions() = REQUIRED_PERMISSIONS.all {
        log("Check Permissions")
        ContextCompat.checkSelfPermission(
            context, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }

    private fun dialog(title:String, desc:String, operation: () -> Unit){

        log("Start Dialog")

        val view = context.layoutInflater.inflate(R.layout.pop_up, null)
        val dialog = Dialog(context, R.style.TransparentRoundedLayout)
        dialog.setContentView(view)

        //Parametros para a janela pop_up

        val layoutParams = WindowManager.LayoutParams().apply {
            copyFrom(dialog.window?.attributes)
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        dialog.window?.attributes = layoutParams


        val pop_up_title:TextView = view.findViewById(R.id.title)

        pop_up_title.setText(title)

        val pop_up_desc:TextView = view.findViewById(R.id.desc1)

        pop_up_desc.setText(desc)

        val btnCancel:TextView = view.findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener{
            log("finish application")
            context.finish()
        }

        val btnPermitir:TextView = view.findViewById(R.id.btnPermitir)

        btnPermitir.setOnClickListener{
            log("Requesting Permission")
            operation()
            dialog.dismiss()
        }

        context.runOnUiThread{
            dialog.show()
        }
    }

    private fun log(s: String) {
        Log.d(TAG, s)
    }

    fun setFilter(e: EnumFilters) {
        tagFilter = e
    }

    fun setOrientation(e: EnumFilters){
        tagOrientaion = e
    }

    fun resume() {
        init()
        if (cameraPreview.isAvailable && !imageReader.surface.isValid) {
            log("Camera Preview Started")
            val surfaceTexture: SurfaceTexture? = cameraPreview.surfaceTexture
            surfaceTexture!!.setDefaultBufferSize(imageSize!!.width, imageSize!!.height)
            surface = Surface(surfaceTexture)
            log("SurfaceTexture Width: ${cameraPreview.width}, Height: ${cameraPreview.height}")
            log("Surface Created: ${surface.isValid}")
            getPermission()
        }
    }

    fun handlePermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            log("Permission: ${checkPermissions()}")
            dialog("Permissão","A permissão de utilização da camêra do dispositivo\" +\n" +
                    "                    \"é fundamental para o funcionament da aplicação!",::getPermission)
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