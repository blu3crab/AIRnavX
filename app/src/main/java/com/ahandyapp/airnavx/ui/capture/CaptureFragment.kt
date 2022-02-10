package com.ahandyapp.airnavx.ui.capture

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
import androidx.fragment.app.activityViewModels
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.databinding.FragmentCaptureBinding
import com.ahandyapp.airnavx.ui.grid.GridViewAdapter
import java.io.IOException


class CaptureFragment : Fragment() {

    private val TAG = "CaptureFragment"
    // test thumb only image capture
    private val TEST_THUMBNAIL_ONLY = false

    // view model
//    private lateinit var captureViewModel: CaptureViewModel
    private lateinit var captureViewModel: CaptureViewModel
    private var _binding: FragmentCaptureBinding? = null
    // view elements
    private lateinit var textViewPreview: TextView
    private lateinit var textViewDecibel: TextView
    private lateinit var textViewAngle: TextView
    private lateinit var imageViewPreview: ImageView       // preview image display
    private lateinit var gridView: GridView

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    //////////////////
    // angle & sound meters
    private var angleMeter = AngleMeter()
    private var soundMeter = SoundMeter()

    // image capture ->
    //      dispatchTakePictureIntent - create image capture file & timestamp
    //      onActivityResult - pipeline image capture data stream
    val REQUEST_IMAGE_CAPTURE = 1001
    private lateinit var captureFile: File          // capture file
    private var captureTimestamp: String = "nada"   // capture file creation timestamp

    /////////////////////////////life-cycle////////////////////////////////////
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // get activity level view model
        val viewModel: CaptureViewModel by activityViewModels()
        captureViewModel = viewModel
        // binding
        _binding = FragmentCaptureBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // live data
        textViewPreview = binding.textPreview
        captureViewModel.text.observe(viewLifecycleOwner, Observer {
            textViewPreview.text = it
        })
        textViewDecibel = binding.textDecibel
        captureViewModel.decibel.observe(viewLifecycleOwner, Observer {
            textViewDecibel.text = it
        })
        textViewAngle = binding.textAngle
        captureViewModel.angle.observe(viewLifecycleOwner, Observer {
            textViewAngle.text = it
        })

        val packageName = this.context?.getPackageName()
        // get reference to button
        val buttonCameraIdString = "button_camera"
        val buttonCameraId = resources.getIdentifier(buttonCameraIdString, "id", packageName)
        val buttonCamera = root.findViewById(buttonCameraId) as Button
        // set camera button on-click listener
        buttonCamera.setOnClickListener {
            Toast.makeText(this.context, "launching camera...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonCamera.setOnClickListener launching camera...")
            dispatchTakePictureIntent()
        }
        val previewViewIdString = "imageViewPreview"
        val previewViewId = resources.getIdentifier(previewViewIdString, "id", packageName)
        imageViewPreview = root.findViewById(previewViewId) as ImageView

        // establish grid view
        gridView = root.findViewById(R.id.gridView)

        // if grid empty
        if (captureViewModel.gridCount == 0) {
            Log.d(TAG, "onCreateView EMPTY captureViewModel $captureViewModel")

            // if unable to restore captureViewModel from previous session
            if (!fetchViewModel()) {
                // create empty view model
                createEmptyViewModel()
            }
        }
        else {
            // re-establish view elements
            Log.d(TAG, "onCreateView DEFINED captureViewModel $captureViewModel")
            restoreViewModel()
        }
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
        val startMeters = startMeters(angleMeter, soundMeter)
        Log.d(TAG, "dispatchTakePictureIntent startExerciseMeters ${startMeters}")

