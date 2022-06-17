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

    val SWIPE_MIN_DISTANCE = 120
    val SWIPE_THRESHOLD_VELOCITY = 200

}
