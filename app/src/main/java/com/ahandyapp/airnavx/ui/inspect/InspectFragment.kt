package com.ahandyapp.airnavx.ui.inspect

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.databinding.FragmentInspectBinding
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.model.AirCapture
import java.util.ArrayList
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
    private lateinit var identifyButton: Button
    private lateinit var measureButton: Button

    private lateinit var dimensionTextView: TextView
    private lateinit var orientTextView: TextView
    private lateinit var craftTypeTextView: TextView
    private lateinit var identifyTextView: TextView
    private lateinit var measureTextView: TextView

    private lateinit var craftIdentList: ArrayList<String>

    private lateinit var airCapture: AirCapture

    private lateinit var captureBitmap: Bitmap      // original capture bitmap
    private lateinit var referenceBitmap: Bitmap    // current reference bitmap
    private lateinit var inspectBitmap: Bitmap      // inspect view image bitmap

    private var referenceCenterX = 0
    private var referenceCenterY = 0
    private var referenceUpperLeftX = 0
    private var referenceUpperLeftY = 0

    private var zoomBase = 0
    private var zoomStepX = ArrayList<Int>()
    private var zoomStepY = ArrayList<Int>()

    private var dimRatioX = 0.0
    private var dimRatioY = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        inspectViewModel = ViewModelProvider(this).get(InspectViewModel::class.java)

        _binding = FragmentInspectBinding.inflate(inflater, container, false)
        val root: View = binding.root

        guideTextView = binding.textInspect
        inspectViewModel.guideText.observe(viewLifecycleOwner) {
            guideTextView.text = it
        }

        dimensionTextView = binding.textDimension
        //orientTextView = binding.textDimension
        craftTypeTextView = binding.textCrafttype
        //identifyTextView = binding.textIdentify
        measureTextView = binding.textMeasure

        // establish inspect imageview
        inspectImageView = root.findViewById(R.id.imageview_inspect) as ImageView

        // reference capture viewmodel
        Log.d(TAG, "onCreateView captureViewModel access...")

        //var captureViewModel = ViewModelProvider(this).get(CaptureViewModel::class.java)
        val viewModel: CaptureViewModel by activityViewModels()
        captureViewModel = viewModel

        airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
        Log.d(TAG, "onCreateView captureViewModel airCapture $airCapture")

        // set inspect image to selected capture thumb
        Log.d(TAG, "onCreateView captureViewModel grid position ${captureViewModel.gridPosition}")
        captureBitmap = captureViewModel.fullBitmapArray[captureViewModel.gridPosition]
        referenceBitmap = captureBitmap
        inspectBitmap = captureBitmap
        inspectImageView.setImageBitmap(captureBitmap)
        Log.d(TAG, "onCreateView captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${inspectImageView.width}/${inspectImageView.height}")

        if (captureBitmap.width < captureBitmap.height) {
            inspectViewModel.imageOrientation = InspectViewModel.ImageOrientation.PORTRAIT
        }
        else {
            inspectViewModel.imageOrientation = InspectViewModel.ImageOrientation.LANDSCAPE
        }
        Log.d(TAG, "onCreateView imageOrientation ${inspectViewModel.imageOrientation}")

        // establish measure button listeners
        this.context?.let { setButtonListeners(root, it) }

        // TODO: image attri data object
        // set image attributes
        referenceUpperLeftX = 0
        referenceUpperLeftY = 0
        referenceCenterX = captureBitmap.width / 2
        referenceCenterY = captureBitmap.height / 2
        Log.d(TAG, "onCreateView center X/Y $referenceCenterX/$referenceCenterY")

        if (captureBitmap.width < captureBitmap.height) {
            dimRatioX = (captureBitmap.width.toFloat() / captureBitmap.height.toFloat()).toDouble()
            dimRatioY = (captureBitmap.height.toFloat() / captureBitmap.width.toFloat()).toDouble()
        }
        else if (captureBitmap.width > captureBitmap.height) {
            dimRatioX = (captureBitmap.width.toFloat() / captureBitmap.height.toFloat()).toDouble()
            dimRatioY = (captureBitmap.height.toFloat() / captureBitmap.width.toFloat()).toDouble()
        }
        Log.d(TAG, "onCreateView ratio X/Y $dimRatioX/$dimRatioY")

        // establish inspect image gesture detector
        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent?): Boolean {
                Log.i("TAG", "onCreateView onDown: ")
                // don't return false here or else none of the other gestures will work
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                Log.i("TAG", "onCreateView onSingleTapConfirmed ZOOM IN...")
                inspectZoomOnTap(InspectViewModel.ZoomDirection.IN)
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                val x = e?.x?.roundToInt()
                val y = e?.y?.roundToInt()
                Log.i("TAG", "onCreateView onLongPress: x $x, y $y")
                if (x != null && y != null) {
                    centerBitmap(x, y)
                }
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (zoomStepX.size > 0) {
                    Log.i("TAG", "onCreateView onDoubleTap ZOOM OUT...")
                    inspectZoomOnTap(InspectViewModel.ZoomDirection.OUT)
                }
                else {
                    Log.i("TAG", "onCreateView onDoubleTap NO ZOOM OUT at full size...")
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent?,
                distanceX: Float, distanceY: Float
            ): Boolean {
                Log.i("TAG", "onCreateView nScroll: distanceX $distanceX distanceY $distanceY")
                return true
            }

            override fun onFling(
                event1: MotionEvent?, event2: MotionEvent?,
                velocityX: Float, velocityY: Float
            ): Boolean {
                Log.d("TAG", "onCreateView onFling: velocityX $velocityX velocityY $velocityY")
                return true
            }

            override fun onShowPress(e: MotionEvent?) {
                Log.i("TAG", "onCreateView onShowPress: ")
                return
            }

        })
        inspectImageView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
          // TODO: pinch/zoom?
