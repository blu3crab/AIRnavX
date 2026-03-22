package com.ahandyapp.airnavx.ui.inspect

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.databinding.FragmentInspectBinding
import com.ahandyapp.airnavx.model.AirCapture
import com.ahandyapp.airnavx.model.AirConstant
import com.ahandyapp.airnavx.model.AirConstant.SWIPE_MIN_DISTANCE
import com.ahandyapp.airnavx.model.AirConstant.SWIPE_THRESHOLD_VELOCITY
import com.ahandyapp.airnavx.model.AirConstant.ratioFor100PerCentFOVat1FootPixel6Landscape
import com.ahandyapp.airnavx.model.AirConstant.ratioFor100PerCentFOVat1FootPixel6Portrait
import com.ahandyapp.airnavx.model.CraftSpec
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class InspectFragment : Fragment() {

    private val TAG = "InspectFragment"

    private lateinit var inspectViewModel: InspectViewModel
    private var _binding: FragmentInspectBinding? = null

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var captureViewModel: CaptureViewModel

    private lateinit var guideTextView: TextView
    private lateinit var inspectImageView: ImageView

    private lateinit var dimensionButton: Button
    private lateinit var orientButton: Button
    private lateinit var craftTypeButton: Button
    private lateinit var craftTagButton: Button
    private lateinit var measureButton: Button

    private lateinit var dimensionTextView: TextView
    private lateinit var craftTypeTextView: TextView
    private lateinit var measureTextView: TextView

    private lateinit var airCapture: AirCapture
    private var craftSpec: CraftSpec = CraftSpec()

    private lateinit var captureBitmap: Bitmap
    private lateinit var referenceBitmap: Bitmap
    private lateinit var inspectBitmap: Bitmap

    private var dimRatioX: Double = 0.0
    private var dimRatioY: Double = 0.0

    private var zoomStepX = mutableListOf<Int>()
    private var zoomStepY = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inspectViewModel = ViewModelProvider(this).get(InspectViewModel::class.java)

        _binding = FragmentInspectBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val activityViewModels: CaptureViewModel by activityViewModels()
        captureViewModel = activityViewModels

        guideTextView = root.findViewById(R.id.text_inspect)
        inspectImageView = root.findViewById(R.id.imageview_inspect) as ImageView

        dimensionTextView = root.findViewById(R.id.text_dimension)
        craftTypeTextView = root.findViewById(R.id.text_crafttype)
        measureTextView = root.findViewById(R.id.text_measure)

        // set listeners
        setButtonListeners(root)

        // retrieve AirCapture under inspection
        airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]

        // show original air capture image
        captureBitmap = captureViewModel.origBitmapArray[captureViewModel.gridPosition]
        referenceBitmap = captureBitmap
        inspectBitmap = captureBitmap
        inspectImageView.setImageBitmap(inspectBitmap)
        Log.d(TAG, "onCreateView captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${inspectImageView.width}/${inspectImageView.height}")

        // establish display dimension ratio: for landscape 4/3 -> 1.33 | for portrait 3/4 -> 0.75
        if (captureBitmap.width > captureBitmap.height) {
            dimRatioX = (captureBitmap.width.toFloat() / captureBitmap.height.toFloat()).toDouble()
            dimRatioY = (captureBitmap.height.toFloat() / captureBitmap.width.toFloat()).toDouble()
        }
        else {
            dimRatioX = (captureBitmap.width.toFloat() / captureBitmap.height.toFloat()).toDouble()
            dimRatioY = (captureBitmap.height.toFloat() / captureBitmap.width.toFloat()).toDouble()
        }
        Log.d(TAG, "onCreateView ratio X/Y $dimRatioX/$dimRatioY")

        // establish inspect image gesture detector
        establishGestureDetector(inspectImageView)

        return root
    }
    // TODO: refactor button handlers to methods
    // establish button listeners
    private fun setButtonListeners(root: View) {
        // BUTTON: measure image dimension by HORIZONTAL | VERTICAL
        dimensionButton = root.findViewById(R.id.button_dimension) as Button
        dimensionButton.text = inspectViewModel.measureDimension.toString()
        dimensionTextView.text = "H: ${inspectViewModel.zoomWidth} x V: ${inspectViewModel.zoomHeight}"
        dimensionButton.setOnClickListener {
            //Toast.makeText(this.context, "Set dimension to measure - horizontal or vertical...", Toast.LENGTH_SHORT).show()
            if (inspectViewModel.measureDimension == AirConstant.MeasureDimension.HORIZONTAL) {
                inspectViewModel.measureDimension = AirConstant.MeasureDimension.VERTICAL
            }
            else {
                inspectViewModel.measureDimension = AirConstant.MeasureDimension.HORIZONTAL
            }
            //updateMeasureDimensionText()
            dimensionButton.text = inspectViewModel.measureDimension.toString()
            dimensionTextView.text = "H: ${inspectViewModel.zoomWidth} x V: ${inspectViewModel.zoomHeight}"
            Log.d(TAG, "buttonDimension.setOnClickListener->Set dimension ${inspectViewModel.measureDimension}")
        }

        // BUTTON: orient craft to WINGSPAN | LENGTH
        orientButton = root.findViewById(R.id.button_orient) as Button
        orientButton.text = inspectViewModel.craftOrientation.toString()
        orientButton.setOnClickListener {
            //Toast.makeText(this.context, "Set orientation of measure - wingspan or length-to-tail...", Toast.LENGTH_SHORT).show()
            if (inspectViewModel.craftOrientation == AirConstant.CraftOrientation.WINGSPAN) {
                inspectViewModel.craftOrientation = AirConstant.CraftOrientation.LENGTH            }
            else {
                inspectViewModel.craftOrientation = AirConstant.CraftOrientation.WINGSPAN
            }
            //updateCraftOrientationText()
            orientButton.text = inspectViewModel.craftOrientation.toString()
            Log.d(TAG, "buttonOrient.setOnClickListener->Set orientation ${inspectViewModel.craftOrientation}...")
        }

        // TODO: enable craft type entry & dimensions
        // BUTTON: select craft type: C172, PA28, P34 plus associated craft dimension text
        // sync CraftSpec to AirCapture
        craftSpec.syncTypeTag(airCapture.craftType, airCapture.craftTag)
        airCapture.craftType = craftSpec.typeList[craftSpec.typeInx]
        airCapture.craftWingspan = craftSpec.dimsList[craftSpec.typeInx].wingspan
        airCapture.craftLength = craftSpec.dimsList[craftSpec.typeInx].length

        craftTypeButton = root.findViewById(R.id.button_crafttype) as Button
        craftTypeButton.text = airCapture.craftType
        craftTypeTextView.text = "wing x length->${airCapture.craftWingspan} x " +
                "${airCapture.craftLength}"

        craftTypeButton.setOnClickListener {
            //Toast.makeText(this.context, "Select aircraft type...", Toast.LENGTH_SHORT).show()
            ++craftSpec.typeInx
            if (craftSpec.typeInx > craftSpec.dimsList.size-1) {
                craftSpec.typeInx = 0
            }
            // clear tag list index when new type is selected
            craftSpec.tagInx = 0
            craftTagButton.text = craftSpec.tagList[craftSpec.typeInx][craftSpec.tagInx]

            // update button text & textview
            craftTypeButton.text = craftSpec.dimsList[craftSpec.typeInx].craftType
            craftTypeTextView.text = "wingspan x length->${craftSpec.dimsList[craftSpec.typeInx].wingspan}x" +
                    "${craftSpec.dimsList[craftSpec.typeInx].length}"

            Log.d(TAG, "buttonCraftType.setOnClickListener->Select aircraft type ${craftSpec.typeInx}, " +
                    "${craftSpec.dimsList[craftSpec.typeInx].craftType}...")
        }

        // BUTTON: identify aircraft tag
        craftTagButton = root.findViewById(R.id.button_identity) as Button
//        craftTagButton.text = airCapture.craftTag
        craftTagButton.text = craftSpec.tagList[craftSpec.typeInx][craftSpec.tagInx]

        craftTagButton.setOnClickListener {
            // present aircraft identification tag list
            showIdentifyAlertDialog()
            Log.d(TAG, "buttonIdentify.setOnClickListener->Identify aircraft list size " +
                    "${craftSpec.tagList[craftSpec.typeInx].size-1}->${craftSpec.tagList[craftSpec.typeInx]}")

            craftTagButton.text = craftSpec.tagList[craftSpec.typeInx][craftSpec.tagInx]
            Log.d(TAG, "buttonIdentify.setOnClickListener->Identify aircraft ${craftSpec.typeInx}->" +
                    "${craftSpec.tagList[craftSpec.typeInx]}")
        }

        // BUTTON: measure
        measureButton = root.findViewById(R.id.button_measure) as Button
        measureTextView.text = "Altitude ${airCapture.airObjectAltitude.toInt()}, distance ${airCapture.airObjectDistance.toInt()}"
        measureButton.setOnClickListener {
            Toast.makeText(this.context, "Measuring Object Under Inspection...", Toast.LENGTH_SHORT).show()
            measure()
            measureTextView.text = "Altitude ${airCapture.airObjectAltitude.toInt()}, distance ${airCapture.airObjectDistance.toInt()}"
            Log.d(TAG, "buttonMeasure.setOnClickListener->Measuring Object Under Inspection...")
        }
    }

    private fun establishGestureDetector(imageView: ImageView) {
        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean {
                Log.i("TAG", "establishGestureDetector onDown: ")
                // don't return false here or else none of the other gestures will work
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                Log.i("TAG", "establishGestureDetector onSingleTapConfirmed ZOOM IN...")
                inspectZoomOnTap(InspectViewModel.ZoomDirection.IN)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val x = e.x.roundToInt()
                val y = e.y.roundToInt()
                Log.i("TAG", "establishGestureDetector onLongPress: x $x, y $y")
                centerBitmap(x, y)
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (zoomStepX.isNotEmpty()) {
                    Log.i("TAG", "establishGestureDetector onDoubleTap ZOOM OUT...")
                    inspectZoomOnTap(InspectViewModel.ZoomDirection.OUT)
                }
                else {
                    Log.i("TAG", "establishGestureDetector onDoubleTap NO ZOOM OUT at full size...")
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ): Boolean {
                Log.i("TAG", "establishGestureDetector nScroll: distanceX $distanceX distanceY $distanceY")
                return true
            }

            override fun onFling(
                event1: MotionEvent?, event2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                Log.d("TAG", "establishGestureDetector onFling: velocityX $velocityX velocityY $velocityY")

                if (event1 != null)
                    if (((event1.x - event2.x) > SWIPE_MIN_DISTANCE) && (abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)) {
                        Log.d("TAG", "establishGestureDetector onFling: LEFT SWIPE")
                    }  else if (event2.x - event1.x > SWIPE_MIN_DISTANCE && abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        Log.d("TAG", "establishGestureDetector onFling: RIGHT SWIPE")
                    }
                    else if (event1.y - event2.y > SWIPE_MIN_DISTANCE && abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        // UP SWIPE
                        Log.d("TAG", "establishGestureDetector onFling: UP SWIPE")
                    }
                    else if (event2.y - event1.y > SWIPE_MIN_DISTANCE && abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        // DOWN SWIPE - refresh overlay
                        // show original air capture image
                        captureBitmap = captureViewModel.origBitmapArray[captureViewModel.gridPosition]
                        referenceBitmap = captureBitmap
                        inspectBitmap = captureBitmap
                        inspectImageView.setImageBitmap(inspectBitmap)
                        Log.d(TAG, "onFling captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}")
                        Log.d(TAG, "onFling imageViewInspect w/h ${inspectImageView.width}/${inspectImageView.height}")
                    }

                return true
            }

            override fun onShowPress(e: MotionEvent) {
                Log.i("TAG", "establishGestureDetector onShowPress: ")
                return
            }
        })
        imageView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    private fun measure() {
        val actualSize: Double = if (inspectViewModel.craftOrientation == AirConstant.CraftOrientation.WINGSPAN) {
            craftSpec.dimsList[craftSpec.typeInx].wingspan
        } else {  // LENGTH
            craftSpec.dimsList[craftSpec.typeInx].length
        }
        Log.d(TAG, "measure-> actualSize $actualSize ")

        // assign ratio for FOV at 1 foot from device/camera characteristics for portrait | landscape
        var ratioFor100PerCentFOVat1FootPixel6 = ratioFor100PerCentFOVat1FootPixel6Portrait // portrait
        if (captureBitmap.width > captureBitmap.height) {
            ratioFor100PerCentFOVat1FootPixel6 = ratioFor100PerCentFOVat1FootPixel6Landscape // landscape 0.71
        }
        Log.d(TAG, "buttonMeasure.setOnClickListener->Measuring Pixel6 $ratioFor100PerCentFOVat1FootPixel6")

        val distObjectAt100PerCentFOV = actualSize * ratioFor100PerCentFOVat1FootPixel6
        Log.d(TAG, "measure-> distObjectAt100PerCentFOV $distObjectAt100PerCentFOV ")

        val imageSize: Double
        val apparentSize: Double
        if (inspectViewModel.measureDimension == AirConstant.MeasureDimension.HORIZONTAL) {
            imageSize = captureBitmap.width.toDouble()
            apparentSize = inspectBitmap.width.toDouble()
        }
        else {
            imageSize = captureBitmap.height.toDouble()
            apparentSize = inspectBitmap.height.toDouble()
        }
        Log.d(TAG, "measure-> imageSize $imageSize, apparentSize $apparentSize")
        val sizeRatio = apparentSize/imageSize
        Log.d(TAG, "measure-> sizeRatio $sizeRatio")

        //val distFor100PerCentFOVat1FootPixel6 = 0.96875
        val dist: Double = distObjectAt100PerCentFOV / sizeRatio
        Log.d(TAG, "measure-> dist $dist")
        val cameraAngle: Double = airCapture.cameraAngle.toDouble()
        Log.d(TAG, "measure-> cameraAngle $cameraAngle")
        val angleRadians = cameraAngle * (Math.PI / 180.0)
        Log.d(TAG, "measure-> sin(angleRadians) ${sin(angleRadians)}")
        val altitude: Double = sin(angleRadians) * dist
        Log.d(TAG, "measure-> altitude $altitude !!!!")

        // update & write AirCapture measures
        airCapture.airObjectAltitude = altitude
        airCapture.airObjectDistance = dist
    }

    private fun centerBitmap(x: Int, y: Int) {
        // center bitmap at x, y
        Log.d(TAG, "centerBitmap-> x $x, y $y")
        // TODO: implement centerBitmap
    }

    private fun inspectZoomOnTap(direction: InspectViewModel.ZoomDirection) {
        // zoom in or out at center of image
        Log.d(TAG, "inspectZoomOnTap-> direction $direction")
        // TODO: implement inspectZoomOnTap
    }

    private fun showIdentifyAlertDialog() {
        val builder = AlertDialog.Builder(this.context)
        builder.setTitle("Identify Aircraft")

        val input = EditText(this.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(craftSpec.tagList[craftSpec.typeInx][craftSpec.tagInx])
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val tag = input.text.toString()
            if (tag.isNotEmpty()) {
                craftSpec.tagList[craftSpec.typeInx][craftSpec.tagInx] = tag
                craftTagButton.text = tag
                airCapture.craftTag = tag
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
