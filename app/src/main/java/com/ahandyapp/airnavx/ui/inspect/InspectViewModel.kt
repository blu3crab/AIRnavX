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
        value = "TAP to zoom IN, \nDOUBLE TAP to zoom OUT, \nLONG PRESS to CENTER.\nZOOM & CENTER so craft fills view.\nSelect measurement settings.\nMeasure Altitude!"
        // TODO: value = R.id.guide_text
    }
    val guideText: LiveData<String> = _guideText

    var imageOrientation = AirConstant.ImageOrientation.PORTRAIT

    var measureDimension = AirConstant.MeasureDimension.HORIZONTAL
    var zoomWidth = 0
    var zoomHeight = 0

    var craftOrientation = AirConstant.CraftOrientation.WINGSPAN

    var craftDimListInx = 0

    var craftDimsC172 = CraftDims(craftType = "C172", wingspan = 36.0, length = 27.17)
    var craftDimsPA28 = CraftDims(craftType = "PA28", wingspan = 28.22, length = 21.72)
    var craftDimsPA34 = CraftDims(craftType = "PA34", wingspan = 38.9, length = 27.58)
    var craftDimsList: ArrayList<CraftDims> = arrayListOf(craftDimsC172, craftDimsPA28, craftDimsPA34)

    var craftIdentListInx = 0

    var craftIdentC172List: ArrayList<String> = arrayListOf("UNKNOWN", "N2621Z", "N20283", "N5215E")
    var craftIdentPA28List: ArrayList<String> = arrayListOf("UNKNOWN", "N21803", "N38657", "N8445S")
    var craftIdentPA34List: ArrayList<String> = arrayListOf("UNKNOWN", "N142GD")

}