package com.ahandyapp.airnavx.ui.inspect

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.databinding.FragmentInspectBinding
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import android.view.MotionEvent
import kotlin.math.roundToInt


class InspectFragment : Fragment() {

    private val TAG = "InspectFragment"

    private lateinit var inspectViewModel: InspectViewModel
    private var _binding: FragmentInspectBinding? = null

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var captureViewModel: CaptureViewModel

    private lateinit var imageViewInspect: ImageView
    private lateinit var textViewInspect: TextView

    private lateinit var captureBitmap: Bitmap
    private lateinit var inspectBitmap: Bitmap

    private var zoomCenterX = 0
    private var zoomCenterY = 0
    private var zoomUpperLeftX = 0
    private var zoomUpperLeftY = 0
    private var zoomDeltaPixelX = 0
    private var zoomDeltaPixelY = 0
    private var dimRatio = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        inspectViewModel = ViewModelProvider(this).get(InspectViewModel::class.java)

        _binding = FragmentInspectBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textInspect
        inspectViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        val packageName = this.context?.getPackageName()

        // establish inspect imageview
        val inspectViewIdString = "imageview_inspect"
        val inspectViewId = resources.getIdentifier(inspectViewIdString, "id", packageName)
        imageViewInspect = root.findViewById(inspectViewId) as ImageView

        // reference capture viewmodel
        Log.d(TAG, "onCreateView captureViewModel access...")

        //var captureViewModel = ViewModelProvider(this).get(CaptureViewModel::class.java)
        val viewModel: CaptureViewModel by activityViewModels()
        captureViewModel = viewModel

        // set inspect image to selected capture thumb
        Log.d(TAG, "onCreateView captureViewModel grid position ${captureViewModel.gridPosition}")
        captureBitmap = captureViewModel.fullBitmapArray[captureViewModel.gridPosition]
        inspectBitmap = captureBitmap
//        imageViewInspect.setImageBitmap(captureViewModel.fullBitmapArray[captureViewModel.gridPosition])
        imageViewInspect.setImageBitmap(inspectBitmap)
        Log.d(TAG, "onCreateView inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")

        // set image attributes
        zoomCenterX = inspectBitmap.width / 2
        zoomCenterY = inspectBitmap.height / 2
        Log.d(TAG, "onCreateView center X/Y $zoomCenterX/$zoomCenterY")

        if (inspectBitmap.width < inspectBitmap.height) {
            dimRatio = (inspectBitmap.width.toFloat() / inspectBitmap.height.toFloat()).toDouble()
            zoomDeltaPixelX = (dimRatio * 256).toInt()
            zoomDeltaPixelY = 256
        }
        else if (inspectBitmap.width > inspectBitmap.height) {
            dimRatio = (inspectBitmap.height.toFloat() / inspectBitmap.width.toFloat()).toDouble()
            zoomDeltaPixelX = 256
            zoomDeltaPixelY = (dimRatio * 256).toInt()
        }
        Log.d(TAG, "onCreateView ratio $dimRatio X/Y $zoomDeltaPixelX/$zoomDeltaPixelY")

        // establish inspect image gesture detector
        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent?): Boolean {
                Log.i("TAG", "onCreateView onDown: ")
                // don't return false here or else none of the other gestures will work
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                Log.i("TAG", "onCreateView onSingleTapConfirmed: ")
                inspectZoomOnTap(InspectViewModel.ZoomDirection.IN.direction)
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                var x = e?.x?.roundToInt()
                var y = e?.y?.roundToInt()
                Log.i("TAG", "onCreateView onLongPress: x $x, y $y")
//                if (x != null && y != null) {
//                    centerZoomBitmap(x, y)
//                }
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                Log.i("TAG", "onCreateView onDoubleTap: ")
                inspectZoomOnTap(InspectViewModel.ZoomDirection.OUT.direction)
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
        imageViewInspect.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
          // TODO: pinch/zoom?
//        imageViewFull.setOnTouchListener { v, event ->
//            decodeTouchAction(event)
//            true
//        }
//
        return root
    }

    private fun inspectZoomOnTap(zoomDirection: Int) {

        inspectBitmap = zoomOnBitmap(inspectBitmap, zoomDirection)
        Log.d(TAG, "inspectZoomOnTap inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")

        imageViewInspect.setImageBitmap(inspectBitmap)
        Log.d(TAG, "inspectZoomOnTap captureBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")
        Log.d(TAG, "inspectZoomOnTap imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")

    }

    private fun zoomOnBitmap(imageBitmap: Bitmap, zoomDirection: Int) : Bitmap {
        Log.d(TAG, "zoomOnBitmap imageBitmap w/h ${imageBitmap.width}/${imageBitmap.height}")
        Log.d(TAG, "zoomOnBitmap imageBitmap zoom direction $zoomDirection")
        zoomCenterX = imageBitmap.width / 2
        zoomCenterY = imageBitmap.height / 2
        Log.d(TAG, "zoomOnBitmap center X/Y $zoomCenterX/$zoomCenterY")
        // determine width/height, upper left & create zoom bitmap
        val width = imageBitmap.width + (zoomDirection * zoomDeltaPixelX)
        val height = imageBitmap.height + (zoomDirection * zoomDeltaPixelY)
        zoomUpperLeftX = zoomCenterX - width / 2
        zoomUpperLeftY = zoomCenterY - height / 2
        Log.d(TAG, "zoomOnBitmap next w/h $width/$height upper left X/Y $zoomUpperLeftX/$zoomUpperLeftY")
        zoomCenterX = width / 2
        zoomCenterY = height / 2
        Log.d(TAG, "zoomOnBitmap next center left X/Y $zoomCenterX/$zoomCenterY")

        var zoomBitmap = imageBitmap
        if (zoomDirection == InspectViewModel.ZoomDirection.OUT.ordinal) {
            zoomBitmap = captureBitmap
            zoomCenterX = captureBitmap.width / 2
            zoomCenterY = captureBitmap.height / 2
            zoomUpperLeftX = zoomCenterX - (width / 2)
            zoomUpperLeftY = zoomCenterY - (height / 2)
        }
        try {
            zoomBitmap = Bitmap.createBitmap(imageBitmap, zoomUpperLeftX, zoomUpperLeftY, width, height)
        } catch (ex: Exception) {
            Log.e(TAG, "inspectZoomOnTap Exception ${ex.message}")
        }
//        return Bitmap.createBitmap(imageBitmap, zoomUpperLeftX, zoomUpperLeftY, width, height)
        return zoomBitmap
    }

    private fun centerZoomBitmap(centerX: Int, centerY: Int) {
        zoomCenterX = centerX
        zoomCenterY = centerY

        val width = 512
        val height = 512
        zoomUpperLeftX = zoomCenterX - width / 2
        zoomUpperLeftY = zoomCenterY - height / 2
        inspectBitmap = Bitmap.createBitmap(inspectBitmap, zoomUpperLeftX, zoomUpperLeftY, width, height)
        imageViewInspect.setImageBitmap(inspectBitmap)
        Log.d(TAG, "centerZoomBitmap zoomBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")
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
        var pDownX=0
        var pDownY=0
        var pUpX=0
        var pUpY=0
        var pMoveX=0
        var pMoveY=0

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