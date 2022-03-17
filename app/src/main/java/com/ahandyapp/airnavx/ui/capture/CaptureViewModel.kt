package com.ahandyapp.airnavx.ui.capture

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ahandyapp.airnavx.model.AirCapture
import java.util.ArrayList

class CaptureViewModel : ViewModel() {

    // TODO: public MutableLiveData<var>
    ///////////////////////////////////////////////////////////////////////////
    // AirCapture enums, constants, defaults
    enum class AirFileType {
        IMAGE,
        DATA
    }

    val DEFAULT_DATAFILE_EXT = "json"
    val DEFAULT_IMAGEFILE_EXT = "jpg"
//    val DEFAULT_STRING = "nada"
//    val DEFAULT_DOUBLE = 0.0
//    val DEFAULT_INT = 0
//    val DEFAULT_FLOAT_ARRAY: FloatArray = floatArrayOf(0.0F, 0.0F)
//    val DEFAULT_FLOAT = 0.0F

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

    var fullBitmapArray = ArrayList<Bitmap>()
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