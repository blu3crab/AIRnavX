package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InspectViewModel : ViewModel() {

    enum class ZoomDirection(val direction: Int) {
        IN(-1),
        OUT(1)
    }

    private val _text = MutableLiveData<String>().apply {
        value = "Tap on object, size shape."
    }
    val text: LiveData<String> = _text
}