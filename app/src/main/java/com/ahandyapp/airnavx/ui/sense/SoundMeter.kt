package com.ahandyapp.airnavx.ui.sense

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import kotlin.math.log10

class SoundMeter {
    private val TAG = "SoundMeter"
    private var recorder: MediaRecorder? = null
    private var recorderStarted = false
    fun start(context: Context): Boolean {
        val cacheDirectory: File? = context.externalCacheDir
        try {
            if (recorder == null) {
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

    fun stop() {
        if (recorder != null) {
            recorder!!.stop()
            recorder!!.release()
            recorder = null
        }
    }

    val amplitude: Double
        get() = if (recorder != null) recorder!!.maxAmplitude.toDouble() else 0.0

    val amp: Int
        get() = if (recorder != null) recorder!!.maxAmplitude else 0

//    fun deriveDecibel(ref:Double): Double {
//        val maxAmplScaled: Double = amplitude / ref
////        val maxAmplScaled: Double = amplitude
//        val db: Double = 20 * log10(maxAmplScaled)
//        return db
//    }
}