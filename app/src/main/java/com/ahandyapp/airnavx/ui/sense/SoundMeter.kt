///////////////////////////////////////////////////////////////////////////////
package com.ahandyapp.airnavx.ui.sense

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import kotlin.math.log10
import kotlin.math.truncate

class SoundMeter {
    // https://developer.android.com/guide/topics/media/mediarecorder
    // https://developer.android.com/reference/android/media/MediaRecorder
    // https://developer.android.com/reference/android/media/MediaRecorder#getMaxAmplitude()
    private val TAG = "SoundMeter"

    private var recorder: MediaRecorder? = null
    private var recorderStarted = false
    /////////////////////////
    // start sound meter
    fun start(context: Context): Boolean {
        val cacheDirectory: File? = context.externalCacheDir
        try {
            if (recorder == null) {
                Log.d(TAG, "SoundMeter.start starting MediaRecorder...")

                recorder = MediaRecorder()
                recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//                recorder!!.setOutputFile("/dev/null")
                recorder!!.setOutputFile("${cacheDirectory}/test.3gp")
                recorder!!.prepare()
//            Thread.sleep(1000)
                recorder!!.start()
                recorderStarted = true
                return recorderStarted
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "SoundMeter exception ${e.message}")
        }
        return recorderStarted
    }

    /////////////////////////
    // stop sound meter
    fun stop() {
        if (recorder != null) {
            recorder!!.stop()
            recorder!!.release()
            recorder = null
        }
    }
    /////////////////////////
    // get decibel
    fun deriveDecibel(forceFormat: Boolean): Double {
        val ref = 2.7
        val maxAmplScaled: Double = amplitude / ref
        var decibel: Double = 20 * log10(maxAmplScaled)
        if (forceFormat) {
            if (decibel < 0.0) decibel = 0.0
            decibel = truncate(decibel)
        }
        Log.d(TAG, "SoundMeter.deriveDecibel ref->$ref, amp->$amplitude, db->$decibel")
        return decibel
    }

    val amplitude: Double
        get() = if (recorder != null) recorder!!.maxAmplitude.toDouble() else 0.0

    val amp: Int
        get() = if (recorder != null) recorder!!.maxAmplitude else 0
}
///////////////////////////////////////////////////////////////////////////////
