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
import java.lang.Integer.min
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

    private lateinit var captureBitmap: Bitmap      // original capture bitmap
    private lateinit var referenceBitmap: Bitmap    // current reference bitmap
    private lateinit var inspectBitmap: Bitmap      // inspect view image bitmap

    private var referenceCenterX = 0
    private var referenceCenterY = 0
    private var referenceUpperLeftX = 0
    private var referenceUpperLeftY = 0

    private var zoomCount = 0
    private var zoomDeltaPixelX = 0
    private var zoomDeltaPixelY = 0
    private var zoomDeltaBase = 256
    private var zoomDeltaStep = 8
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
        referenceBitmap = captureBitmap
        inspectBitmap = captureBitmap
//        imageViewInspect.setImageBitmap(captureViewModel.fullBitmapArray[captureViewModel.gridPosition])
        imageViewInspect.setImageBitmap(captureBitmap)
        Log.d(TAG, "onCreateView captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")

        // TODO: image attri data object
        // set image attributes
        referenceUpperLeftX = 0
        referenceUpperLeftY = 0
        zoomCount = 0
        referenceCenterX = captureBitmap.width / 2
        referenceCenterY = captureBitmap.height / 2
        Log.d(TAG, "onCreateView center X/Y $referenceCenterX/$referenceCenterY")

        if (captureBitmap.width < captureBitmap.height) {
            dimRatio = (captureBitmap.width.toFloat() / captureBitmap.height.toFloat()).toDouble()
            zoomDeltaPixelX = (dimRatio * zoomDeltaBase).toInt()
            zoomDeltaPixelY = zoomDeltaBase
        }
        else if (captureBitmap.width > captureBitmap.height) {
            dimRatio = (captureBitmap.height.toFloat() / captureBitmap.width.toFloat()).toDouble()
            zoomDeltaPixelX = zoomDeltaBase
            zoomDeltaPixelY = (dimRatio * zoomDeltaBase).toInt()
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
                ++zoomCount
                Log.i("TAG", "onCreateView onSingleTapConfirmed ZOOM IN zoomCount $zoomCount")
                inspectZoomOnTap(InspectViewModel.ZoomDirection.IN.direction)
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                var x = e?.x?.roundToInt()
                var y = e?.y?.roundToInt()
                Log.i("TAG", "onCreateView onLongPress: x $x, y $y")
                if (x != null && y != null) {
                    centerZoomBitmap(x, y)
                }
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (zoomCount > 0) {
                    --zoomCount
                    Log.i("TAG", "onCreateView onDoubleTap zoomCount $zoomCount")
                    inspectZoomOnTap(InspectViewModel.ZoomDirection.OUT.direction)
                }
                else {
                    Log.i("TAG", "onCreateView onDoubleTap NO ZOOM OUT zoomCount $zoomCount")
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
        Log.d(TAG, "inspectZoomOnTap imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")

//        Log.d(TAG, "inspectZoomOnTap captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}")
//        inspectBitmap = zoomOnBitmap(captureBitmap, zoomDirection)
        Log.d(TAG, "inspectZoomOnTap referenceBitmap w/h ${referenceBitmap.width}/${referenceBitmap.height}")
        inspectBitmap = zoomOnBitmap(referenceBitmap, zoomDirection)
        Log.d(TAG, "inspectZoomOnTap inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")

        imageViewInspect.setImageBitmap(inspectBitmap)
    }

    private fun zoomOnBitmap(imageBitmap: Bitmap, zoomDirection: Int) : Bitmap {
        Log.d(TAG, "zoomOnBitmap imageBitmap w/h ${imageBitmap.width}/${imageBitmap.height}")
        Log.d(TAG, "zoomOnBitmap imageBitmap zoom direction $zoomDirection with zoomCount $zoomCount")
//        zoomCenterX = imageBitmap.width / 2
//        zoomCenterY = imageBitmap.height / 2
//        Log.d(TAG, "zoomOnBitmap center X/Y $zoomCenterX/$zoomCenterY")
        // determine width/height, upper left & create zoom bitmap
//        zoomUpperLeftX = zoomUpperLeftX + (zoomCount * (zoomDeltaPixelX/2))
//        zoomUpperLeftY = zoomUpperLeftY + (zoomCount * (zoomDeltaPixelY/2))
        var zoomUpperLeftX = referenceUpperLeftX + (zoomCount * (zoomDeltaPixelX/2))
        var zoomUpperLeftY = referenceUpperLeftY + (zoomCount * (zoomDeltaPixelY/2))
        Log.d(TAG, "zoomOnBitmap upper left X/Y $zoomUpperLeftX/$zoomUpperLeftY")
        // determine width/height, upper left & create zoom bitmap
        val width = imageBitmap.width - (zoomCount * zoomDeltaPixelX)
        val height = imageBitmap.height - (zoomCount * zoomDeltaPixelY)
        Log.d(TAG, "zoomOnBitmap next w/h $width/$height")
//        val width = imageBitmap.width + (zoomCount * (zoomDirection * zoomDeltaPixelX))
//        val height = imageBitmap.height + (zoomCount * (zoomDirection * zoomDeltaPixelY))
//        zoomCenterX = width / 2
//        zoomCenterY = height / 2
//        Log.d(TAG, "zoomOnBitmap next center left X/Y $zoomCenterX/$zoomCenterY")

        // create zoom bitmap
        var zoomBitmap = imageBitmap
//        if (zoomDirection == InspectViewModel.ZoomDirection.OUT.ordinal) {
//            zoomBitmap = captureBitmap
//            zoomCenterX = captureBitmap.width / 2
//            zoomCenterY = captureBitmap.height / 2
//            zoomUpperLeftX = zoomCenterX - (width / 2)
//            zoomUpperLeftY = zoomCenterY - (height / 2)
//        }
        try {
            zoomBitmap = Bitmap.createBitmap(imageBitmap, zoomUpperLeftX, zoomUpperLeftY, width, height)
        } catch (ex: Exception) {
            Log.e(TAG, "zoomOnBitmap Exception ${ex.message}")
        }
//        Log.d(TAG, "zoomOnBitmap zoomDeltaPixel pre-adjust X/Y $zoomDeltaPixelX/$zoomDeltaPixelY")
//        zoomDeltaBase = zoomDeltaBase + (zoomDirection * zoomDeltaStep)
//        stepZoomDelta(zoomDeltaBase)
//        Log.d(TAG, "zoomOnBitmap zoomDeltaPixel post adjust X/Y $zoomDeltaPixelX/$zoomDeltaPixelY")

        return zoomBitmap
    }

    // find width, height, upper left, zoomCount based on new center
    private fun centerZoomBitmap(centerX: Int, centerY: Int) {
        if (zoomCount > 0 ) {
            referenceBitmap = inspectBitmap
        }
        var imageBitmap = referenceBitmap
        Log.d(TAG, "centerZoomBitmap captureBitmap w/h ${imageBitmap.width}/${imageBitmap.height}")
        Log.d(TAG, "centerZoomBitmap imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")
        // translate imageView coords to image bitmap coords
        var viewBitmapRatioX = (imageBitmap.width.toFloat() / imageViewInspect.width.toFloat()).toDouble()
        var viewBitmapRatioY = (imageBitmap.height.toFloat() / imageViewInspect.height.toFloat()).toDouble()
        Log.d(TAG, "centerZoomBitmap ratio x/y $viewBitmapRatioX/$viewBitmapRatioY")

        referenceCenterX = (centerX * viewBitmapRatioX).toInt()
        referenceCenterY = (centerY * viewBitmapRatioY).toInt()
        Log.d(TAG, "centerZoomBitmap capture center x/y $referenceCenterX/$referenceCenterY")

//        val width = 512
//        val height = 512
        // find edge distances based on new center (may be off the edge of image)
        var leftEdgeDist = referenceCenterX
        var rightEdgeDist = imageBitmap.width - referenceCenterX
        var upperEdgeDist = referenceCenterY
        var lowerEdgeDist = imageBitmap.height - referenceCenterY
        Log.d(TAG, "centerZoomBitmap dist left/right $leftEdgeDist/$rightEdgeDist, upper/lower $upperEdgeDist/$lowerEdgeDist")
//        var width = rightEdgeDist + leftEdgeDist
//        var height = lowerEdgeDist + upperEdgeDist
//        // find zoomcount to generate width, height
//        var zoomedWidth = imageBitmap.width - width
//        var zoomedHeight = imageBitmap.height - height
//        var zoomCountX = zoomedWidth / zoomDeltaPixelX
//        var zoomCountY = zoomedHeight / zoomDeltaPixelY
//        Log.d(TAG, "centerZoomBitmap zoomed width/height $zoomedWidth/$zoomedHeight, zoomCount X/Y $zoomCountX/$zoomCountY")
////        zoomCount = min(zoomCountX,zoomCountY) + 2
//        zoomCount = min(zoomCountX,zoomCountY)
//        Log.d(TAG, "centerZoomBitmap zoomCount $zoomCount")
//        width = imageBitmap.width - (zoomCount * zoomDeltaPixelX)
//        height = imageBitmap.height - (zoomCount * zoomDeltaPixelY)
//        Log.d(TAG, "centerZoomBitmap width/height $width/$height")

        // find centered width height
        var width = leftEdgeDist + rightEdgeDist
        var height = upperEdgeDist + lowerEdgeDist
        Log.d(TAG, "centerZoomBitmap pre-bounds check width/height $width/$height")

        if (width < height && (width/height).toDouble() != dimRatio) {
            width = (height.toDouble() * dimRatio).toInt()
            Log.d(TAG, "centerZoomBitmap pre-bounds recalculated WIDTH/height $width/$height")
        }
        else if (height > width && (height/width).toDouble() != dimRatio) {
            height = (width.toDouble() * dimRatio).toInt()
            Log.d(TAG, "centerZoomBitmap pre-bounds recalculated width/HEIGHT $width/$height")
        }

        // set upper left
        referenceUpperLeftX = referenceCenterX - (width / 2)
        referenceUpperLeftY = referenceCenterY - (height / 2)
        Log.d(TAG, "centerZoomBitmap pre-bounds check upper left X/Y $referenceUpperLeftX/$referenceUpperLeftY")
        // adjust for off LEFT edge
        if (referenceUpperLeftX < 0) {
            width = width + (referenceUpperLeftX*2) // negative UL X subtracts from width
            referenceUpperLeftX = 0
            Log.d(TAG, "centerZoomBitmap off LEFT image edge, adjusted upper left X $referenceUpperLeftX, width $width")
        }
        // adjust for off RIGHT edge
        if (referenceUpperLeftX + width > imageBitmap.width) {
            val delta = (referenceUpperLeftX + width) - imageBitmap.width
            width = width - (delta*2)
            referenceUpperLeftX = referenceCenterX - (width / 2)
            Log.d(TAG, "centerZoomBitmap off RIGHT image edge, adjusted upper left X $referenceUpperLeftX, width $width")
        }
        // adjust for off UPPER edge
        if (referenceUpperLeftY < 0) {
            height = height + (referenceUpperLeftY*2) // negative UL X subtracts from width
            referenceUpperLeftY = 0
            Log.d(TAG, "centerZoomBitmap off UPPER image edge, adjusted upper left Y $referenceUpperLeftY, height $height")
        }
        // adjust for off LOWER edge
        if (referenceUpperLeftY + height > imageBitmap.height) {
            val delta = (referenceUpperLeftY + height) - imageBitmap.height
            height = height - (delta*2)
            referenceUpperLeftY = referenceCenterY - (height / 2)
            Log.d(TAG, "centerZoomBitmap off LOWER image edge, adjusted upper left X $referenceUpperLeftY, height $height")
        }
        Log.d(TAG, "centerZoomBitmap post-bounds check upper left X/Y $referenceUpperLeftX/$referenceUpperLeftY")
        Log.d(TAG, "centerZoomBitmap post-bounds width/height $width/$height")

        // create bitmap
        try {
            inspectBitmap = Bitmap.createBitmap(imageBitmap, referenceUpperLeftX, referenceUpperLeftY, width, height)
        } catch (ex: Exception) {
            Log.e(TAG, "inspectZoomOnTap Exception ${ex.message}")
            return
        }
        // TODO: init reference
        // reset reference bitmap
        referenceBitmap = inspectBitmap
        zoomCount = 0
        referenceUpperLeftX = 0
        referenceUpperLeftY = 0
        referenceCenterX = (width / 2)
        referenceCenterY = (height / 2)

//        Log.d(TAG, "zoomOnBitmap zoomDeltaPixel pre-adjust X/Y $zoomDeltaPixelX/$zoomDeltaPixelY")
//        zoomDeltaBase = zoomDeltaBase - (zoomDeltaBase / zoomDeltaStep)
//        stepZoomDelta(zoomDeltaBase)
//        Log.d(TAG, "zoomOnBitmap zoomDeltaPixel post adjust X/Y $zoomDeltaPixelX/$zoomDeltaPixelY")

        imageViewInspect.setImageBitmap(inspectBitmap)
        Log.d(TAG, "centerZoomBitmap inspectBitmap w/h ${inspectBitmap.width}/${inspectBitmap.height}")
        Log.d(TAG, "onCreateView imageViewInspect w/h ${imageViewInspect.width}/${imageViewInspect.height}")
    }

    private fun stepZoomDelta(base: Int) {
        if (captureBitmap.width < captureBitmap.height) {
            zoomDeltaPixelX = (dimRatio * zoomDeltaBase).toInt()
            zoomDeltaPixelY = zoomDeltaBase
        }
        else if (captureBitmap.width > captureBitmap.height) {
            zoomDeltaPixelX = zoomDeltaBase
            zoomDeltaPixelY = (dimRatio * zoomDeltaBase).toInt()
        }
        Log.d(TAG, "onCreateView ratio $dimRatio X/Y $zoomDeltaPixelX/$zoomDeltaPixelY")

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