package com.ahandyapp.airnavx.ui.home

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Camera
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.databinding.FragmentHomeBinding
import com.ahandyapp.airnavx.ui.sense.AngleMeter
import com.ahandyapp.airnavx.ui.sense.SoundMeter

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //
    lateinit var imageView: ImageView
    //////////////////
    // angle & location meters
    private var angleMeter = AngleMeter()
    private var soundMeter = SoundMeter()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        // get reference to button
        val buttonCameraIdString = "button_camera"
        val packageName = this.context?.getPackageName()
        val buttonCameraId = resources.getIdentifier(buttonCameraIdString, "id", packageName)
        val buttonCamera = root.findViewById(buttonCameraId) as Button
        // set on-click listener
        buttonCamera.setOnClickListener {
            Toast.makeText(this.context, "launching camera...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonCamera.setOnClickListener launching camera...")
            dispatchTakePictureIntent()
        }
        val imageViewIdString = "imageView2"
        val imageViewId = resources.getIdentifier(imageViewIdString, "id", packageName)
        imageView = root.findViewById(imageViewId) as ImageView

        //////////////////
        // angle meter one-time init
        angleMeter.create(requireActivity())
        //////////////////

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //////////////////
    // onResume
    override fun onResume() {
        super.onResume()

        // start angle meter
        angleMeter.start()
        Log.d(TAG, "onResume angleMeter started.")
        // start sound meter
        this.context?.let { soundMeter.start(it) }
        Log.d(TAG, "onResume soundMeter started.")

    }

    override fun onPause() {
        super.onPause()

//        // stop angle meter
//        angleMeter.stop()
//        // stop sound meter
//        soundMeter.stop()
    }

    val REQUEST_IMAGE_CAPTURE = 1

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            // exercise meters
            var angle = angleMeter.getAngle()
            Log.d(TAG, "onActivityResult angleMeter.getAngle ->${angle.toString()}")
            var db = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG, "onActivityResult soundMeter.deriveDecibel db->${db.toString()}")
        } catch (e: ActivityNotFoundException) {
            // display error state
            Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent -> NO camera launch...")
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this.context, "camera image captured...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera image captured...")
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)

            // capture meters
            var angle = angleMeter.getAngle()
            Log.d(TAG, "onActivityResult angleMeter.getAngle ->${angle.toString()}")
            var db = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG, "onActivityResult soundMeter.deriveDecibel db->${db.toString()}")

            // loop dispatch until cancelled
            Log.d(TAG, "onActivityResult launching camera...")
            dispatchTakePictureIntent()
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            Toast.makeText(this.context, "camera canceled...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera canceled...")

        }
    }
}