package com.ahandyapp.airnavx.model

import android.util.Log
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import com.google.gson.Gson
import java.io.File
import java.io.IOException

class AirCaptureJson {

    private val TAG = "AirCaptureJson"

    public fun getAirFilename(type: CaptureViewModel.AirFileType, captureTimestamp : String): String {
        //var airFilename = captureViewModel.DEFAULT_STRING
        var airFilename = AirConstant.DEFAULT_STRING
        if (type == CaptureViewModel.AirFileType.IMAGE) {
//            airFilename = "AIR-" + captureTimestamp + "." + AirConstant.DEFAULT_IMAGEFILE_EXT
            airFilename = AirConstant.DEFAULT_FILE_PREFIX + captureTimestamp + "." + AirConstant.DEFAULT_IMAGEFILE_EXT

        }
        else if (type == CaptureViewModel.AirFileType.DATA) {
            airFilename = AirConstant.DEFAULT_FILE_PREFIX + captureTimestamp + "." + AirConstant.DEFAULT_DATAFILE_EXT
        }
        return airFilename
    }

    public fun read(airCaptureFile: File): AirCapture {
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

    public fun write(storageDir: File, timestamp: String, airCapture: AirCapture): Boolean {
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