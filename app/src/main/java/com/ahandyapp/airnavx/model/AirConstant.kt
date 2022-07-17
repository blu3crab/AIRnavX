package com.ahandyapp.airnavx.model

object AirConstant
{
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

    // pixel6:
    // distance for 100% FOV of 12" object for portrait & landscape
    // ratio = dist/12
    val distFor100PerCentFOVof1FootObjectPixel6Portrait = 11.625    // 11 5/8"
    val distRatioFor100PerCentFOVat1FootPixel6Landscape = 8.5       // 8 1/2"
    val ratioFor100PerCentFOVat1FootPixel6Portrait = distFor100PerCentFOVof1FootObjectPixel6Portrait/12.0   // 0.96875
    val ratioFor100PerCentFOVat1FootPixel6Landscape = distRatioFor100PerCentFOVat1FootPixel6Landscape/12.0  // 0.70833
    //val distFor100PerCentFOVat1FootPixel4 = 1.065     // 12 3/4"

    val AIR_VERSION: String = "v1-27mar22"
    val DEFAULT_STRING: String = "nada"
    val DEFAULT_DOUBLE: Double = 0.0
    val DEFAULT_INT: Int = 0
    val DEFAULT_FLOAT_ARRAY: FloatArray = floatArrayOf(0.0F, 0.0F)
    val DEFAULT_FLOAT: Float = 0.0F

    val DEFAULT_FILE_PREFIX = "AIR-"
    val DEFAULT_DATAFILE_EXT = "json"
    val DEFAULT_IMAGEFILE_EXT = "jpg"
    val DEFAULT_ZOOM_SUFFIX = "-zoom"
    val DEFAULT_OVER_SUFFIX = "-over"
    val DEFAULT_EXTENSION_SEPARATOR = "."

    val DEFAULT_CRAFTSPEC_NAME = "CraftSpec"

    val SWIPE_MIN_DISTANCE = 120
    val SWIPE_THRESHOLD_VELOCITY = 200

    val REQUEST_IMAGE_CAPTURE = 1001
    val REQUEST_SPOKEN_CRAFT_TAG = 1002

}
