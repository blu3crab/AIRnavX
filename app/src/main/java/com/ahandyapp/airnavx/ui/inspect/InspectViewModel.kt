package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InspectViewModel : ViewModel() {

    enum class ZoomDirection(val direction: Int) {
        IN(-1),
        OUT(1)
    }
    enum class ImageOrientation(val orientation: Int) {
        PORTRAIT(0),
        LANDSCAPE(1)
    }

    var ouiWidth = 0
    var ouiHeight = 0

    private val _guideText = MutableLiveData<String>().apply {
        value = "TAP to zoom IN, \nDOUBLE TAP to zoom OUT, \nLONG PRESS to CENTER."
    }
    val guideText: LiveData<String> = _guideText

    private val _ouiSizeText = MutableLiveData<String>().apply {
        value = "Object Under Inspection \nwidth $ouiWidth x height $ouiHeight"
    }
    val ouiSizeText: LiveData<String> = _ouiSizeText

}