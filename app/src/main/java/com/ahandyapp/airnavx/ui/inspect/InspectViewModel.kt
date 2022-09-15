package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ahandyapp.airnavx.model.AirConstant

class InspectViewModel : ViewModel() {
    ///////////////ENUM////////////////
    enum class ZoomDirection {
        IN,
        OUT
    }

    ///////////////////MODEL DATA////////////////////////
    private val _guideText = MutableLiveData<String>().apply {
        value = "ZOOM & CENTER so craft fills view.\n" +
                "LONG PRESS to CENTER.\n" +
                "TAP to zoom IN, DOUBLE TAP to zoom OUT, \n" +
                "Select measurement settings - Measure Altitude!"
    }
    val guideText: LiveData<String> = _guideText

    var imageOrientation = AirConstant.ImageOrientation.PORTRAIT

    var measureDimension = AirConstant.MeasureDimension.HORIZONTAL
    var zoomWidth = 0
    var zoomHeight = 0

    var craftOrientation = AirConstant.CraftOrientation.WINGSPAN

}