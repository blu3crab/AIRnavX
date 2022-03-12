package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ahandyapp.airnavx.model.CraftDims
import java.util.ArrayList

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

//    enum class CraftType(val crafttype: String) {
//        C172("C172"),
//        P28("P28"),
//        P44("P44")
//    }

    ///////////////////MODEL DATA////////////////////////
    private val _guideText = MutableLiveData<String>().apply {
        value = "TAP to zoom IN, \nDOUBLE TAP to zoom OUT, \nLONG PRESS to CENTER.\nSelect measurement settings.\nMeasure Altitude!"
        // TODO: value = R.id.guide_text
    }
    val guideText: LiveData<String> = _guideText

    var measureDimension = MeasureDimension.HORIZONTAL
    var zoomWidth = 0
    var zoomHeight = 0

    var craftOrientation = CraftOrientation.WINGSPAN

    var craftDimListInx = 0

    var craftDimsC172 = CraftDims(craftType = "C172", wingspan = 36.0, length = 27.17)
    var craftDimsPA28 = CraftDims(craftType = "PA-28", wingspan = 28.22, length = 21.72)
    var craftDimsPA44 = CraftDims(craftType = "PA-44", wingspan = 39.0, length = 27.26)
    var craftDimsList: ArrayList<CraftDims> = arrayListOf(craftDimsC172, craftDimsPA28, craftDimsPA44)

//    var ouiWidth = 0
//    var ouiHeight = 0
//
//    private val _ouiSizeText = MutableLiveData<String>().apply {
//        value = "Object Under Inspection \nwidth $ouiWidth x height $ouiHeight"
//    }
//    val ouiSizeText: LiveData<String> = _ouiSizeText

}