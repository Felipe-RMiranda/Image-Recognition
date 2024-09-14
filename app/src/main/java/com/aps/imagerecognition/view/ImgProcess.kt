package com.aps.imagerecognition.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.aps.imagerecognition.control.EnumFilters.*
import com.aps.imagerecognition.R
import com.aps.imagerecognition.control.StaticProcessing


class ImgProcess(private val img: ImageView): Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_img_process, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val img: ImageView = view.findViewById(R.id.preview)
        val previewP: ImageView = view.findViewById(R.id.previewP)
        val gry:ImageView = view.findViewById(R.id.gry)
        val hel:ImageView = view.findViewById(R.id.hel)
        val hsv:ImageView = view.findViewById(R.id.hsv)
        val ced:ImageView = view.findViewById(R.id.ced)
        val rgb:ImageView = view.findViewById(R.id.rgb)
        val close:ImageView = view.findViewById(R.id.close)

        val imgGaleria = ImagemGaleria()
        val fragment = parentFragmentManager
        val transaction = fragment.beginTransaction()
        var selectedButton: ImageView? = null

        val filterMap = mapOf(
            gry to GRY,
            hel to HEL,
            hsv to HSV,
            ced to CED,
            rgb to RGB,
            close to null
        )

        img.setImageDrawable(this.img.drawable)

        val staticProcess = StaticProcessing(this.img, previewP)

        filterMap.forEach { (button, filter) ->

            if ( button != close) {

                button.setOnClickListener {
                    if (button != rgb) {
                        rgb.setImageResource(R.drawable.camera)
                    }
                    selectedButton?.setImageResource(R.drawable.camera)
                    button.setImageResource(R.drawable.set_filter)
                    selectedButton = button
                    staticProcess.setFilter(filter!!)
                }
            } else {
                button.setOnClickListener {
                    transaction.replace(R.id.main, imgGaleria).commit()
                }
            }
        }


    }
}