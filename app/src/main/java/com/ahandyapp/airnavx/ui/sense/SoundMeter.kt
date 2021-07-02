package com.ahandyapp.airnavx.ui.sense

import android.media.MediaRecorder

class SoundMeter {
    private var mRecorder: MediaRecorder? = null
    fun start() {
        if (mRecorder == null) {
            mRecorder = MediaRecorder()
            mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mRecorder!!.setOutputFile("/dev/null")
            mRecorder!!.prepare()
            mRecorder!!.start()
        }
    }

    fun stop() {
        if (mRecorder != null) {
            mRecorder!!.stop()
            mRecorder!!.release()
            mRecorder = null
        }
    }

    val amplitude: Double
        get() = if (mRecorder != null) mRecorder!!.maxAmplitude.toDouble() else 0.0
}