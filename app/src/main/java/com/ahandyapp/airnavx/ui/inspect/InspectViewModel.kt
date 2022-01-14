package com.ahandyapp.airnavx.ui.inspect

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InspectViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Inspect Fragment"
    }
    val text: LiveData<String> = _text
}