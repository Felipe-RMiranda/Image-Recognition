package com.aps.imagerecognition.model

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class Application: Application() {
    override fun onCreate() {
        super.onCreate()
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded")
        }
    }
}