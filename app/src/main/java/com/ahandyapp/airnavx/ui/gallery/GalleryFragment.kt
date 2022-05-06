package com.ahandyapp.airnavx.ui.gallery

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        // set camera button on-click listener
        val buttonRefresh = root.findViewById(R.id.button_refresh) as Button
        buttonRefresh.setOnClickListener {
            Toast.makeText(this.context, "refreshing overlay...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonRefresh.setOnClickListener refresh...")
            refresh()
        }

        // connect to inspectViewModel
        val viewModelT2: InspectViewModel by activityViewModels()
        inspectViewModel = viewModelT2

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}