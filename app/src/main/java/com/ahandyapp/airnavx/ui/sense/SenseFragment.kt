package com.ahandyapp.airnavx.ui.sense

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.databinding.FragmentSenseBinding
import java.io.File
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.truncate


class SenseFragment : Fragment(), SensorEventListener {

    private val TAG = "SenseFragment"
    //////////////////
    // sensor listener
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val orientationDegrees = FloatArray(3)
    //////////////////
    private var soundMeter = SoundMeter()
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
//    private var recorder: MediaRecorder? = null
//    private var recorderStarted = false



    private lateinit var senseViewModel: SenseViewModel
    private var _binding: FragmentSenseBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        senseViewModel =
            ViewModelProvider(this).get(SenseViewModel::class.java)

        _binding = FragmentSenseBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textViewTitle: TextView = binding.textSense
        senseViewModel.textTitle.observe(viewLifecycleOwner, Observer {
            textViewTitle.text = it
        })

        val editTextAngle: EditText = binding.editCameraAngle
        senseViewModel.editCameraAngle.observe(viewLifecycleOwner, Observer {
            editTextAngle.text = it.toString().toEditable()
        })
        val editTextDecibel: EditText = binding.editDecibelLevel
        senseViewModel.editDecibelLevel.observe(viewLifecycleOwner, Observer {
            editTextDecibel.text = it.toString().toEditable()
        })

        editTextAngle.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                if (count > 0) {
                    val angleText = s.toString()
                    Log.d(TAG, "onTextChanged incoming angle = $angleText")
                    // constrain infinite loop!
                    if (!senseViewModel.editCameraAngle.value.toString().equals(angleText)) {
                        senseViewModel.editCameraAngle.value = angleText.toInt()
                    }
                    val viewModelText = senseViewModel.editCameraAngle.value.toString()
                    Log.d(TAG, "SenseViewModel angle = $viewModelText")
                }
            }
        })

        //////////////////
        // sensor listener
        //        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //////////////////

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    //////////////////
    // sensor listener
    override fun onResume() {
        super.onResume()

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL * 1000 * 1000,    // 3 secs
                SensorManager.SENSOR_DELAY_UI * 1000 * 1000       // 2 secs
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL * 1000 * 1000,
                SensorManager.SENSOR_DELAY_UI * 1000 * 1000
            )
        }
        if (isPermissionAudioGranted()) {
            // start sound meter
            this.context?.let { soundMeter.start(it) }
            Log.d(TAG, "onSensorChanged soundMeter started.")
        }
    }

    override fun onPause() {
        super.onPause()

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
        // stop sound meter
        soundMeter.stop()

    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            Log.d(TAG, "onSensorChanged accelerometerReading->" + accelerometerReading.contentToString())
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            Log.d(TAG, "onSensorChanged magnetometerReading->" + magnetometerReading.contentToString())
        }

        updateOrientationAngles()

        if (isPermissionAudioGranted()) {
            // get amplitude
            var amplitude = soundMeter.amplitude
//            var db = soundMeter.deriveDecibel(32767.0)
//            Log.d(TAG, "onSensorChanged soundMeter.deriveDecibel 32767->$amplitude, db->$db")
//            var db = soundMeter.deriveDecibel(65534.0)
//            Log.d(TAG, "onSensorChanged soundMeter.deriveDecibel 65534->$amplitude, db->$db")
//            var db = soundMeter.deriveDecibel(0.2)
//            var db = deriveDecibel(amplitude, 0.2)
            var db = deriveDecibel(amplitude, 2.7)
            Log.d(TAG, "onSensorChanged soundMeter.deriveDecibel 2.7->${amplitude.toString()}, db->${db.toString()}")
            if (db < 0.0) db = 0.0
            db = truncate(db)
            senseViewModel.editDecibelLevel.value = db
        }

    }
    fun deriveDecibel(amplitude: Double, ref:Double): Double {
        val maxAmplScaled: Double = amplitude / ref
//        val maxAmplScaled: Double = amplitude
        val db: Double = 20 * log10(maxAmplScaled)
        return db
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        // "orientationAngles" now has up-to-date information.

        //Log.d(TAG, "updateOrientationAngles rotationMatrix->" + rotationMatrix.contentToString())
        Log.d(TAG, "updateOrientationAngles orientationAngles->" + orientationAngles.contentToString())

        // convert orientation angles (radians) to degrees
        for ((index, angle) in orientationAngles.withIndex()) {
            orientationDegrees[index] = (orientationAngles[index] * (180/ PI)).toFloat()
//            orientationDegrees[index] = orientationAngles[index] * 57.2958f
        }
        Log.d(TAG, "updateOrientationAngles orientationDegrees->" + orientationDegrees.contentToString())

        // shift orientation degrees to camera angle - 0=parallel to earth, 90=perpendicular to earth
        senseViewModel.editCameraAngle.value = 90 + orientationDegrees[1].toInt()   // adjust neg angles to 0(parallel to earth) to 90(flat, straight up)
        //senseViewModel.editCameraAngle.value = (orientationDegrees[1].toInt() * -1)
        Log.d(TAG, "updateOrientationAngles editCameraAngle PITCH->" + senseViewModel.editCameraAngle.value)

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        updateOrientationAngles()
    }
    //////////////////////
    // permissions
    fun isPermissionAudioGranted(): Boolean {
        if (this.context?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO) }
            != PERMISSION_GRANTED
        ) {
            this.activity?.let {
                ActivityCompat.requestPermissions(
//                    it, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    it, arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_PERMISSION
                )
            }
        } else {
            Log.d(TAG, "onRequestPermissionAudio Manifest.permission.RECORD_AUDIO granted.")
            return true
        }
        Log.d(TAG, "onRequestPermissionAudio Manifest.permission.RECORD_AUDIO denied!")
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] == PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult Manifest.permission.RECORD_AUDIO granted.")
            } else {
                Log.d(TAG, "onRequestPermissionsResult Manifest.permission.RECORD_AUDIO denied!")
            }
        }
    }

//    //////////////////////////////////////
//    fun start(): Boolean {
//        val cacheDirectory: File? = this.requireContext().externalCacheDir
//        try {
//            if (recorder == null) {
//                recorder = MediaRecorder()
//                recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
//                recorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//                recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//                recorder!!.setOutputFile("${cacheDirectory}/test.3gp")
//                recorder!!.prepare()
////                Thread.sleep(10000)
//                recorder!!.start()
//                recorderStarted = true
//                return recorderStarted
//            }
//        }
//        catch (e: Exception) {
//            Log.e(TAG, "SoundMeter exception ${e.message}")
//        }
//        return recorderStarted
//    }
//
//    fun stop() {
//        if (recorder != null) {
//            recorder!!.stop()
//            recorder!!.release()
//            recorder = null
//        }
//    }
//
//    val amplitude: Double
//        get() = if (recorder != null) recorder!!.maxAmplitude.toDouble() else 0.0

}

