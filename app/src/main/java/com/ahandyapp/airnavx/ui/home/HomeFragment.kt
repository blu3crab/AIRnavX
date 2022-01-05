package com.ahandyapp.airnavx.ui.home

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
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
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Matrix

import android.media.ExifInterface
import android.view.MotionEvent
import android.widget.*
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.ui.grid.GridViewAdapter


class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private lateinit var textViewPreview: TextView
    private lateinit var textViewDecibel: TextView
    private lateinit var textViewAngle: TextView

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // test thumb only image capture
    private val THUMBNAIL_ONLY = false

    //////////////////
    // angle & location meters
    private var angleMeter = AngleMeter()
    private var soundMeter = SoundMeter()

    // air capture
    //lateinit var airCapture: AirCapture

    // image capture
    val REQUEST_IMAGE_CAPTURE = 1001
    lateinit var currentPhotoPath: String
    lateinit var storageDir: File

    private lateinit var photoFile: File           // photo file
    private lateinit var photoUri: Uri              // photo URI

    private var timestamp: String = "nada"
    // TODO: migrate preview view to model
    private lateinit var imageViewPreview: ImageView       // preview image display

    // TODO: full, thumb image collections
    private var airCaptureBitmap: Bitmap? = null
    private var thumbBitmap: Bitmap? = null

    // TODO: gridview
    private lateinit var gridView: GridView
    private lateinit var blankBitmap: Bitmap

    ///////////////////////////////////////////////////////////////////////////
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
//            // init AirCapture data class
//            airCapture = initAirCapture();
            Toast.makeText(this.context, "launching camera...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonCamera.setOnClickListener launching camera...")
            dispatchTakePictureIntent()
        }
        val previewViewIdString = "imageViewPreview"
        val previewViewId = resources.getIdentifier(previewViewIdString, "id", packageName)
        imageViewPreview = root.findViewById(previewViewId) as ImageView

        // TODO: onclick listener actions - launch sizer frag?
        imageViewPreview.setOnTouchListener { v, event ->
            decodeTouchAction(event)
            true
        }
        // establish grid view, initialize gridViewAdapter
        gridView = root.findViewById(R.id.gridView)

        blankBitmap = createBlankBitmap(homeViewModel.DEFAULT_BLANK_GRID_WIDTH,homeViewModel.DEFAULT_BLANK_GRID_HEIGHT)
        val airCapture = createAirCapture();
        // TODO: gridview init fun v
        if (blankBitmap != null) {
            homeViewModel.gridBitmapArray.add(blankBitmap)
            ++homeViewModel.gridCount
            homeViewModel.gridLabelArray.add("thumb${homeViewModel.gridCount.toString()}")
            homeViewModel.fullBitmapArray.add(blankBitmap)
            homeViewModel.airCaptureArray.add(airCapture)
        }
        updateGridViewAdapter(gridView, homeViewModel.gridLabelArray, homeViewModel.gridBitmapArray)
        // TODO: gridview init fun ^

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

    private fun dispatchTakePictureIntent() {
        // start/exercise angle & sound meters
//        val startExerciseMeters = startExerciseMeters(angleMeter, soundMeter, airCapture)
        val startMeters = startMeters(angleMeter, soundMeter)
        Log.d(TAG, "dispatchTakePictureIntent startExerciseMeters ${startMeters}")

        if (!THUMBNAIL_ONLY) {
            // for full image capture, supply created image file URI
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                // create the photo File
                timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val imageFile = createImageFile(timestamp)
                imageFile?.let {
                    photoFile = imageFile
                    photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.ahandyapp.airnavx",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } catch (ex: Exception) {
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "dispatchTakePictureIntent -> NO camera launch Exception ${ex.stackTrace}...")
            }
        } else {  // THUMBNAIL_ONLY
            // for thumbnail only, do NOT supply image file URI
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (ex: Exception) {
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "dispatchTakePictureIntent -> NO camera launch Exception ${ex.stackTrace}...")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG,"dispatchTakePictureIntent onActivityResult requestCode ${requestCode}, resultCode ${resultCode}")
        // request code match & result OK
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this.context, "camera image captured...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult camera image captured...")
            if (!THUMBNAIL_ONLY) {
                // generate thumbnail from file uri if not available as thumbnail in data
                // photo file is file EXTRA_OUTPUT location of actual camera image
                Log.d(TAG, "dispatchTakePictureIntent onActivityResult generate thumbnail...")
                val uri = Uri.fromFile(photoFile)
                try {
                    airCaptureBitmap = MediaStore.Images.Media.getBitmap(
                        activity?.applicationContext?.contentResolver,
                        uri
                    )
                    // TODO: retain full bitmap set
                    if (airCaptureBitmap != null) {
                        // create initialized aircapture object
                        var airCapture = createAirCapture()
                        // capture image attributes
//                        airCapture.timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                        airCapture.timestamp = timestamp
                        airCapture.imagePath = Environment.DIRECTORY_PICTURES
                        airCapture.imageName = "AIR-" + airCapture.timestamp + ".jpg"
                        // update image length & width prior to thumbnail extraction
                        airCapture.imageWidth = airCaptureBitmap!!.width
                        airCapture.imageHeight = airCaptureBitmap!!.height
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult width ${airCapture.imageWidth} X height ${airCapture.imageHeight}")

                        /////////////////////////////////////
                        // extractEXIF(photoFile): Boolean
                        val exifExtracted = extractExif(photoFile, airCapture)
                        Log.d(TAG, "dispatchTakePictureIntent onActivityResult exifExtracted ${exifExtracted}")

                        // extract thumbnail at scale factor
                        thumbBitmap = extractThumbnail(airCaptureBitmap!!, homeViewModel.THUMB_SCALE_FACTOR)

                        // rotateBitmap(imageBitmap, rotationDegrees): Bitmap
                        if (thumbBitmap != null) {
                            // captureMeters(airCapture): Boolean
                            val metersCaptured = captureMeters(airCapture)
                            Log.d(TAG,"dispatchTakePictureIntent onActivityResult metersCaptured ${metersCaptured}")

                            // rotate thumb bitmap
                            thumbBitmap = rotateBitmap(thumbBitmap!!, airCapture.exifRotation.toFloat())
                            imageViewPreview.setImageBitmap(thumbBitmap)

                            // TODO: gridview update fun v
                            // insert thumb in grid view
                            ++homeViewModel.gridCount
                            homeViewModel.gridLabelArray.add("thumb${homeViewModel.gridCount.toString()}")
                            homeViewModel.gridBitmapArray.add(0, thumbBitmap!!)
                            updateGridViewAdapter(gridView, homeViewModel.gridLabelArray, homeViewModel.gridBitmapArray)

                            // rotate full bitmap
                            var fullBitmap = rotateBitmap(airCaptureBitmap!!, airCapture.exifRotation.toFloat())
                            // insert full bitmap into array
                            homeViewModel.fullBitmapArray.add(0, fullBitmap!!)
                            homeViewModel.airCaptureArray.add(0, airCapture)
                            // TODO: gridview update fun ^

                            // recordCapture(airCapture): Boolean
                            val captureRecorded = recordCapture(airCapture)
                            Log.d(TAG,"dispatchTakePictureIntent onActivityResult captureRecorded ${captureRecorded}")

                            // refresh viewmodel
                            val refreshResult = refreshViewModel(airCapture)
                            Log.d(TAG,"dispatchTakePictureIntent onActivityResult refreshViewModel $refreshResult")
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "dispatchTakePictureIntent createImageFile Exception ${ex.stackTrace}")
                }
           } else {
               data?.let {
                   // extra will contain thumbnail image if image capture not in play
                   data.extras?.let {
                       val extraPhotoUri: Uri = data.extras?.get(MediaStore.EXTRA_OUTPUT) as Uri
                       Log.d(TAG, "dispatchTakePictureIntent onActivityResult thumb URI $extraPhotoUri")

                       thumbBitmap = data.extras?.get("data") as Bitmap
                   } ?: run {
                       Log.e(TAG, "dispatchTakePictureIntent onActivityResult data.extras NULL.")
                   }
               } ?: run {
                   Log.e(TAG, "dispatchTakePictureIntent onActivityResult data NULL.")
               }
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

    fun refreshViewModel(airCapture: AirCapture): Boolean {
        // update viewModel
        textViewPreview.text = airCapture.timestamp
        textViewDecibel.text = airCapture.decibel.toString() + " dB"
        textViewAngle.text = airCapture.cameraAngle.toString() + " degrees"
        Log.d(TAG,"refreshViewModel $textViewPreview.text $textViewDecibel.text $textViewAngle.text")
        return true
    }

    // TODO: onclick listener
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
    // TODO: refactor to AirCapture model
    fun createAirCapture(): AirCapture {
        val airCapture = AirCapture(
            homeViewModel.DEFAULT_STRING,
            homeViewModel.DEFAULT_STRING,
            homeViewModel.DEFAULT_STRING,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_DOUBLE,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_FLOAT_ARRAY,
            homeViewModel.DEFAULT_DOUBLE,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_INT,
            homeViewModel.DEFAULT_FLOAT,
            homeViewModel.DEFAULT_FLOAT,
            homeViewModel.DEFAULT_FLOAT
        )
        return airCapture
    }

    private fun createBlankBitmap(width: Int, height: Int): Bitmap {
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        val bitmap1 = Bitmap.createBitmap(width, height, conf) // creates a MUTABLE bitmap
        return bitmap1
    }
    // TODO: untangle listener from viewmodel updates
    private fun updateGridViewAdapter(
        gridView: GridView,
        gridLabelArray: ArrayList<String>,
        gridBitmapArray: ArrayList<Bitmap>) {

        homeViewModel.gridPosition = 0

        val gridViewAdapter = GridViewAdapter(this.requireContext(), gridLabelArray, gridBitmapArray)
        gridView.adapter = gridViewAdapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Toast.makeText(this.context, "Touch at " + gridLabelArray[+position], Toast.LENGTH_SHORT).show()
            // TODO: gridview position selection fun
            homeViewModel.gridPosition = position
            imageViewPreview.setImageBitmap(gridBitmapArray[homeViewModel.gridPosition])
            // test full bitmap
            //imageViewPreview.setImageBitmap(fullBitmapArray[gridPosition])
            // TODO: always the latest dB - WRONG!
            // sync AirCapture to thumb selection
            val airCapture = homeViewModel.airCaptureArray[homeViewModel.gridPosition]
            Log.d(TAG,"updateGridViewAdapter soundMeter.deriveDecibel db->${airCapture.decibel.toString()}")
            refreshViewModel(airCapture)
        }
    }

    private fun startMeters(angleMeter: AngleMeter, soundMeter: SoundMeter): Boolean {
//        private fun startExerciseMeters(angleMeter: AngleMeter, soundMeter: SoundMeter, airCapture: AirCapture): Boolean {
        try {
            // start angle meter
            angleMeter.start()
            Log.d(TAG, "dispatchTakePictureIntent angleMeter started.")
            // start sound meter
            this.context?.let { soundMeter.start(it) }
            Log.d(TAG, "dispatchTakePictureIntent soundMeter started.")

            // exercise meters
//            airCapture.cameraAngle = angleMeter.getAngle()
//            airCapture.decibel = soundMeter.deriveDecibel(forceFormat = true)
            val cameraAngle = angleMeter.getAngle()
            Log.d(TAG, "startMeters angleMeter.getAngle ->$cameraAngle")
//            airCapture.decibel = soundMeter.deriveDecibel(forceFormat = true)
//            Log.d(TAG, "dispatchTakePictureIntent soundMeter.deriveDecibel db->${airCapture.decibel.toString()}")
            val decibel = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG, "startMeters soundMeter.deriveDecibel db->$decibel")

        } catch (ex: Exception) {
            Log.e(TAG, "dispatchTakePictureIntent Meter Exception ${ex.stackTrace}")
            return false
        }
        return true;
    }

    private fun createImageFile(timestamp: String): File? {
//        private fun createImageFile(): File? {
        var imageFile: File? = null
        try {
            var context = getContext()
//            // capture image attributes
//            airCapture.timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//            airCapture.imagePath = Environment.DIRECTORY_PICTURES
//            airCapture.imageName = "AIR-" + airCapture.timestamp + ".jpg"
//            storageDir = context?.getExternalFilesDir(airCapture.imagePath)!!
//            Log.d(TAG, "createImageFile storageDir->${storageDir.toString()}")
//            // create file
//            imageFile = File(storageDir, airCapture.imageName)
            // capture image attributes
            //val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imagePath = Environment.DIRECTORY_PICTURES
            val imageName = "AIR-" + timestamp + ".jpg"
            storageDir = context?.getExternalFilesDir(imagePath)!!
            Log.d(TAG, "createImageFile storageDir->${storageDir.toString()}")
            // create file
            imageFile = File(storageDir, imageName)

            currentPhotoPath = imageFile.absolutePath
        } catch (ex: Exception) {
            Log.e(TAG, "dispatchTakePictureIntent createImageFile Exception ${ex.stackTrace}")
        }
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

    private fun extractThumbnail(imageBitmap: Bitmap, scaleFactor: Int): Bitmap {
        val width = (imageBitmap.width)?.div(scaleFactor)
        val height = (imageBitmap.height)?.div(scaleFactor)
        val thumbBitmap = ThumbnailUtils.extractThumbnail(
            BitmapFactory.decodeFile(currentPhotoPath),
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
        // Bitmap resized = ThumbnailUtils.extractThumbnail(sourceBitmap, width, height);
        //imageBitmap = ThumbnailUtils.extractThumbnail()
        // Bitmap resized = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getPath()), width, height);

        //imageBitmap = cropAndScale(imageBitmap, 768)
    }

    private fun rotateBitmap(sourceBitmap: Bitmap, rotationDegrees: Float): Bitmap {
        Log.i(TAG,"rotateBitmap $rotationDegrees")
        var rotatedBitmap: Bitmap? = null
        val mat = Matrix()
        mat.postRotate(rotationDegrees)
        rotatedBitmap = Bitmap.createBitmap(
            sourceBitmap,
            0,
            0,
            sourceBitmap.getWidth(),
            sourceBitmap.getHeight(),
            mat,
            true
        )
        return rotatedBitmap
    }

    private fun extractExif(photoFile: File, airCapture: AirCapture): Boolean {
        // TODO: extractEXIF(photoFile): Boolean
        try {
            // extract EXIF attributes from photoFile
            var rotation = 0

            val exif = ExifInterface(photoFile.getAbsolutePath())

            // orientation
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270
                ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180
                ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90
            }

            Log.i(TAG,"dispatchTakePictureIntent onActivityResult EXIF orientation: $orientation")
            Log.i(TAG, "dispatchTakePictureIntent onActivityResult EXIF Rotate value: $rotation")

            // lat-lon
            var latlong: FloatArray = floatArrayOf(0.0F, 0.0F)
            exif.getLatLong(latlong)
            Log.i(TAG, "dispatchTakePictureIntent EXIF ->${latlong[0]} , ${latlong[1]}")
            // altitude
            val altitude = exif.getAltitude(0.0)
            Log.i(TAG, "dispatchTakePictureIntent EXIF altitude $altitude")
            // width
            val width: Int = exif.getAttributeInt(
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.ORIENTATION_NORMAL
            )
            Log.i(TAG,"dispatchTakePictureIntent onActivityResult EXIF width: $width")
            // length
            val length: Int = exif.getAttributeInt(
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.ORIENTATION_NORMAL
            )
            Log.i(TAG,"dispatchTakePictureIntent onActivityResult EXIF length: $length")

            // update airCapture data
            airCapture.exifOrientation = orientation
            airCapture.exifRotation = rotation
            airCapture.exifLatLon = latlong
            airCapture.exifAltitude = altitude
            airCapture.exifLength = length
            airCapture.exifWidth = width

        } catch (ex: Exception) {
            Log.e(TAG, "dispatchTakePictureIntent Meter Exception ${ex.stackTrace}")
            return false
        }
        return true
    }

    private fun captureMeters(airCapture: AirCapture): Boolean {
        // TODO: captureMeters(airCapture): Boolean
        try {
            // capture meters
            airCapture.cameraAngle = angleMeter.getAngle()
            Log.d(TAG,"captureMeters angleMeter.getAngle ->${airCapture.cameraAngle.toString()}")
            airCapture.decibel = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG,"captureMeters soundMeter.deriveDecibel db->${airCapture.decibel.toString()}")
        } catch (ex: Exception) {
            Log.e(TAG, "captureMeters captureMeters Exception ${ex.stackTrace}")
            return false
        }
        return true
    }

    private fun recordCapture(airCapture: AirCapture): Boolean {
        // TODO: refactor to AirCapture model
        // TODO: recordCapture(airCapture) -> write yourself!
        try {
            // transform AirCapture data class to json
            val jsonCapture = Gson().toJson(airCapture)
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult $jsonCapture")
            // format AirCapture name & write json file
            Log.d(TAG, "dispatchTakePictureIntent onActivityResult fileDir->$context.filesDir()")
            File(context?.filesDir, "aircapture.json").printWriter().use { out ->
                out.println("$jsonCapture")
            }
            var name = "AIR-" + airCapture.timestamp + "." + homeViewModel.DEFAULT_DATAFILE_EXT
            File(storageDir, name).printWriter().use { out ->
                out.println("$jsonCapture")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "dispatchTakePictureIntent onActivityResult recordCapture Exception ${ex.stackTrace}")
            return false
        }
        return true
    }

}