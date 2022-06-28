package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ahandyapp.airnavx.model.AirConstant
import com.ahandyapp.airnavx.model.CraftDims
import java.util.ArrayList

class InspectViewModel : ViewModel() {
    ///////////////ENUM////////////////
    enum class ZoomDirection(val direction: Int) {
        IN(-1),
        OUT(1)
    }
//    enum class ImageOrientation(val orientation: Int) {
//        PORTRAIT(0),
//        LANDSCAPE(1)
//    }
//
//    enum class MeasureDimension(val dimension: String) {
//        HORIZONTAL("HORIZONTAL"),
//        VERTICAL("VERTICAL")
//    }
//    enum class CraftOrientation(val orientation: String) {
//        WINGSPAN("CRAFT WINGSPAN"),
//        LENGTH("CRAFT LENGTH")
//    }

    ///////////////////MODEL DATA////////////////////////
    private val _guideText = MutableLiveData<String>().apply {
        value = "ZOOM & CENTER so craft fills view.\n" +
                "LONG PRESS to CENTER.\n" +
                "TAP to zoom IN, DOUBLE TAP to zoom OUT, \n" +
                "Select measurement settings - Measure Altitude!"
        // TODO: value = R.id.guide_text
    }
    val guideText: LiveData<String> = _guideText

    var imageOrientation = AirConstant.ImageOrientation.PORTRAIT

    var measureDimension = AirConstant.MeasureDimension.HORIZONTAL
    var zoomWidth = 0
    var zoomHeight = 0

    var craftOrientation = AirConstant.CraftOrientation.WINGSPAN

}