        if (!TEST_THUMBNAIL_ONLY) {
            // for full image capture, supply created image file URI
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                // create the photo File
                captureTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val imageName = getAirFilename(CaptureViewModel.AirFileType.IMAGE, captureTimestamp)
                val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val imageFile = createImageFile(storageDir, imageName)

                imageFile?.let {
                    captureFile = imageFile
                    var photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.ahandyapp.airnavx",
                        captureFile
                    )

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            } catch (ex: Exception) {
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "dispatchTakePictureIntent -> NO camera launch Exception ${ex.message}...")
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
            if (!TEST_THUMBNAIL_ONLY) {
                // generate thumbnail from file uri if not available as thumbnail in data
                // photo file is file EXTRA_OUTPUT location of actual camera image
                Log.d(TAG, "dispatchTakePictureIntent onActivityResult generate thumbnail...")
                val uri = Uri.fromFile(captureFile)
                val currentPhotoPath = captureFile.absolutePath
                try {
                    var airCaptureBitmap = MediaStore.Images.Media.getBitmap(
                        activity?.applicationContext?.contentResolver,
                        uri
                    )
                    if (airCaptureBitmap != null) {
                        // create initialized aircapture object
                        var airCapture = createAirCapture()
                        // capture image attributes
                        airCapture.timestamp = captureTimestamp
                        airCapture.imagePath = Environment.DIRECTORY_PICTURES
                        airCapture.imageName = getAirFilename(CaptureViewModel.AirFileType.IMAGE, captureTimestamp)

                        // update image length & width prior to thumbnail extraction
                        airCapture.imageWidth = airCaptureBitmap!!.width
                        airCapture.imageHeight = airCaptureBitmap!!.height
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult width ${airCapture.imageWidth} X height ${airCapture.imageHeight}")

                        /////////////////////////////////////
                        // extractEXIF(photoFile): Boolean
                        val exifExtracted = extractExif(captureFile, airCapture)
                        Log.d(TAG, "dispatchTakePictureIntent onActivityResult exifExtracted ${exifExtracted}")

                        // extract thumbnail at scale factor
                        var thumbBitmap = extractThumbnail(currentPhotoPath, airCaptureBitmap!!, captureViewModel.THUMB_SCALE_FACTOR)

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
                            ++captureViewModel.gridCount
                            captureViewModel.gridLabelArray.add("thumb${captureViewModel.gridCount.toString()}")
                            captureViewModel.gridBitmapArray.add(0, thumbBitmap!!)
                            updateGridViewAdapter(gridView, captureViewModel.gridLabelArray, captureViewModel.gridBitmapArray)

                            // rotate full bitmap
                            var fullBitmap = rotateBitmap(airCaptureBitmap!!, airCapture.exifRotation.toFloat())
                            // insert full bitmap into array
                            captureViewModel.fullBitmapArray.add(0, fullBitmap!!)
                            captureViewModel.airCaptureArray.add(0, airCapture)
                            // TODO: gridview update fun ^

                            // recordCapture(airCapture): Boolean
                            val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                            val captureRecorded = recordAirCapture(storageDir, airCapture)
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

                       var thumbBitmap = data.extras?.get("data") as Bitmap
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
    /////////////////////////////life-cycle////////////////////////////////////

    /////////////////////////////view model helpers////////////////////////////
    private fun createEmptyViewModel(): Boolean {
        // create empty bitmap to seed grid
        var blankBitmap = createBlankBitmap(
            captureViewModel.DEFAULT_BLANK_GRID_WIDTH,
            captureViewModel.DEFAULT_BLANK_GRID_HEIGHT
        )
        // initialize AirCapture to seed grid
        val airCapture = createAirCapture();
        // initialize view
        if (blankBitmap != null) {
            captureViewModel.gridBitmapArray.add(blankBitmap)
            ++captureViewModel.gridCount
            captureViewModel.gridLabelArray.add("thumb${captureViewModel.gridCount.toString()}")
            captureViewModel.fullBitmapArray.add(blankBitmap)
            captureViewModel.airCaptureArray.add(airCapture)
        }
        // initialize gridViewAdapter
        updateGridViewAdapter(
            gridView,
            captureViewModel.gridLabelArray,
            captureViewModel.gridBitmapArray
        )

        return true
    }

    private fun restoreViewModel(): Boolean {
        // grid view
        updateGridViewAdapter(
            gridView,
            captureViewModel.gridLabelArray,
            captureViewModel.gridBitmapArray
        )
        // show selected grid thumb in preview
        imageViewPreview.setImageBitmap(captureViewModel.gridBitmapArray[captureViewModel.gridPosition])
        // restore aircapture data
        var airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
        // refresh viewmodel
        Log.d(TAG, "restoreViewModel refreshComplete $airCapture")
        val refreshComplete = refreshViewModel(airCapture)
        Log.d(TAG, "restoreViewModel refreshComplete $refreshComplete")
        Log.d(TAG,"restoreViewModel refreshComplete ${textViewPreview.text} ${textViewDecibel.text} ${textViewAngle.text}")

        return true
    }

    private fun fetchViewModel(): Boolean {
        // get jpg file list by descending time
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        Log.d(TAG, "fetchViewModel storageDir ${storageDir.toString()}")
        val files = storageDir.listFiles()
        for (file in files) {
            val name = file.name
            Log.d(TAG, "fetchViewModel listFiles file name $name")
        }
        // for each name
        File(storageDir.toString()).walk().filter { file-> hasRequiredSuffix(file) }.forEach {
            //println(it)
            //val name = it.name    // full name w/ ext jpg
            val name = it.nameWithoutExtension
            val ext = it.extension
            Log.d(TAG, "fetchViewModel walk file name $name ext $ext")
            val airCaptureName = name + "." + captureViewModel.DEFAULT_DATAFILE_EXT
            val airCapturePath = storageDir.toString() + "/" + airCaptureName
            Log.d(TAG, "fetchViewModel airCaptureName $airCaptureName check...")
            val airCaptureFile = File(airCapturePath)
            //   if name.json exists
            if (airCaptureFile.exists()) {
                Log.d(TAG, "fetchViewModel airCaptureName $airCaptureName exists...")
                // TODO: add to file pair collection in time order (alpha)

                val airImageName = name + "." + captureViewModel.DEFAULT_IMAGEFILE_EXT
                val airImagePath = storageDir.toString() + "/" + airImageName
                Log.d(TAG, "fetchViewModel airImageName $airImageName...")
                val airImageFile = File(airImagePath)

                //   extract aircapture (read json)
                var jsonString: String = "nada"
                try {
                    jsonString = airCaptureFile.bufferedReader().use { it.readText() }
                } catch (ioException: IOException) {
                    ioException.printStackTrace()
                } finally {
                    airCaptureFile.bufferedReader().close()
                }
                Log.d(TAG, "fetchViewModel airCapture json $jsonString...")
                var airCapture = Gson().fromJson(jsonString, AirCapture::class.java)
                Log.d(TAG, "fetchViewModel AirCapture $airCapture")

                //   extract thumb
                val uri = Uri.fromFile(airImageFile)
                var airImageBitmap = MediaStore.Images.Media.getBitmap(
                    activity?.applicationContext?.contentResolver,
                    uri
                )
                if (airImageBitmap != null) {
                    // extract thumbnail at scale factor
                    var thumbBitmap = extractThumbnail(airImagePath, airImageBitmap!!, captureViewModel.THUMB_SCALE_FACTOR)

                    // rotateBitmap(imageBitmap, rotationDegrees): Bitmap
                    if (thumbBitmap != null) {
                        // rotate thumb bitmap
                        thumbBitmap = rotateBitmap(thumbBitmap!!, airCapture.exifRotation.toFloat())
                        imageViewPreview.setImageBitmap(thumbBitmap)
                        // TODO: gridview update fun v
                        // insert thumb in grid view
                        ++captureViewModel.gridCount
                        captureViewModel.gridLabelArray.add("thumb${captureViewModel.gridCount.toString()}")
                        captureViewModel.gridBitmapArray.add(0, thumbBitmap!!)
                        updateGridViewAdapter(gridView, captureViewModel.gridLabelArray, captureViewModel.gridBitmapArray)

                        // rotate full bitmap
                        var fullBitmap = rotateBitmap(airImageBitmap!!, airCapture.exifRotation.toFloat())
                        // insert full bitmap into array
                        captureViewModel.fullBitmapArray.add(0, fullBitmap!!)
                        captureViewModel.airCaptureArray.add(0, airCapture)
                        // TODO: gridview update fun ^

                    }

                }
            }
        }
        // for each name
        //   if name.json exists
        //   get name.json
        //   extract aircapture (read json)
        //   extract thumb
        //   if first image
        //      set preview image for first image
        //      set position
        //   update view model
        //     assign grid view & associated element
        return false
    }

    private fun hasRequiredSuffix(file: File): Boolean {
        val requiredSuffixes = listOf(captureViewModel.DEFAULT_IMAGEFILE_EXT)
        return requiredSuffixes.contains(file.extension)
    }

    private fun updateGridViewAdapter(
        gridView: GridView,
        gridLabelArray: ArrayList<String>,
        gridBitmapArray: ArrayList<Bitmap>) {

        captureViewModel.gridPosition = 0

        val gridViewAdapter = GridViewAdapter(this.requireContext(), gridLabelArray, gridBitmapArray)
        gridView.adapter = gridViewAdapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Toast.makeText(this.context, "Touch at " + gridLabelArray[+position], Toast.LENGTH_SHORT).show()
            captureViewModel.gridPosition = position
            imageViewPreview.setImageBitmap(gridBitmapArray[captureViewModel.gridPosition])
            // test full bitmap
            //imageViewPreview.setImageBitmap(fullBitmapArray[gridPosition])
            // sync AirCapture to thumb selection
            val airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
            Log.d(TAG,"updateGridViewAdapter soundMeter.deriveDecibel db->${airCapture.decibel.toString()}")
            refreshViewModel(airCapture)
            // TODO: invalidate view text
            //captureViewModel.apply {  }
        }
    }

    fun refreshViewModel(airCapture: AirCapture): Boolean {
        // update viewModel
        textViewPreview.text = airCapture.timestamp
        textViewDecibel.text = airCapture.decibel.toString() + " dB"
        textViewAngle.text = airCapture.cameraAngle.toString() + " degrees"
        Log.d(TAG,"refreshViewModel ${textViewPreview.text} ${textViewDecibel.text} ${textViewAngle.text}")
        return true
    }
    /////////////////////////////view model helpers////////////////////////////

    /////////////////////////////image manipulation////////////////////////////
    private fun createBlankBitmap(width: Int, height: Int): Bitmap {
        val conf = Bitmap.Config.ARGB_8888 // see other conf types
        val bitmap1 = Bitmap.createBitmap(width, height, conf) // creates a MUTABLE bitmap
        return bitmap1
    }

    private fun createImageFile(storageDir: File, imageName: String): File? {
        var imageFile: File? = null
        try {
            // create file
            imageFile = File(storageDir, imageName)
            Log.d(TAG, "createImageFile storageDir->${storageDir.toString()}, name->$imageName")
        } catch (ex: Exception) {
            Log.e(TAG, "createImageFile Exception ${ex.stackTrace}")
        }
        return imageFile
    }

    private fun extractThumbnail(currentPhotoPath: String, imageBitmap: Bitmap, scaleFactor: Int): Bitmap {
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

    fun getAirFilename(type: CaptureViewModel.AirFileType, captureTimestamp : String): String {
        var airFilename = captureViewModel.DEFAULT_STRING
        if (type == CaptureViewModel.AirFileType.IMAGE) {
            airFilename = "AIR-" + captureTimestamp + "." + captureViewModel.DEFAULT_IMAGEFILE_EXT
        }
        else if (type == CaptureViewModel.AirFileType.DATA) {
            airFilename = "AIR-" + captureTimestamp + "." + captureViewModel.DEFAULT_DATAFILE_EXT
        }
        return airFilename
    }
    /////////////////////////////image manipulation////////////////////////////

    /////////////////////////////AirCapture handlers///////////////////////////
    // TODO: refactor to AirCapture model
    fun createAirCapture(): AirCapture {
        val airCapture = AirCapture(
            captureViewModel.DEFAULT_STRING,
            captureViewModel.DEFAULT_STRING,
            captureViewModel.DEFAULT_STRING,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_DOUBLE,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_FLOAT_ARRAY,
            captureViewModel.DEFAULT_DOUBLE,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_INT,
            captureViewModel.DEFAULT_FLOAT,
            captureViewModel.DEFAULT_FLOAT,
            captureViewModel.DEFAULT_FLOAT
        )
        return airCapture
    }

    private fun recordAirCapture(storageDir: File, airCapture: AirCapture): Boolean {
        // TODO: refactor to AirCapture model-> write yourself!
        try {
            // transform AirCapture data class to json
            val jsonCapture = Gson().toJson(airCapture)
            Log.d(TAG, "recordAirCapture $jsonCapture")

            // format AirCapture name & write json file
            var name = getAirFilename(CaptureViewModel.AirFileType.DATA, captureTimestamp)
            Log.d(TAG, "recordAirCapture storageDir->$storageDir, name->$name")
            File(storageDir, name).printWriter().use { out ->
                out.println("$jsonCapture")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "recordAirCapture Exception ${ex.stackTrace}")
            return false
        }
        return true
    }
    /////////////////////////////AirCapture handlers///////////////////////////

    /////////////////////////////meter handlers////////////////////////////////
    private fun startMeters(angleMeter: AngleMeter, soundMeter: SoundMeter): Boolean {
        try {
            // start angle meter
            angleMeter.start(requireActivity())
            Log.d(TAG, "dispatchTakePictureIntent angleMeter started.")
            // start sound meter
            this.context?.let { soundMeter.start(it) }
            Log.d(TAG, "dispatchTakePictureIntent soundMeter started.")

            // exercise meters
            val cameraAngle = angleMeter.getAngle()
            Log.d(TAG, "startMeters angleMeter.getAngle ->$cameraAngle")
            val decibel = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG, "startMeters soundMeter.deriveDecibel db->$decibel")

        } catch (ex: Exception) {
            Log.e(TAG, "dispatchTakePictureIntent Meter Exception ${ex.stackTrace}")
            return false
        }
        return true;
    }

    private fun captureMeters(airCapture: AirCapture): Boolean {
        // capture Meters in airCapture
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
    /////////////////////////////meter handlers////////////////////////////////

}