//        imageViewFull.setOnTouchListener { v, event ->
//            decodeTouchAction(event)
//            true
//        }
//
        return root
    }
    // establish button listeners
    private fun setButtonListeners(root: View, context: Context) {
        // measure image dimension by HORIZONTAL | VERTICAL
        dimensionButton = root.findViewById(R.id.button_dimension) as Button
        dimensionButton.text = inspectViewModel.measureDimension.toString()
        dimensionTextView.text = "H: ${captureBitmap.width} x V: ${captureBitmap.height}"
        dimensionButton.setOnClickListener {
            //Toast.makeText(this.context, "Set dimension to measure - horizontal or vertical...", Toast.LENGTH_SHORT).show()
            if (inspectViewModel.measureDimension == InspectViewModel.MeasureDimension.HORIZONTAL) {
                inspectViewModel.measureDimension = InspectViewModel.MeasureDimension.VERTICAL
            }
            else {
                inspectViewModel.measureDimension = InspectViewModel.MeasureDimension.HORIZONTAL
            }
            //updateMeasureDimensionText()
            dimensionButton.text = inspectViewModel.measureDimension.toString()
            dimensionTextView.text = "H: ${inspectViewModel.zoomWidth} x V: ${inspectViewModel.zoomHeight}"
            Log.d(TAG, "buttonDimension.setOnClickListener->Set dimension ${inspectViewModel.measureDimension}")
        }

        // orient craft to WINGSPAN | LENGTH
        orientButton = root.findViewById(R.id.button_orient) as Button
        orientButton.text = inspectViewModel.craftOrientation.toString()
        orientButton.setOnClickListener {
            //Toast.makeText(this.context, "Set orientation of measure - wingspan or length-to-tail...", Toast.LENGTH_SHORT).show()
            if (inspectViewModel.craftOrientation == InspectViewModel.CraftOrientation.WINGSPAN) {
                inspectViewModel.craftOrientation = InspectViewModel.CraftOrientation.LENGTH            }
            else {
                inspectViewModel.craftOrientation = InspectViewModel.CraftOrientation.WINGSPAN
            }
            //updateCraftOrientationText()
            orientButton.text = inspectViewModel.craftOrientation.toString()
            Log.d(TAG, "buttonOrient.setOnClickListener->Set orientation ${inspectViewModel.craftOrientation.toString()}...")
        }

        // select craft type TODO: enable craft type entry & dimensions
        craftTypeButton = root.findViewById(R.id.button_crafttype) as Button
        craftTypeButton.text = inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].craftType
        craftTypeTextView.text = "wing x length->${inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].wingspan} x " +
                "${inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].length}"
        // clear ident list index
        inspectViewModel.craftIdentListInx = 0
        setCraftIdentList()
        craftTypeButton.setOnClickListener {
            //Toast.makeText(this.context, "Select aircraft type...", Toast.LENGTH_SHORT).show()
            ++inspectViewModel.craftDimListInx
            if (inspectViewModel.craftDimListInx > inspectViewModel.craftDimsList.size-1) {
                inspectViewModel.craftDimListInx = 0
            }
            // clear ident list index
            inspectViewModel.craftIdentListInx = 0
            setCraftIdentList()
            identifyButton.text = craftIdentList[inspectViewModel.craftIdentListInx]

            // update button text & textview
            craftTypeButton.text = inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].craftType
            craftTypeTextView.text = "wingspan x length->${inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].wingspan}x" +
                    "${inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].length}"

            Log.d(TAG, "buttonCraftType.setOnClickListener->Select aircraft type ${inspectViewModel.craftDimListInx}, " +
                    "${inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].craftType}...")
        }

        identifyButton = root.findViewById(R.id.button_identity) as Button
        identifyButton.text = craftIdentList[inspectViewModel.craftIdentListInx]
        identifyButton.setOnClickListener {
            ++inspectViewModel.craftIdentListInx
            if (inspectViewModel.craftIdentListInx > craftIdentList.size-1) {
                inspectViewModel.craftIdentListInx = 0
            }
            Log.d(TAG, "buttonIdentify.setOnClickListener->Identify aircraft list size ${craftIdentList.size-1}->${craftIdentList}")

            identifyButton.text = craftIdentList[inspectViewModel.craftIdentListInx]
            Log.d(TAG, "buttonIdentify.setOnClickListener->Identify aircraft ${inspectViewModel.craftIdentListInx}->" +
                    "${craftIdentList[inspectViewModel.craftIdentListInx]}")
        }

        measureButton = root.findViewById(R.id.button_measure) as Button
        measureTextView.text = "Altitude ${airCapture.airObjectAltitude}, distance ${airCapture.airObjectDistance}"
        measureButton.setOnClickListener {
            //Toast.makeText(this.context, "Measuring Object Under Inspection...", Toast.LENGTH_SHORT).show()
//            val distFor100PerCentFOVat1FootPixel4 = 1.065
//            Log.d(TAG, "buttonMeasure.setOnClickListener->Measuring Pixel4 $distFor100PerCentFOVat1FootPixel4")
//            measure(distFor100PerCentFOVat1FootPixel4)
            val distFor100PerCentFOVat1FootPixel6 = 0.96875
            Log.d(TAG, "buttonMeasure.setOnClickListener->Measuring Pixel6 $distFor100PerCentFOVat1FootPixel6")
            measure(distFor100PerCentFOVat1FootPixel6)
            measureTextView.text = "Altitude ${airCapture.airObjectAltitude}, distance ${airCapture.airObjectDistance}"
            Log.d(TAG, "buttonMeasure.setOnClickListener->Measuring Object Under Inspection...")
        }
    }

    private fun setCraftIdentList(): ArrayList<String> {
//        // clear ident list index
//        inspectViewModel.craftIdentListInx = 0
        // set ident list based on selected type
//            var craftIdentList: ArrayList<String>
        when (inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].craftType) {
            "PA28" -> craftIdentList = inspectViewModel.craftIdentPA28List
            "PA34" -> craftIdentList = inspectViewModel.craftIdentPA34List
            else -> craftIdentList = inspectViewModel.craftIdentC172List
        }
        Log.d(TAG, "setCraftIdentList $craftIdentList")
        return craftIdentList
    }

    private fun measure(distFor100PerCentFOVat1Foot:Double) {
        var actualSize: Double = 0.0
        if (inspectViewModel.craftOrientation == InspectViewModel.CraftOrientation.WINGSPAN) {
            actualSize = inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].wingspan
        }
        else {  // LENGTH
            actualSize = inspectViewModel.craftDimsList[inspectViewModel.craftDimListInx].length
        }
        Log.d(TAG, "measure-> actualSize $actualSize ")

        //val distFor100PerCentFOVat1Foot = 1.065

        val distObjectAt100PerCentFOV = actualSize * distFor100PerCentFOVat1Foot
        Log.d(TAG, "measure-> distObjectAt100PerCentFOV $distObjectAt100PerCentFOV ")

        var imageSize: Double = 0.0
        var apparentSize: Double = 0.0
        if (inspectViewModel.measureDimension == InspectViewModel.MeasureDimension.HORIZONTAL) {
            imageSize = captureBitmap.width.toDouble()
            apparentSize = inspectViewModel.zoomWidth.toDouble()
        }
        else {
            imageSize = captureBitmap.height.toDouble()
            apparentSize = inspectViewModel.zoomHeight.toDouble()
        }
        Log.d(TAG, "measure-> imageSize $imageSize, apparentSize $apparentSize")
        var sizeRatio = apparentSize/imageSize
        Log.d(TAG, "measure-> sizeRatio $sizeRatio")

        //val distFor100PerCentFOVat1FootPixel6 = 0.96875
        var dist: Double = distObjectAt100PerCentFOV / sizeRatio
        Log.d(TAG, "measure-> dist $dist")
        var cameraAngle: Double = airCapture.cameraAngle.toDouble()
        Log.d(TAG, "measure-> cameraAngle $cameraAngle")
        var angleRadians = cameraAngle * 0.01745329251994329576923690768489
        Log.d(TAG, "measure-> sin(angleRadians) ${sin(angleRadians)}")
        var altitude: Double = sin(angleRadians) * dist
        Log.d(TAG, "measure-> altitude $altitude !!!!")
        // TODO: float->double
        airCapture.airObjectAltitude = altitude.toFloat()
        airCapture.airObjectDistance = dist.toFloat()
    }

    private fun inspectZoomOnTap(zoomDirection: InspectViewModel.ZoomDirection) {
        Log.d(TAG, "inspectZoomOnTap imageViewInspect w/h ${inspectImageView.width}/${inspectImageView.height}")
        Log.d(TAG, "inspectZoomOnTap referenceBitmap w/h ${referenceBitmap.width}/${referenceBitmap.height}")
        inspectBitmap = zoomOnBitmap(referenceBitmap, zoomDirection)
        Log.d(TAG, "inspectZoomOnTap inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")

        inspectImageView.setImageBitmap(inspectBitmap)
    }

    private fun zoomOnBitmap(imageBitmap: Bitmap, zoomDirection: InspectViewModel.ZoomDirection) : Bitmap {
        // determine width/height, upper left & create zoom bitmap
        Log.d(TAG, "zoomOnBitmap imageBitmap w/h ${imageBitmap.width}/${imageBitmap.height}")
        Log.d(TAG, "zoomOnBitmap imageBitmap zoom direction $zoomDirection")
        Log.d(TAG, "zoomOnBitmap imageBitmap imageOrientation ${inspectViewModel.imageOrientation}")

        Log.d(TAG, "zoomOnBitmap inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")
        if (zoomDirection == InspectViewModel.ZoomDirection.IN) {
            // set zoomBase & zoomFactor for PORTRAIT
            zoomBase = inspectBitmap.width
            if (inspectViewModel.imageOrientation == InspectViewModel.ImageOrientation.LANDSCAPE) {
                // set zoomBase & zoomFactor for LANDSCAPE
                zoomBase = inspectBitmap.height
            }
            if (zoomBase > 2048) {
                zoomBase = zoomBase / 2
            }
            else if (zoomBase > 1024) {
                zoomBase = zoomBase / 3
            }
            else if (zoomBase > 512) {
                zoomBase = zoomBase / 4
            }
            else if (zoomBase > 256) {
                zoomBase = zoomBase / 5
            }
            else if (zoomBase > 128) {
                zoomBase = zoomBase / 6
            }
            else {
                zoomBase = 8
            }
            Log.d(TAG, "zoomOnBitmap zoomBase $zoomBase")
            if (inspectViewModel.imageOrientation == InspectViewModel.ImageOrientation.PORTRAIT) {
                // adjust stepX leaving stepY unchanged
                zoomStepX.add((dimRatioX * zoomBase).toInt())
                zoomStepY.add(zoomBase)
            }
            else {
                // adjust stepY leaving stepX unchanged
                zoomStepX.add(zoomBase)
                zoomStepY.add((dimRatioY * zoomBase).toInt())
            }
        }
        else { // OUT
            zoomStepX.removeAt(zoomStepX.size-1)
            zoomStepY.removeAt(zoomStepY.size-1)
        }
        Log.d(TAG, "zoomOnBitmap zoomBase $zoomBase, zoom step X/Y $zoomStepX/$zoomStepY")

        var zoomUpperLeftX = referenceUpperLeftX
        var width = imageBitmap.width
        var totalStepX = 0
        for (stepX in zoomStepX) {
            totalStepX += stepX
        }
        var zoomUpperLeftY = referenceUpperLeftY
        var height = imageBitmap.height
        var totalStepY = 0
        for (stepY in zoomStepY) {
            totalStepY += stepY
        }
        zoomUpperLeftX += (totalStepX / 2)
        width -= totalStepX
        zoomUpperLeftY += (totalStepY / 2)
        height -= totalStepY
        Log.d(TAG, "zoomOnBitmap zoomed upper left X/Y $zoomUpperLeftX/$zoomUpperLeftY")
        Log.d(TAG, "zoomOnBitmap zoomed w/h $width/$height")
        // TODO: prevent off-edge overflow!  fine zoomBase
        if (zoomUpperLeftX < 0 || zoomUpperLeftY < 0 ||
            width > imageBitmap.width || height > imageBitmap.height) {
            Log.e(TAG, "zoomOnBitmap OFF-EDGE upper left X/Y $zoomUpperLeftX/$zoomUpperLeftY, w/h $width/$height")
            return imageBitmap
        }
        // create zoom bitmap
        var zoomBitmap = imageBitmap
        try {
            zoomBitmap = Bitmap.createBitmap(imageBitmap, zoomUpperLeftX, zoomUpperLeftY, width, height)
        } catch (ex: Exception) {
            Log.e(TAG, "zoomOnBitmap Exception ${ex.message}")
        }
        // update viewmodel
        inspectViewModel.zoomWidth = width
        inspectViewModel.zoomHeight = height
        dimensionTextView.text = "H: ${inspectViewModel.zoomWidth} x V: ${inspectViewModel.zoomHeight}"
        //updateMeasureDimensionText()
        return zoomBitmap
    }

    // find width, height, upper left, zoomCount based on new center
    private fun centerBitmap(centerX: Int, centerY: Int) {
        if (zoomStepX.size > 0 ) {
            referenceBitmap = inspectBitmap
        }
        val imageBitmap = referenceBitmap
        Log.d(TAG, "centerZoomBitmap captureBitmap w/h ${imageBitmap.width}/${imageBitmap.height}")
        Log.d(TAG, "centerZoomBitmap imageViewInspect w/h ${inspectImageView.width}/${inspectImageView.height}")
        // translate imageView coords to image bitmap coords
        val viewBitmapRatioX = (imageBitmap.width.toFloat() / inspectImageView.width.toFloat()).toDouble()
        val viewBitmapRatioY = (imageBitmap.height.toFloat() / inspectImageView.height.toFloat()).toDouble()
        Log.d(TAG, "centerZoomBitmap ratio x/y $viewBitmapRatioX/$viewBitmapRatioY")
        // assign reference bitmap center
        referenceCenterX = (centerX * viewBitmapRatioX).toInt()
        referenceCenterY = (centerY * viewBitmapRatioY).toInt()
        Log.d(TAG, "centerZoomBitmap capture center x/y $referenceCenterX/$referenceCenterY")
        // disallow low-probability touch exactly on meridian
        if (referenceCenterX == (imageBitmap.width/2)) {
            ++referenceCenterX
            Log.d(TAG, "centerZoomBitmap capture adjusted center X $referenceCenterX")
        }
        if (referenceCenterY == (imageBitmap.height/2)) {
            ++referenceCenterY
            Log.d(TAG, "centerZoomBitmap capture adjusted center Y $referenceCenterY")
        }
        val centerX = referenceCenterX
        val centerY = referenceCenterY
        // find width height maintaining ratio
        var width = imageBitmap.width
        var height = imageBitmap.height
        Log.d(TAG, "centerZoomBitmap pre-bounds check width/height $width/$height")

        // set pre-bounds check upper left, lower right
        var upperLeftX = centerX - (width / 2)
        var upperLeftY = centerY - (height / 2)
        var lowerRightX = centerX + (width / 2)
        var lowerRightY = centerY + (height / 2)
        Log.d(TAG, "centerZoomBitmap pre-bounds check upper left X/Y $upperLeftX/$upperLeftY")
        Log.d(TAG, "centerZoomBitmap pre-bounds check lowerRight X/Y $lowerRightX/$lowerRightY")

        if (upperLeftX < 0 && upperLeftY < 0 &&
            lowerRightX < imageBitmap.width && lowerRightY < imageBitmap.height) {
            Log.d(TAG, "centerZoomBitmap off-edge upper left...")
            upperLeftX = 0
            width = centerX * 2
            // recalculate width/height maintaining aspect ratio
            height = (width.toDouble() * dimRatioY).toInt()
            upperLeftY = centerY - (height / 2)
            // if upper left Y off-edge
            if (upperLeftY < 0) {
                upperLeftY = 0
                height = centerY * 2
                width = (height.toDouble() * dimRatioX).toInt()
                upperLeftX = centerX - (width/2)
            }
            Log.d(TAG, "centerZoomBitmap off-edge upper left UL X/Y $upperLeftX/$upperLeftY")
            Log.d(TAG, "centerZoomBitmap off-edge upper left width/height $width/$height")
        }
        else if (upperLeftX < 0 && upperLeftY < imageBitmap.height &&
            lowerRightY > imageBitmap.height) {
            Log.d(TAG, "centerZoomBitmap off-edge lower left...")
            upperLeftX = 0
            width = centerX * 2
            // recalculate width/height maintaining aspect ratio
            height = (width.toDouble() * dimRatioY).toInt()
            lowerRightY = centerY + (height / 2)
            // if lowerRightY off-edge
            if (lowerRightY > imageBitmap.height) {
                height = (imageBitmap.height - centerY) * 2
                upperLeftY = centerY - (height/2)
                width = (height.toDouble() * dimRatioX).toInt()
                upperLeftX = centerX - (width/2)
            }
            Log.d(TAG, "centerZoomBitmap off-edge lower left UL X/Y $upperLeftX/$upperLeftY")
            Log.d(TAG, "centerZoomBitmap off-edge lower left width/height $width/$height")
        }
        else if (upperLeftX < imageBitmap.width && upperLeftY < 0 &&
            lowerRightX > imageBitmap.width && lowerRightY < imageBitmap.height) {
            Log.d(TAG, "centerZoomBitmap off-edge upper right...")
            width = (imageBitmap.width - centerX) * 2
            height = (width.toDouble() * dimRatioY).toInt()
            upperLeftY = centerY - (height / 2)
            // if upper left Y off-edge
            if (upperLeftY < 0) {
                height = centerY * 2
                width = (height.toDouble() * dimRatioX).toInt()
            }
            Log.d(TAG, "centerZoomBitmap off-edge upper right width/height $width/$height")
        }
        else if (upperLeftX < imageBitmap.width && upperLeftY < imageBitmap.height &&
            lowerRightX > imageBitmap.width && lowerRightY > imageBitmap.height) {
            Log.d(TAG, "centerZoomBitmap off-edge lower right...")
            width = (imageBitmap.width - centerX) * 2
            height = (width.toDouble() * dimRatioY).toInt()
            lowerRightY = centerY + (height / 2)
            // if upper left Y off-edge
            if (lowerRightY > imageBitmap.height) {
                height = (imageBitmap.height - centerY) * 2
                width = (height.toDouble() * dimRatioX).toInt()
            }
            Log.d(TAG, "centerZoomBitmap off-edge lower right width/height $width/$height")
        }
        // set post-bounds check upper left, lower right
        upperLeftX = centerX - (width / 2)
        upperLeftY = centerY - (height / 2)
        lowerRightX = centerX + (width / 2)
        lowerRightY = centerY + (height / 2)
        Log.d(TAG, "centerZoomBitmap pre-bounds check upper left X/Y $upperLeftX/$upperLeftY")
        Log.d(TAG, "centerZoomBitmap pre-bounds check lowerRight X/Y $lowerRightX/$lowerRightY")

        // set upper left
        referenceCenterX = centerX
        referenceCenterY = centerY
        Log.d(TAG, "centerZoomBitmap post-bounds center x/y $referenceCenterX/$referenceCenterY")
        referenceUpperLeftX = referenceCenterX - (width / 2)
        referenceUpperLeftY = referenceCenterY - (height / 2)
        Log.d(TAG, "centerZoomBitmap post-bounds upper left X/Y $referenceUpperLeftX/$referenceUpperLeftY")
        Log.d(TAG, "centerZoomBitmap post-bounds width/height $width/$height")

        // create bitmap
        try {
            inspectBitmap = Bitmap.createBitmap(imageBitmap, referenceUpperLeftX, referenceUpperLeftY, width, height)
        } catch (ex: Exception) {
            Log.e(TAG, "inspectZoomOnTap Exception ${ex.message}")
            return
        }
        // TODO: reference data class w/ init
        // reset reference bitmap
        referenceBitmap = inspectBitmap
        referenceUpperLeftX = 0
        referenceUpperLeftY = 0
        referenceCenterX = (width / 2)
        referenceCenterY = (height / 2)

        inspectImageView.setImageBitmap(inspectBitmap)
        Log.d(TAG, "centerZoomBitmap inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${inspectImageView.width}/${inspectImageView.height}")
        // reset zoomStep
        zoomStepX = ArrayList<Int>()
        zoomStepY = ArrayList<Int>()
    }

    /////////////////////////lifecycle///////////////////////////
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume...")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause...")
    }
    /////////////////////////unused///////////////////////////
    private fun scaleImage(imageBitmap: Bitmap, scaleFactor: Int): Bitmap {
//        val width = (imageBitmap.width)?.div(scaleFactor)
//        val height = (imageBitmap.height)?.div(scaleFactor)
        val width = (imageBitmap.width)?.times(scaleFactor)
        val height = (imageBitmap.height)?.times(scaleFactor)
        val thumbBitmap = ThumbnailUtils.extractThumbnail(
            imageBitmap,
            width,
            height
        )
        if (thumbBitmap != null) {
            Log.d(TAG, "extractThumbnail source image width $imageBitmap.width x height $imageBitmap.height")
            Log.d(TAG, "extractThumbnail $thumbBitmap at scale factor $scaleFactor")
        } else {
            Log.e(TAG, "extractThumbnail NULL (at scale factor $scaleFactor)")
        }
        return thumbBitmap
    }

    // TODO: pinch/zoom? onclick listener
    fun decodeTouchAction(event: MotionEvent) {
        val action = event.action
        var pDownX: Int
        var pDownY: Int
        var pUpX: Int
        var pUpY: Int
        var pMoveX: Int
        var pMoveY: Int

        when(action){

            MotionEvent.ACTION_DOWN -> {
                pDownX= event.x.toInt()
                pDownY= event.y.toInt()
                Log.d(TAG, "decodeTouchAction DOWN event at $pDownX, $pDownY...")
            }

            MotionEvent.ACTION_MOVE -> {
                pMoveX= event.x.toInt()
                pMoveY= event.y.toInt()
                Log.d(TAG, "decodeTouchAction MOVE event at $pMoveX, $pMoveY...")
            }

            MotionEvent.ACTION_UP -> {
                pUpX= event.x.toInt()
                pUpY= event.y.toInt()
                Log.d(TAG, "decodeTouchAction UP event at $pUpX, $pUpY...")
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.d(TAG, "decodeTouchAction CANCEL event...")
            }

            else ->{
                Log.d(TAG, "imageViewPreview.setOnClickListener UNKNOWN event...")
            }
        }
        true

    }

}