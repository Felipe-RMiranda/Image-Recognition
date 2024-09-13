package com.aps.imagerecognition.view

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.aps.imagerecognition.R

class ImagemGaleria(private val context:Activity) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val fragment_imagem_galeria = inflater.inflate(R.layout.fragment_imagem_galeria, container, false)

        val imageViews1:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView1)
        val imageViews2:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView2)
        val imageViews3:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView3)
        val imageViews4:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView4)
        val imageViews5:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView5)
        val imageViews6:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView6)
        val imageViews7:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView7)
        val imageViews8:ImageView = fragment_imagem_galeria.findViewById(R.id.imageView8)
        val btnCancel:TextView = fragment_imagem_galeria.findViewById(R.id.btnCancel2)



        return fragment_imagem_galeria
    }


}