package com.ahandyapp.airnavx.ui.sense

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.databinding.FragmentSenseBinding
import java.util.*
import kotlin.math.PI

class SenseFragment : Fragment(), SensorEventListener {

    private val TAG = "SenseFragment"
    //////////////////
    // sensor listener
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    //////////////////

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val orientationDegrees = FloatArray(3)


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
    }

    override fun onPause() {
        super.onPause()

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
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

//        val axisX: Float = event.values[0]
//        val axisY: Float = event.values[1]
//        val axisZ: Float = event.values[2]

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

        for ((index, angle) in orientationAngles.withIndex()) {
            orientationDegrees[index] = (orientationAngles[index] * (180/ PI)).toFloat()
            orientationDegrees[index] = orientationAngles[index] * 57.2958f
        }
        Log.d(TAG, "updateOrientationAngles orientationDegrees->" + orientationDegrees.contentToString())

        senseViewModel.editCameraAngle.value = 90 + orientationDegrees[1].toInt()   // adjust neg angles to 0(parallel to earth) to 90(flat, straight up)
        //senseViewModel.editCameraAngle.value = (orientationDegrees[1].toInt() * -1)
        Log.d(TAG, "updateOrientationAngles editCameraAngle PITCH->" + senseViewModel.editCameraAngle.value)

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
//        updateOrientationAngles()
    }



}

