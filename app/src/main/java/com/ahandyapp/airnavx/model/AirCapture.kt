package com.ahandyapp.airnavx.model

//val DEFAULT_STRING = "nada"
//val DEFAULT_DOUBLE = 0.0
//val DEFAULT_INT = 0
//val DEFAULT_FLOAT_ARRAY: FloatArray = floatArrayOf(0.0F, 0.0F)
//val DEFAULT_FLOAT = 0.0F
//
data class AirCapture(
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
//    var airObjectPixelSize: Float = AirConstant.DEFAULT_FLOAT,
    var airObjectPixelSize: Double = AirConstant.DEFAULT_DOUBLE,
    var airObjectDistance: Float = AirConstant.DEFAULT_FLOAT,
    var airObjectAltitude: Float = AirConstant.DEFAULT_FLOAT
) {

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
        if (airObjectPixelSize != other.airObjectPixelSize) return false
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
        result = 31 * result + airObjectPixelSize.hashCode()
        result = 31 * result + airObjectDistance.hashCode()
        result = 31 * result + airObjectAltitude.hashCode()
        return result
    }

}
