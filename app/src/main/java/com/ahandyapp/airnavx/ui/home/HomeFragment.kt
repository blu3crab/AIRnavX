package com.ahandyapp.airnavx.ui.home

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Camera
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ahandyapp.airnavx.databinding.FragmentHomeBinding
import com.ahandyapp.airnavx.ui.sense.AngleMeter
import com.ahandyapp.airnavx.ui.sense.SoundMeter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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
    ): View {
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
        val THUMBNAIL_ONLY = false
        // exercise meters
        var angle = angleMeter.getAngle()
        Log.d(TAG, "dispatchTakePictureIntent angleMeter.getAngle ->${angle.toString()}")
        var db = soundMeter.deriveDecibel(forceFormat = true)
        Log.d(TAG, "dispatchTakePictureIntent soundMeter.deriveDecibel db->${db.toString()}")

        if (!THUMBNAIL_ONLY) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "dispatchTakePictureIntent IOException ${ex.stackTrace}")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.ahandyapp.airnavx",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } catch (e: ActivityNotFoundException) {
                // display error state
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "dispatchTakePictureIntent -> NO camera launch...")
            }
        }
        else {  // THUMBNAIL_ONLY
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: ActivityNotFoundException) {
                // display error state
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "dispatchTakePictureIntent -> NO camera launch...")
            }
        }

//    private fun dispatchTakePictureIntent() {
//        // exercise meters
//        var angle = angleMeter.getAngle()
//        Log.d(TAG, "dispatchTakePictureIntent angleMeter.getAngle ->${angle.toString()}")
//        var db = soundMeter.deriveDecibel(forceFormat = true)
//        Log.d(TAG, "dispatchTakePictureIntent soundMeter.deriveDecibel db->${db.toString()}")
//        // define intent
//        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
//            // Ensure that there's a camera activity to handle the intent
//            var packageManager = context?.getPackageManager()
//            if (packageManager != null) {
//                Log.d(TAG, "dispatchTakePictureIntent resolveActivity ${takePictureIntent.resolveActivity(packageManager)}")
//                takePictureIntent.resolveActivity(packageManager)?.also {
//                    // Create the File where the photo should go
//                    val photoFile: File? = try {
//                        createImageFile()
//                    } catch (ex: IOException) {
//                        // Error occurred while creating the File
//                        Log.e(TAG, "dispatchTakePictureIntent IOException ${ex.stackTrace}")
//                        null
//                    }
//                    // Continue only if the File was successfully created
//                    photoFile?.also {
//                        val photoURI: Uri = FileProvider.getUriForFile(
//                            requireContext(),
//                            "com.ahandyapp.airnavx",
//                            it
//                        )
//                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//                    }
//                }
//            }
//        }

//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        try {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//        } catch (e: ActivityNotFoundException) {
//            // display error state
//            Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
//            Log.d(TAG, "dispatchTakePictureIntent -> NO camera launch...")
//        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "dispatchTakePictureIntent onActivityResult requestCode ${requestCode}, resultCode ${resultCode}")
        var imageBitmap: Bitmap? = null
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this.context, "camera image captured...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera image captured...")
            //if (data != null) {
            data?.let {
                data.extras?.let {
//                    val imageBitmap = data.extras?.get("data") as Bitmap
                    imageBitmap = data.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(imageBitmap)
                } ?: run {
                    Log.e(TAG, "dispatchTakePictureIntent onActivityResult data.extras NULL.")
                }
            } ?: run {
                Log.e(TAG, "dispatchTakePictureIntent onActivityResult data NULL.")
            }
            // generate thumbnail from file uri if not available as thumbnail
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult generate thumbnail...")
            // TODO: read photo into bitmap

            // Bitmap resized = ThumbnailUtils.extractThumbnail(sourceBitmap, width, height);
            //imageBitmap = ThumbnailUtils.extractThumbnail()
            // Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getPath()), width, height);

            // capture meters
            var angle = angleMeter.getAngle()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult angleMeter.getAngle ->${angle.toString()}")
            var db = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult soundMeter.deriveDecibel db->${db.toString()}")

            // loop dispatch until cancelled
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult launching camera...")
            dispatchTakePictureIntent()
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            Toast.makeText(this.context, "camera canceled...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera canceled...")

        }
    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        var context = getContext()
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        Log.d(TAG, "createImageFile storageDir->${storageDir.toString()}")
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
}