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
import android.widget.Toast


class ImagemGaleria() : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val fragment_imagem_galeria = inflater.inflate(R.layout.fragment_imagem_galeria, container, false)

        val imageViews = listOf<ImageView>(
            fragment_imagem_galeria.findViewById(R.id.imageView1),
            fragment_imagem_galeria.findViewById(R.id.imageView2),
            fragment_imagem_galeria.findViewById(R.id.imageView3),
            fragment_imagem_galeria.findViewById(R.id.imageView4),
            fragment_imagem_galeria.findViewById(R.id.imageView5),
            fragment_imagem_galeria.findViewById(R.id.imageView6),
            fragment_imagem_galeria.findViewById(R.id.imageView7),
            fragment_imagem_galeria.findViewById(R.id.imageView8)
        )

        imageViews.forEach { imageView ->
            imageView.setOnClickListener {
                imgProcessing(imageView)
            }
        }

        val btnCancel: TextView = fragment_imagem_galeria.findViewById(R.id.btnCancel2)
        btnCancel.setOnClickListener {
            activity?.finish()
        }

        return fragment_imagem_galeria
    }

    private fun imgProcessing(imageView: ImageView) {

        val imgProcess = ImgProcess(imageView)

        val fragment = parentFragmentManager
        val transaction = fragment.beginTransaction()
        transaction.replace(R.id.main, imgProcess).commit()
    }
}


