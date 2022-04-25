package com.ahandyapp.airnavx.ui.gallery

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.databinding.FragmentGalleryBinding
import com.ahandyapp.airnavx.model.AirCapture
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

//        val textView: TextView = binding.textGallery
//        galleryViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })
        // establish gallery imageview
        galleryImageView = root.findViewById(R.id.imageview_gallery) as ImageView

        // connect to captureViewModel
        val viewModelT1: CaptureViewModel by activityViewModels()
        captureViewModel = viewModelT1

        airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
        Log.d(TAG, "onCreateView captureViewModel airCapture $airCapture")

        // set inspect image to selected capture thumb
        Log.d(TAG, "onCreateView captureViewModel grid position ${captureViewModel.gridPosition}")
        captureBitmap = captureViewModel.fullBitmapArray[captureViewModel.gridPosition]
        galleryBitmap = captureBitmap
        galleryImageView.setImageBitmap(captureBitmap)
        Log.d(TAG, "onCreateView captureBitmap w/h ${captureBitmap.width}/${captureBitmap.height}")
        Log.d(TAG, "onCreateView imageViewGallery w/h ${galleryImageView.width}/${galleryImageView.height}")

        // connect to inspectViewModel
        val viewModelT2: InspectViewModel by activityViewModels()
        inspectViewModel = viewModelT2
        // TODO: inspectViewModel stores zoom array?
        zoomBitmap = captureBitmap

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}