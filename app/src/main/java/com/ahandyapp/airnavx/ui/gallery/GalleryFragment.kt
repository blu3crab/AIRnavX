package com.ahandyapp.airnavx.ui.gallery

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.databinding.FragmentGalleryBinding
import com.ahandyapp.airnavx.model.AirCapture
import com.ahandyapp.airnavx.model.AirConstant
import com.ahandyapp.airnavx.model.AirImageUtil
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import com.ahandyapp.airnavx.ui.inspect.InspectViewModel
import kotlin.math.roundToInt

class GalleryFragment : Fragment() {

    private val TAG = "GalleryFragment"

    private lateinit var galleryViewModel: GalleryViewModel
    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var galleryImageView: ImageView

    private lateinit var captureViewModel: CaptureViewModel
    private lateinit var inspectViewModel: InspectViewModel

    private lateinit var airCapture: AirCapture

    private lateinit var captureBitmap: Bitmap  // original capture bitmap
    private lateinit var zoomBitmap: Bitmap     // paired zoom bitmap
    private lateinit var overBitmap: Bitmap     // paired zoom bitmap
    private lateinit var galleryBitmap: Bitmap  // gallery view image bitmap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // establish galleryViewModel bindings
        galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // establish gallery imageview
        galleryImageView = root.findViewById(R.id.imageview_gallery) as ImageView

        // connect to captureViewModel
        val viewModelT1: CaptureViewModel by activityViewModels()
        captureViewModel = viewModelT1

        airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
        Log.d(TAG, "onCreateView captureViewModel airCapture $airCapture")

        // set inspect image to selected capture thumb
        Log.d(TAG, "onCreateView captureViewModel grid position ${captureViewModel.gridPosition}")
        captureBitmap = captureViewModel.origBitmapArray[captureViewModel.gridPosition]
        zoomBitmap = captureViewModel.zoomBitmapArray[captureViewModel.gridPosition]
        overBitmap = captureViewModel.overBitmapArray[captureViewModel.gridPosition]
        galleryImageView.setImageBitmap(overBitmap)

        Log.d(TAG,"onCreateView captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}" )
        Log.d(TAG,"onCreateView imageViewGallery w/h ${galleryImageView.width}/${galleryImageView.height}")

        // set button on-click listener
        val buttonRefresh = root.findViewById(R.id.button_refresh) as Button
        buttonRefresh.setOnClickListener {
            Toast.makeText(this.context, "refreshing overlay...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonRefresh.setOnClickListener refresh...")
            refresh()
        }

        // connect to inspectViewModel
        val viewModelT2: InspectViewModel by activityViewModels()
        inspectViewModel = viewModelT2

