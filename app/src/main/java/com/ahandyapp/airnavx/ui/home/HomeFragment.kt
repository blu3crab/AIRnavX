package com.ahandyapp.airnavx.ui.home

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
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
import com.ahandyapp.airnavx.model.AirCapture
import com.ahandyapp.airnavx.ui.sense.AngleMeter
import com.ahandyapp.airnavx.ui.sense.SoundMeter
import com.google.gson.Gson
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Matrix

import android.media.ExifInterface
import androidx.lifecycle.MutableLiveData


class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private lateinit var textViewPreview: TextView
    private lateinit var textViewDecibel: TextView
    private lateinit var textViewAngle: TextView

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    //////////////////
    // angle & location meters
    private var angleMeter = AngleMeter()
    private var soundMeter = SoundMeter()

    // air capture
    lateinit var airCapture: AirCapture

    // data type defaults
    val DEFAULT_DATAFILE_EXT = "json"
    val DEFAULT_STRING = "nada"
    val DEFAULT_DOUBLE = 0.0
    val DEFAULT_INT = 0
    val DEFAULT_FLOAT_ARRAY: FloatArray = floatArrayOf(0.0F, 0.0F)
    val DEFAULT_FLOAT = 0.0F

    // image capture
    val REQUEST_IMAGE_CAPTURE = 1001
    lateinit var currentPhotoPath: String
    lateinit var storageDir: File

    // photo thumb
    private lateinit var imageViewPreview: ImageView       // thumb photo display
    private lateinit var photoFile: File            // photo file
    private lateinit var photoUri: Uri              // photo URI

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val textViewPreview: TextView = binding.textPreview
        textViewPreview = binding.textPreview
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textViewPreview.text = it
        })
        //val textViewDecibel: TextView = binding.textDecibel
        textViewDecibel = binding.textDecibel
        homeViewModel.decibel.observe(viewLifecycleOwner, Observer {
            textViewDecibel.text = it
        })
        //val textViewAngle: TextView = binding.textAngle
        textViewAngle = binding.textAngle
        homeViewModel.angle.observe(viewLifecycleOwner, Observer {
            textViewAngle.text = it
        })

        // get reference to button
        val buttonCameraIdString = "button_camera"
        val packageName = this.context?.getPackageName()
        val buttonCameraId = resources.getIdentifier(buttonCameraIdString, "id", packageName)
        val buttonCamera = root.findViewById(buttonCameraId) as Button
        // set on-click listener
        buttonCamera.setOnClickListener {
            // init AirCapture data class
            airCapture = initAirCapture();
            Toast.makeText(this.context, "launching camera...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonCamera.setOnClickListener launching camera...")
            dispatchTakePictureIntent()
        }
        val imageViewIdString = "imageViewPreview"
        val imageViewId = resources.getIdentifier(imageViewIdString, "id", packageName)
        imageViewPreview = root.findViewById(imageViewId) as ImageView

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

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume...")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause...")
    }

    fun initAirCapture(): AirCapture {
        val airCapture = AirCapture(
            DEFAULT_STRING,
            DEFAULT_STRING,
            DEFAULT_STRING,
            DEFAULT_DOUBLE,
            DEFAULT_INT,
            DEFAULT_INT,
            DEFAULT_INT,
            DEFAULT_FLOAT_ARRAY,
            DEFAULT_FLOAT,
            DEFAULT_FLOAT,
            DEFAULT_FLOAT
        )
        return airCapture
    }

    private fun dispatchTakePictureIntent() {
        val THUMBNAIL_ONLY = false
        try {
            // start angle meter
            angleMeter.start()
            Log.d(TAG, "dispatchTakePictureIntent angleMeter started.")
            // start sound meter
            this.context?.let { soundMeter.start(it) }
            Log.d(TAG, "dispatchTakePictureIntent soundMeter started.")

            // exercise meters
            airCapture.cameraAngle = angleMeter.getAngle()
            Log.d(
                TAG,
                "dispatchTakePictureIntent angleMeter.getAngle ->${airCapture.cameraAngle.toString()}"
            )
            airCapture.decibel = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(
                TAG,
                "dispatchTakePictureIntent soundMeter.deriveDecibel db->${airCapture.decibel.toString()}"
            )
        } catch (ex: Exception) {
            Log.e(TAG, "dispatchTakePictureIntent Meter Exception ${ex.stackTrace}")
        }
        if (!THUMBNAIL_ONLY) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                // create the photo File
                try {
                    photoFile = createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "dispatchTakePictureIntent IOException ${ex.stackTrace}")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.ahandyapp.airnavx",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } catch (e: ActivityNotFoundException) {
                // display error state
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "dispatchTakePictureIntent -> NO camera launch...")
            }
        } else {  // THUMBNAIL_ONLY
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: ActivityNotFoundException) {
                // display error state
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "dispatchTakePictureIntent -> NO camera launch...")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(
            TAG,
            "dispatchTakePictureIntent onActivityResult requestCode ${requestCode}, resultCode ${resultCode}"
        )
        //var imageBitmap: Bitmap?
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this.context, "camera image captured...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera image captured...")
            // if (data != null) {
            data?.let {
                // extra will contain thumbnail image if image capture not in play
                data.extras?.let {
                    val extraPhotoUri: Uri = data.extras?.get(MediaStore.EXTRA_OUTPUT) as Uri
                    Log.d(
                        TAG,
                        "dispatchTakePictureIntent onActivityResult thumb URI $extraPhotoUri"
                    )

                    var imageBitmap = data.extras?.get("data") as Bitmap
                    imageViewPreview.setImageBitmap(imageBitmap)
                } ?: run {
                    Log.d(TAG, "dispatchTakePictureIntent onActivityResult generate thumbnail...")
                    // generate thumbnail from file uri if not available as thumbnail
                    // photo file is file EXTRA_OUTPUT location of actual camera image
                    val uri = Uri.fromFile(photoFile)
                    var imageBitmap: Bitmap?
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(
                            activity?.applicationContext?.contentResolver, uri
                        )
                        //imageBitmap = cropAndScale(imageBitmap, 768)
                        // TODO: genThumbnail(imageBitmap): Bitmap
                        var thumbBitmap = genThumbnail(imageBitmap,5)
//                        var thumbBitmap: Bitmap? = null
//                        imageBitmap?.let {
//                            val width = (imageBitmap.width)?.div(5)
//                            val height = (imageBitmap.height)?.div(5)
//                            thumbBitmap = ThumbnailUtils.extractThumbnail(
//                                BitmapFactory.decodeFile(currentPhotoPath),
//                                width,
//                                height
//                            )
//                        }

//                        imageViewPreview.setImageBitmap(imageBitmap)
                        var sourceBitmap: Bitmap = imageBitmap
                        thumbBitmap?.let {
//                            val bm = imageBitmap
                            sourceBitmap = thumbBitmap as Bitmap
                        }
                        var bMapRotate: Bitmap? = null
                        val mat = Matrix()
                        mat.postRotate(90F)
                        bMapRotate = Bitmap.createBitmap(
                            sourceBitmap,
                            0,
                            0,
                            sourceBitmap.getWidth(),
                            sourceBitmap.getHeight(),
                            mat,
                            true
                        )
                        imageViewPreview.setImageBitmap(bMapRotate)

//                        imageViewPreview.setImageBitmap(imageBitmap)

//                        thumbImage?.let {
//                            imageViewPreview.setImageBitmap(thumbImage)
//                            Log.d(TAG, "dispatchTakePictureIntent onActivityResult thumbImage extracted...")
//                        }

                        // Bitmap resized = ThumbnailUtils.extractThumbnail(sourceBitmap, width, height);
                        //imageBitmap = ThumbnailUtils.extractThumbnail()
                        // Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getPath()), width, height);
                    } catch (e: FileNotFoundException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }
                }
            } ?: run {
                Log.e(TAG, "dispatchTakePictureIntent onActivityResult data NULL.")
            }

            // TODO: extractEXIF(photoFile): Boolean
            try {
                // extract EXIF attributes from photoFile
                var rotate = 0

                val exif = ExifInterface(photoFile.getAbsolutePath())
                val orientation: Int = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
                }

                Log.i(
                    TAG,
                    "dispatchTakePictureIntent onActivityResult Exif orientation: $orientation"
                )
                Log.i(TAG, "dispatchTakePictureIntent onActivityResult Rotate value: $rotate")
                var latlong: FloatArray = floatArrayOf(0.0F, 0.0F)
                exif.getLatLong(latlong)
                Log.d(TAG, "dispatchTakePictureIntent EXIF ->${latlong[0]} , ${latlong[1]}")
                // update airCapture data
                airCapture.exifOrientation = orientation
                airCapture.exifRotation = rotate
                airCapture.exifLatLon = latlong
                // update viewModel
                textViewPreview.text = airCapture.timestamp
                textViewDecibel.text = airCapture.decibel.toString()
                textViewAngle.text = airCapture.cameraAngle.toString()
            } catch (ex: Exception) {
                Log.e(TAG, "dispatchTakePictureIntent Meter Exception ${ex.stackTrace}")
            }

            try {
                // capture meters
                airCapture.cameraAngle = angleMeter.getAngle()
                Log.d(
                    TAG,
                    "dispatchTakePictureIntent angleMeter.getAngle ->${airCapture.cameraAngle.toString()}"
                )
                airCapture.decibel = soundMeter.deriveDecibel(forceFormat = true)
                Log.d(
                    TAG,
                    "dispatchTakePictureIntent soundMeter.deriveDecibel db->${airCapture.decibel.toString()}"
                )
                // update viewModel
                textViewPreview.text = airCapture.timestamp
                textViewDecibel.text = airCapture.decibel.toString() + " dB"
                textViewAngle.text = airCapture.cameraAngle.toString() + " degrees"
            } catch (ex: Exception) {
                Log.e(TAG, "dispatchTakePictureIntent Meter Exception ${ex.stackTrace}")
            }

            // TODO: captureMeters(airCapture): Boolean

            // TODO: refreshViewModel(airCapture): Boolean

            // TODO: recordCapture(airCapture): Boolean
            // transform AirCapture data class to json
            val jsonCapture = Gson().toJson(airCapture)
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult $jsonCapture")
            // format AirCapture name & write json file
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult fileDir->$context.filesDir()")
            File(context?.filesDir, "aircapture.json").printWriter().use { out ->
                out.println("$jsonCapture")
            }
            var name = "AIR-" + airCapture.timestamp + "." + DEFAULT_DATAFILE_EXT
            File(storageDir, name).printWriter().use { out ->
                out.println("$jsonCapture")
            }
            // loop dispatch until cancelled
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult launching camera...")
            dispatchTakePictureIntent()
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
            Toast.makeText(this.context, "camera canceled...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera canceled...")
            // stop angle meter
            angleMeter.stop()
            // stop sound meter
            soundMeter.stop()
        }
    }

    private fun cropAndScale(source: Bitmap, scale: Int): Bitmap? {
        var source = source
        val factor = if (source.height <= source.width) source.height else source.width
        val longer = if (source.height >= source.width) source.height else source.width
        val x = if (source.height >= source.width) 0 else (longer - factor) / 2
        val y = if (source.height <= source.width) 0 else (longer - factor) / 2
        source = Bitmap.createBitmap(source, x, y, factor, factor)
        source = Bitmap.createScaledBitmap(source, scale, scale, false)
        return source
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        var context = getContext()
        // Create an image file name
        airCapture.timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        airCapture.imagePath = Environment.DIRECTORY_PICTURES
        airCapture.imageName = "AIR-" + airCapture.timestamp + ".jpg"
        storageDir = context?.getExternalFilesDir(airCapture.imagePath)!!
        Log.d(TAG, "createImageFile storageDir->${storageDir.toString()}")

        val imageFile = File(storageDir, airCapture.imageName)
        currentPhotoPath = imageFile.absolutePath
        return imageFile

//        return File.createTempFile(
//            "JPEG_${timeStamp}_", /* prefix */
//            ".jpg", /* suffix */
//            storageDir /* directory */
//        ).apply {
//            // Save a file: path for use with ACTION_VIEW intents
//            currentPhotoPath = absolutePath
//            Log.d(TAG, "createImageFile imagePath->$currentPhotoPath")
//        }
    }

    private fun genThumbnail(imageBitmap: Bitmap, scaleFactor: Int): Bitmap {
        val width = (imageBitmap.width)?.div(scaleFactor)
        val height = (imageBitmap.height)?.div(scaleFactor)
        val thumbBitmap = ThumbnailUtils.extractThumbnail(
            BitmapFactory.decodeFile(currentPhotoPath),
            width,
            height
        )
        thumbBitmap?.let { Log.d(TAG, "genThumbnail $thumbBitmap at scale factor $scaleFactor") }
            .run { Log.e(TAG, "genThumbnail NULL at scale factor $scaleFactor")  }
        return thumbBitmap
    }

}