package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InspectViewModel : ViewModel() {
    ///////////////ENUM////////////////
    enum class ZoomDirection(val direction: Int) {
        IN(-1),
        OUT(1)
    }
    enum class ImageOrientation(val orientation: Int) {
        PORTRAIT(0),
        LANDSCAPE(1)
    }

    enum class MeasureDimension(val dimension: String) {
        HORIZONTAL("HORIZONTAL"),
        VERTICAL("VERTICAL")
    }
    enum class CraftOrientation(val orientation: String) {
        WINGSPAN("CRAFT WINGSPAN"),
        LENGTH("CRAFT LENGTH")
    }

    ///////////////////LIVE DATA////////////////////////
    private val _guideText = MutableLiveData<String>().apply {
        value = "TAP to zoom IN, \nDOUBLE TAP to zoom OUT, \nLONG PRESS to CENTER."
    }
    val guideText: LiveData<String> = _guideText

    var measureDimension = MeasureDimension.HORIZONTAL
    var zoomWidth = 0
    var zoomHeight = 0

    var craftOrientation = CraftOrientation.WINGSPAN

    private var _measureDimensionText = MutableLiveData<String>().apply {
        value = MeasureDimension.HORIZONTAL.toString() + "(" + zoomWidth.toString() + " pixels)"
        if (measureDimension == MeasureDimension.VERTICAL) {
            value = MeasureDimension.VERTICAL.toString() + "(" + zoomHeight.toString() + " pixels)"
        }
    }
    var measureDimensionText: LiveData<String> = _measureDimensionText

    var ouiWidth = 0
    var ouiHeight = 0

    private val _ouiSizeText = MutableLiveData<String>().apply {
        value = "Object Under Inspection \nwidth $ouiWidth x height $ouiHeight"
    }
    val ouiSizeText: LiveData<String> = _ouiSizeText

}