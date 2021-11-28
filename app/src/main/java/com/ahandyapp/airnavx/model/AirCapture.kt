package com.ahandyapp.airnavx.model


data class AirCapture(
    var timestamp: String,
    var imagePath: String,
    var imageName: String,
    var imageWidth: Int,
    var imageHeight: Int,
    var decibel: Double,
    var cameraAngle: Int,
    var exifOrientation: Int,
    var exifRotation: Int,
    var exifLatLon: FloatArray,
    var exifAltitude: Double,
    var exifLength: Int,
    var exifWidth: Int,
    var airObjectPixelSize: Float,
    var airObjectDistance: Float,
    var airObjectAltitude: Float,
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
