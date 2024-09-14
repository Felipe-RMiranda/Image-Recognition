package com.aps.imagerecognition.control

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aps.imagerecognition.R
import com.aps.imagerecognition.view.ImagemGaleria

class GaleriaPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_galeria_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val galeria_page_fragment = ImagemGaleria()
        val fragment_manager = supportFragmentManager
        val fragment_transation = fragment_manager.beginTransaction()


        fragment_transation.add(R.id.main, galeria_page_fragment).commit()


        }
}

