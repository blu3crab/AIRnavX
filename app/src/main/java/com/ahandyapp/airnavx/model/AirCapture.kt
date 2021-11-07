package com.ahandyapp.airnavx.model

import android.telephony.CellLocation
import java.sql.Timestamp

data class AirCapture (
    val timestamp: String,
    val imagePath: String,
    val imageName: String,
//    val location: CellLocation,
    val decibel: Double,
    val cameraAngle: Int )