        // establish gesture detector
        this.context?.let { establishGestureDetector(it, galleryImageView) }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateOverlay(toggle: Int): Boolean {
        Log.d(TAG,"navigateOverlay gridPosition ${captureViewModel.gridPosition} of ${captureViewModel.gridCount}, toggle $toggle")
        if ((captureViewModel.gridPosition + toggle) >= 0 && (captureViewModel.gridPosition + toggle) < captureViewModel.gridCount) {
            captureViewModel.gridPosition += toggle
            overBitmap = captureViewModel.overBitmapArray[captureViewModel.gridPosition]
            galleryImageView.setImageBitmap(overBitmap)
            Log.d(TAG,"navigateOverlay updated gridPosition ${captureViewModel.gridPosition}")
            return true
        }
        return false
    }
    private fun refresh() {
        overBitmap = overlay(captureBitmap, zoomBitmap)
        galleryImageView.setImageBitmap(overBitmap)
        // save overlay image
        val imageFilename = AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp + AirConstant.DEFAULT_OVER_SUFFIX
        var airImageUtil = AirImageUtil()
        val success = airImageUtil.convertBitmapToFile(context!!, overBitmap, imageFilename)
        if (success) {
            // update capture viewmodel overlay bitmap array
            captureViewModel.overBitmapArray[captureViewModel.gridPosition] = overBitmap
            Log.d(TAG,"refresh captureViewModel overBitmapArray position ${captureViewModel.gridPosition} updated with $imageFilename")
        }
    }
    private fun overlay(bmp1: Bitmap, bmp2: Bitmap): Bitmap {
        val bmOverlay = Bitmap.createBitmap(bmp1.width, bmp1.height, bmp1.config)
        val canvas = Canvas(bmOverlay)
        canvas.drawBitmap(bmp1, Matrix(), null)
        // draw zoom image in lower left corner
        canvas.drawBitmap(bmp2,
            Rect(0, 0, bmp2.width, bmp2.height),
            Rect(0, bmp1.height-1024, 1024, bmp1.height), null)
        // draw overlay & zoom borders
        var paint = Paint()
        paint.setStyle(Paint.Style.STROKE)  // border - no fill
        paint.setColor(Color.BLACK)
        paint.setStrokeWidth(16F)
        // draw zoom border
        canvas.drawRect(0F, (bmp1.height-1024).toFloat(),1024F,  bmp1.height.toFloat(), paint);
        // test altitude for border color code
        paint.setColor(Color.CYAN)
        if (airCapture.airObjectAltitude > 1000) {
            paint.setColor(Color.GREEN)
        }
        else if (airCapture.airObjectAltitude < 1000 && airCapture.airObjectAltitude > 500) {
            paint.setColor(Color.YELLOW)
        }
        else if (airCapture.airObjectAltitude < 500) {
            paint.setColor(Color.RED)
        }
        paint.setStrokeWidth(48F)
        // overlay border
        canvas.drawRect(0F, 0F, bmp1.width.toFloat(), bmp1.height.toFloat(), paint);
        // draw measure results
        paint.setColor(Color.BLACK)
        paint.setStyle(Paint.Style.FILL_AND_STROKE)
        paint.setStrokeWidth(8F)
        // initial offsets, size
        var offsetX = 128F
        var offsetY = 512F
        var textsize = 256F

        // primary group: altitude, decibels
        paint.setTextSize(textsize)

        val altitude = airCapture.airObjectAltitude.toInt()
        canvas.drawText("Altitude->  $altitude  feet", offsetX, offsetY, paint)

        offsetY += textsize + textsize/2
        val decibel = airCapture.decibel.toInt()
        canvas.drawText("Decibels->  $decibel  dB", offsetX, offsetY, paint)
        // corollary group: camera angle - distance
        paint.setTextSize(textsize/2)

        offsetY += textsize
        val craftType = airCapture.craftType
        val craftId = airCapture.craftId
        val craftTypeText = "Aircraft->  $craftType   Id-> $craftId"
        canvas.drawText("$craftTypeText", offsetX, offsetY, paint)

        offsetY += textsize
        val cameraAngle = airCapture.cameraAngle
        val dist = airCapture.airObjectDistance.toInt()
        val cameraDistText = "CameraAngle->  $cameraAngle deg   Distance->  $dist feet"
        canvas.drawText("$cameraDistText", offsetX, offsetY, paint)

        // craft dimensions
        offsetY = (bmp1.height-1024) - (textsize * 2)
        val craftWingspan = airCapture.craftWingspan
        val craftLength = airCapture.craftLength
        val craftDimsText = "wingspan $craftWingspan feet X length $craftLength feet"
        canvas.drawText("$craftDimsText", offsetX, offsetY, paint)

        // measure WINGSPAN | LENGTH, HORIZONTAL | VERTICAL
        offsetY += textsize
        val craftOrientation = airCapture.craftOrientation  // WINGSPAN | LENGTH
        val measureDimension = airCapture.measureDimension  // HORIZONTAL | VERTICAL
        val measureText = "Measure->  $craftOrientation $measureDimension"
        canvas.drawText("$measureText", offsetX, offsetY, paint)

        // zoom w x h
        offsetX = 1024F + 128F
        offsetY += (textsize * 2)
        val zoomWidth = airCapture.zoomWidth
        val zoomHeight = airCapture.zoomHeight
        val zoomText = "Zoom w X h ->  $zoomWidth X $zoomHeight pixels"
        canvas.drawText("$zoomText", offsetX, offsetY, paint)

        return bmOverlay
    }

    private fun establishGestureDetector(context: Context, imageView: ImageView) {
        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent?): Boolean {
                Log.i("TAG", "establishGestureDetector onDown: ")
                // don't return false here or else none of the other gestures will work
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                Log.i("TAG", "establishGestureDetector onSingleTapConfirmed ZOOM IN...")
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                val x = e?.x?.roundToInt()
                val y = e?.y?.roundToInt()
                Log.i("TAG", "establishGestureDetector onLongPress: x $x, y $y")
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                Log.i("TAG", "establishGestureDetector onDoubleTap...")
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent?,
                distanceX: Float, distanceY: Float
            ): Boolean {
                Log.i("TAG", "establishGestureDetector nScroll: distanceX $distanceX distanceY $distanceY")
                return true
            }

            override fun onFling(
                event1: MotionEvent?, event2: MotionEvent?,
                velocityX: Float, velocityY: Float
            ): Boolean {
                val SWIPE_MIN_DISTANCE = 120
                val SWIPE_THRESHOLD_VELOCITY = 200

                Log.d("TAG", "establishGestureDetector onFling: velocityX $velocityX velocityY $velocityY")
                if (event1 != null && event2 != null) {
                    var newImageDisplayed = false
                    if (event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        Log.d("TAG", "establishGestureDetector onFling: LEFT SWIPE")
                        newImageDisplayed = navigateOverlay(1)      // next
                        if (!newImageDisplayed ) {
                            Toast.makeText(context, "End of Gallery", Toast.LENGTH_SHORT).show()
                        }
                    }  else if (event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                        Log.d("TAG", "establishGestureDetector onFling: RIGHT SWIPE")
                        newImageDisplayed = navigateOverlay(-1) // prev
                        if (!newImageDisplayed ) {
                            Toast.makeText(context, "Start of Gallery", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else if (event1.getY() - event2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        // UP SWIPE - delete image set
                        Log.d("TAG", "establishGestureDetector onFling: UP SWIPE")
                    }  else if (event2.getY() - event1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                        // DOWN SWIPE - refresh overlay
                        Log.d("TAG", "establishGestureDetector onFling: DOWN SWIPE")
                        Log.d("TAG", "establishGestureDetector refreshing overlay...")
                        refresh()
                    }

                }

                return true
            }

            override fun onShowPress(e: MotionEvent?) {
                Log.i("TAG", "establishGestureDetector onShowPress: ")
                return
            }
        })
        imageView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

}