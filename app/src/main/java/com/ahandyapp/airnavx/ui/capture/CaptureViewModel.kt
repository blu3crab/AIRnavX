package com.ahandyapp.airnavx.ui.capture

import android.graphics.Bitmap
import android.widget.GridView
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ahandyapp.airnavx.model.AirCapture
import java.util.ArrayList

class CaptureViewModel : ViewModel() {
    ///////////////////////////////////////////////////////////////////////////
    // AirCapture enums, constants, defaults
    enum class AirFileType {
        IMAGE,
        DATA
    }

    //<GridView
    //android:id="@+id/gridView"
    //android:layout_width="324dp"
    //android:layout_height="223dp"
    val DEFAULT_BLANK_GRID_WIDTH = 160     // trunc(324 / 2) for 2 column gird
    val DEFAULT_BLANK_GRID_HEIGHT = 223    // 223

    val THUMB_SCALE_FACTOR = 5

    ///////////////////////////////////////////////////////////////////////////
    // gridView
    var gridCount = 0
    var gridPosition = 0

    var gridBitmapArray = ArrayList<Bitmap>()
    var gridLabelArray = ArrayList<String>()

    lateinit var imageViewPreview: ImageView       // preview image display
    lateinit var gridView: GridView

    var origBitmapArray = ArrayList<Bitmap>()
    var zoomBitmapArray = ArrayList<Bitmap>()
    var zoomDirtyArray = ArrayList<Boolean>()
    var overBitmapArray = ArrayList<Bitmap>()
    var airCaptureArray = ArrayList<AirCapture>()
    ///////////////////////////////////////////////////////////////////////////
    // live data
    private val _text = MutableLiveData<String>().apply {
        value = "Image Capture Preview"
    }
    var text: LiveData<String> = _text

    private val _decibel = MutableLiveData<String>().apply {
        value = "--- dB"
    }
    val decibel: LiveData<String> = _decibel

    private val _angle = MutableLiveData<String>().apply {
        value = "--- degrees"
    }
    val angle: LiveData<String> = _angle
    ///////////////////////////////////////////////////////////////////////////
}