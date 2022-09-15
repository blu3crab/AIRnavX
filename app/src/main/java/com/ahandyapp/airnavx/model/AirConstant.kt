package com.ahandyapp.airnavx.model

object AirConstant
{
    enum class ImageOrientation {
        PORTRAIT,
        LANDSCAPE
    }
    enum class MeasureDimension {
        HORIZONTAL,
        VERTICAL
    }
    enum class CraftOrientation {
        WINGSPAN,
        LENGTH
    }

    // pixel6:
    // distance for 100% FOV of 12" object for portrait & landscape
    // ratio = dist/12
    const val distFor100PerCentFOVof1FootObjectPixel6Portrait = 11.625    // 11 5/8"
    const val distRatioFor100PerCentFOVat1FootPixel6Landscape = 8.5       // 8 1/2"
    const val ratioFor100PerCentFOVat1FootPixel6Portrait = distFor100PerCentFOVof1FootObjectPixel6Portrait/12.0   // 0.96875
    const val ratioFor100PerCentFOVat1FootPixel6Landscape = distRatioFor100PerCentFOVat1FootPixel6Landscape/12.0  // 0.70833
    //val distFor100PerCentFOVat1FootPixel4 = 1.065     // 12 3/4"

    const val AIR_VERSION: String = "v1-27mar22"
    const val DEFAULT_STRING: String = "nada"
    const val DEFAULT_DOUBLE: Double = 0.0
    const val DEFAULT_INT: Int = 0
    val DEFAULT_FLOAT_ARRAY: FloatArray = floatArrayOf(0.0F, 0.0F)
    const val DEFAULT_FLOAT: Float = 0.0F

    const val DEFAULT_FILE_PREFIX = "AIR-"
    const val DEFAULT_DATAFILE_EXT = "json"
    const val DEFAULT_IMAGEFILE_EXT = "jpg"
    const val DEFAULT_ZOOM_SUFFIX = "-zoom"
    const val DEFAULT_OVER_SUFFIX = "-over"
    const val DEFAULT_EXTENSION_SEPARATOR = "."

    const val DEFAULT_CRAFTSPEC_NAME = "CraftSpec"

    const val SWIPE_MIN_DISTANCE = 120
    const val SWIPE_THRESHOLD_VELOCITY = 200

    const val REQUEST_IMAGE_CAPTURE = 1001
    const val REQUEST_SPOKEN_CRAFT_TAG = 1002

}
