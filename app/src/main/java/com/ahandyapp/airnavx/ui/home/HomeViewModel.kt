package com.ahandyapp.airnavx.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

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
}