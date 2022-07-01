package com.ahandyapp.airnavx.model

import android.util.Log
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import com.google.gson.Gson
import java.io.File
import java.io.IOException

data class AirCapture(
    var version: String = AirConstant.AIR_VERSION,
    var timestamp: String = AirConstant.DEFAULT_STRING,
    var imagePath: String = AirConstant.DEFAULT_STRING,
    var imageName: String = AirConstant.DEFAULT_STRING,
    var imageWidth: Int = AirConstant.DEFAULT_INT,
    var imageHeight: Int = AirConstant.DEFAULT_INT,

    var decibel: Double = AirConstant.DEFAULT_DOUBLE,
    var cameraAngle: Int = AirConstant.DEFAULT_INT,

    var exifOrientation: Int = AirConstant.DEFAULT_INT,
    var exifRotation: Int = AirConstant.DEFAULT_INT,
    var exifLatLon: FloatArray = AirConstant.DEFAULT_FLOAT_ARRAY,
    var exifAltitude: Double = AirConstant.DEFAULT_DOUBLE,
    var exifLength: Int = AirConstant.DEFAULT_INT,
    var exifWidth: Int = AirConstant.DEFAULT_INT,

    var craftOrientation: AirConstant.CraftOrientation = AirConstant.CraftOrientation.WINGSPAN,
    var measureDimension: AirConstant.MeasureDimension = AirConstant.MeasureDimension.HORIZONTAL,
    var zoomWidth: Int = AirConstant.DEFAULT_INT,
    var zoomHeight: Int = AirConstant.DEFAULT_INT,

    var airObjectDistance: Double = AirConstant.DEFAULT_DOUBLE,
    var airObjectAltitude: Double = AirConstant.DEFAULT_DOUBLE,

    var craftTag: String = AirConstant.DEFAULT_STRING,
    var craftType: String = AirConstant.DEFAULT_STRING,
    var craftWingspan: Double = AirConstant.DEFAULT_DOUBLE,
    var craftLength: Double = AirConstant.DEFAULT_DOUBLE,

    var xtra1: String = AirConstant.DEFAULT_STRING,
    var xtra2: String = AirConstant.DEFAULT_STRING,
    ) {

    private val TAG = "AirCapture"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AirCapture

        if (timestamp != other.timestamp) return false
        if (imagePath != other.imagePath) return false
        if (imageName != other.imageName) return false
        if (decibel != other.decibel) return false
        if (cameraAngle != other.cameraAngle) return false
        if (exifOrientation != other.exifOrientation) return false
        if (exifRotation != other.exifRotation) return false
        if (!exifLatLon.contentEquals(other.exifLatLon)) return false
        if (airObjectDistance != other.airObjectDistance) return false
        if (airObjectAltitude != other.airObjectAltitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + imageName.hashCode()
        result = 31 * result + decibel.hashCode()
        result = 31 * result + cameraAngle
        result = 31 * result + exifOrientation
        result = 31 * result + exifRotation
        result = 31 * result + exifLatLon.contentHashCode()
        result = 31 * result + airObjectDistance.hashCode()
        result = 31 * result + airObjectAltitude.hashCode()
        return result
    }

    fun getAirFilename(type: CaptureViewModel.AirFileType, captureTimestamp : String): String {
        var airFilename = AirConstant.DEFAULT_STRING
        if (type == CaptureViewModel.AirFileType.IMAGE) {
            airFilename = AirConstant.DEFAULT_FILE_PREFIX + captureTimestamp  + AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT

        }
        else if (type == CaptureViewModel.AirFileType.DATA) {
            airFilename = AirConstant.DEFAULT_FILE_PREFIX + captureTimestamp + AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_DATAFILE_EXT
        }
        return airFilename
    }

    fun read(airCaptureFile: File): AirCapture {
        //   extract aircapture json string
        var jsonString = AirConstant.DEFAULT_STRING
        try {
            jsonString = airCaptureFile.bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        } finally {
            airCaptureFile.bufferedReader().close()
        }
        Log.d(TAG, "fetchViewModel airCapture json $jsonString...")
        // decode json string into airCapture
        val airCapture = Gson().fromJson(jsonString, AirCapture::class.java)
        Log.d(TAG, "fetchViewModel AirCapture $airCapture")
        return airCapture
    }

    fun write(storageDir: File, timestamp: String, airCapture: AirCapture): Boolean {
        try {
            // transform AirCapture data class to json
            val jsonCapture = Gson().toJson(airCapture)
            Log.d(TAG, "recordAirCapture $jsonCapture")

            // format AirCapture name & write json file
            val name = getAirFilename(CaptureViewModel.AirFileType.DATA, timestamp)
            Log.d(TAG, "recordAirCapture storageDir->$storageDir, name->$name")
            File(storageDir, name).printWriter().use { out ->
                out.println(jsonCapture)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "recordAirCapture Exception ${ex.stackTrace}")
            return false
        }
        return true
    }

}
