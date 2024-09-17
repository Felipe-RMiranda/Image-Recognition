package com.aps.imagerecognition.control

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.ImageView
import com.aps.imagerecognition.control.EnumFilters.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class StaticProcessing(private val img:ImageView, private val preview:ImageView) {

    private val TAG = "Log Image Recognition"
    private var tagFilter: EnumFilters? = null

    init {
        openCV()
        processFrame()
    }

    //Processa a Imagem
    private fun processFrame() {
        val mat = convertToMat()
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
        val redColor = Scalar(225.0, 125.0, 0.0) // VERDE em RGB
        redEdges.setTo(redColor, edgesMat)

        // Criar uma imagem em azul para destacar as bordas
        val blueEdges = Mat.zeros(mat.size(), CvType.CV_8UC3)
        val blueColor = Scalar(0.0, 0.0, 225.0) // azul em RGB
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


    //Convertendo ImageView
    private fun convertToMat(): Mat {

        //Verifica se o ImageView tem um drawable válido
        val drawable = img.drawable as BitmapDrawable

        //Converte o Drawable em Bitmap
        val bitmap = drawable.bitmap

        //Converte para mat
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        return mat
    }

    //PRinta os frames processados
    private fun updatePreview(mat: Mat) {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        preview.setImageBitmap(bitmap)
    }

    private fun openCV(){
        if (OpenCVLoader.initLocal()){
            log("Start OpenCV")
        }
    }

    private fun log(s:String){
        Log.d(TAG, s)
    }

    fun setFilter(e: EnumFilters) {
        tagFilter = e
        processFrame()
    }
}