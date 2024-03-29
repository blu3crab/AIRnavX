package com.ahandyapp.airnavx.model

import android.content.Context
import android.os.Environment
import android.util.Log
import com.ahandyapp.airnavx.model.AirConstant.DEFAULT_CRAFTSPEC_NAME
import com.ahandyapp.airnavx.model.AirConstant.DEFAULT_DATAFILE_EXT
import com.ahandyapp.airnavx.model.AirConstant.DEFAULT_EXTENSION_SEPARATOR
import com.ahandyapp.airnavx.model.AirConstant.DEFAULT_FILE_PREFIX
import com.google.gson.Gson
import java.io.File
import java.io.IOException

data class CraftSpec(
//    var type: String,
    var typeInx: Int = 0,
    var typeList: ArrayList<String> = arrayListOf("C172", "PA28", "PA34", "PC12", "GOAL"),

    var craftDimsC172: CraftDims = CraftDims(craftType = "C172", wingspan = 36.0, length = 27.17),
    var craftDimsPA28: CraftDims = CraftDims(craftType = "PA28", wingspan = 28.22, length = 21.72),
    var craftDimsPA34: CraftDims = CraftDims(craftType = "PA34", wingspan = 38.9, length = 27.58),
    var craftDimsPC12: CraftDims = CraftDims(craftType = "PC12", wingspan = 53.4, length = 47.25),
    var craftDimsGoal: CraftDims = CraftDims(craftType = "GOAL", wingspan = 24.0, length = 8.0),
//    var craftDimsMoon: CraftDims = CraftDims(craftType = "MOON", wingspan = 11400048.0, length = 11400048.0),

    var dimsList: ArrayList<CraftDims> = arrayListOf(
        craftDimsC172,
        craftDimsPA28,
        craftDimsPA34,
        craftDimsPC12,
        craftDimsGoal),
        //, craftDimsMoon),

    var craftTagC172List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "N2621Z", "N20283", "New(type)", "New(speak)"),
    var craftTagPA28List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "N21803", "N38657", "New(type)", "New(speak)"),
    var craftTagPA34List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "N142GD", "N12345", "New(type)", "New(speak)"),
    var craftTagPC12List: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "UNKNOWN", "UNKNOWN", "New(type)", "New(speak)"),
    var craftTagGoalList: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "UNKNOWN", "UNKNOWN", "New(type)", "New(speak)"),
//    var craftTagGoalMoon: java.util.ArrayList<String> = arrayListOf("UNKNOWN", "UNKNOWN", "UNKNOWN", "New(type)", "New(speak)"),

    var tagInx: Int = 0,
    var tagList: ArrayList<ArrayList<String>> = arrayListOf(
        craftTagC172List,
        craftTagPA28List,
        craftTagPA34List,
        craftTagPC12List,
        craftTagGoalList)
        //, craftTagGoalMoon)
)
{
    private val TAG = "CraftSpec"

    fun syncTypeTag(craftType: String, craftTag: String): Boolean {
        // scan for craft type & assign type index
        this.typeInx = 0
        var typeInx = 0
        for (type in this.typeList) {
            if (type == craftType) {
                this.typeInx = typeInx
                continue
            }
            ++typeInx
        }
        Log.d(TAG, "syncTypeTag aircraft type ${this.typeInx}, " +
                "${this.typeList[this.typeInx]}...")

        // scan for craft tag & assign tag index
        this.tagInx = 0
        val tagInx = 0
        for (tag in this.tagList[this.typeInx]) {
            if (tag == craftTag) {
                this.tagInx = tagInx
            }
        }
        Log.d(TAG, "syncTypeTag aircraft tag ${this.tagInx}, " +
                "${this.tagList[this.tagInx]}...")

        return true
    }

    fun readFromJson(context: Context): CraftSpec {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val filename = DEFAULT_FILE_PREFIX + DEFAULT_CRAFTSPEC_NAME + DEFAULT_EXTENSION_SEPARATOR + DEFAULT_DATAFILE_EXT
        val filePath = storageDir.toString() + File.separator + filename
        Log.d(TAG, "readFromJson filename $filename...")
        try {
            val craftSpecFile = File(filePath)
            Log.d(TAG, "readFromJson File filePath $filePath")
            //   extract CraftSpec json string
            var jsonString = AirConstant.DEFAULT_STRING
            try {
                jsonString = craftSpecFile.bufferedReader().use { it.readText() }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
            } finally {
                craftSpecFile.bufferedReader().close()
            }
            Log.d(TAG, "fromJson craftSpec json $jsonString...")
            // decode json string into craftSpec
            val craftSpec = Gson().fromJson(jsonString, CraftSpec::class.java)
            Log.d(TAG, "fromJson CraftSpec $craftSpec")
            return craftSpec
        } catch (ioException: IOException) {
            ioException.printStackTrace()
        }
        // exceptions occurred - return initialized object
        val craftSpec = CraftSpec()
        return craftSpec
    }

    fun writeToJson(context: Context, craftSpec: CraftSpec): Boolean {
        try {
            // transform CraftSpec data class to json
            val jsonCapture = Gson().toJson(craftSpec)
            Log.d(TAG, "writeToJson $jsonCapture")

            // format CraftSpec name & write json file
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val name = DEFAULT_FILE_PREFIX + DEFAULT_CRAFTSPEC_NAME + DEFAULT_EXTENSION_SEPARATOR + DEFAULT_DATAFILE_EXT
            Log.d(TAG, "writeToJson storageDir->$storageDir, name->$name")
            File(storageDir, name).printWriter().use { out ->
                out.println(jsonCapture)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "writeToJson Exception ${ex.stackTrace}")
            return false
        }
        return true
    }

}
