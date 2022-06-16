package com.ahandyapp.airnavx.ui.gallery

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ahandyapp.airnavx.model.AirCapture

class GalleryViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Gallery Fragment"
    }
    val text: LiveData<String> = _text

    lateinit var galleryImageView: ImageView

    lateinit var airCapture: AirCapture
    lateinit var captureBitmap: Bitmap  // original capture bitmap
    lateinit var zoomBitmap: Bitmap     // paired zoom bitmap
    lateinit var overBitmap: Bitmap     // paired zoom bitmap
//    lateinit var galleryBitmap: Bitmap  // gallery view image bitmap
}