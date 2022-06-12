package com.ahandyapp.airnavx.ui.capture

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.ahandyapp.airnavx.model.AirCapture
import com.ahandyapp.airnavx.ui.sense.AngleMeter
import com.ahandyapp.airnavx.ui.sense.SoundMeter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.view.*
import android.widget.*
import androidx.fragment.app.activityViewModels
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.databinding.FragmentCaptureBinding
import com.ahandyapp.airnavx.model.AirCaptureJson
import com.ahandyapp.airnavx.model.AirConstant
import com.ahandyapp.airnavx.model.AirImageUtil
import com.ahandyapp.airnavx.ui.grid.GridViewAdapter
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.roundToInt


class CaptureFragment : Fragment() {

    private val TAG = "CaptureFragment"
    // test thumb only image capture
    private val TEST_THUMBNAIL_ONLY = false

    // view model
    private lateinit var captureViewModel: CaptureViewModel
    private var _binding: FragmentCaptureBinding? = null
    // view elements
    private lateinit var textViewPreview: TextView
    private lateinit var textViewDecibel: TextView
    private lateinit var textViewAngle: TextView
//    private lateinit var imageViewPreview: ImageView       // preview image display
//    private lateinit var gridView: GridView

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    //////////////////
    // angle & sound meters
    private var angleMeter = AngleMeter()
    private var soundMeter = SoundMeter()

    // image capture ->
    //      dispatchTakePictureIntent - create image capture file & timestamp
    //      onActivityResult - pipeline image capture data stream
    private val REQUEST_IMAGE_CAPTURE = 1001
    private lateinit var captureFile: File                  // capture file
    private var captureTimestamp: String = AirConstant.DEFAULT_STRING   // capture file creation timestamp
    private var airCaptureJson: AirCaptureJson = AirCaptureJson()

    private var airImageUtil = AirImageUtil()

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
        captureViewModel.text.observe(viewLifecycleOwner) {
            textViewPreview.text = it
        }
        textViewDecibel = binding.textDecibel
        captureViewModel.decibel.observe(viewLifecycleOwner) {
            textViewDecibel.text = it
        }
        textViewAngle = binding.textAngle
        captureViewModel.angle.observe(viewLifecycleOwner) {
            textViewAngle.text = it
        }

        // set camera button on-click listener
        val buttonCamera = root.findViewById(R.id.button_camera) as Button
        buttonCamera.setOnClickListener {
            Toast.makeText(this.context, "launching camera...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "buttonCamera.setOnClickListener launching camera...")
            dispatchTakePictureIntent()
        }

        captureViewModel.imageViewPreview = root.findViewById(R.id.imageview_preview) as ImageView

        // establish grid view
        captureViewModel.gridView = root.findViewById(R.id.gridView)

        // if grid empty
        if (captureViewModel.gridCount == 0) {
            Log.d(TAG, "onCreateView EMPTY captureViewModel $captureViewModel")

            // if unable to restore captureViewModel from previous session
            if (!fetchViewModel()) {
                // create empty view model
                createEmptyViewModel()
            }
            else {
                Log.d(TAG, "onCreateView refreshViewModel resetting EMPTY view model elements...")
                refreshViewModel(captureViewModel.airCaptureArray[captureViewModel.gridPosition])
            }
        }
        else {
            // re-establish view elements
            Log.d(TAG, "onCreateView DEFINED captureViewModel $captureViewModel")
            restoreViewModel()
            // re-establish view elements
            Log.d(TAG, "onCreateView refreshViewModel resetting DEFINED view model elements...")
            refreshViewModel(captureViewModel.airCaptureArray[captureViewModel.gridPosition])
        }
        // establish image gesture detector
        establishGestureDetector(captureViewModel.imageViewPreview)
        // TODO: update gridview
        updateGridViewAdapter(captureViewModel.gridView, captureViewModel.gridLabelArray, captureViewModel.gridBitmapArray)


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
        Log.d(TAG, "dispatchTakePictureIntent startExerciseMeters $startMeters")

        if (!TEST_THUMBNAIL_ONLY) {
            // for full image capture, supply created image file URI
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                // create the photo File
                captureTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val imageName = airCaptureJson.getAirFilename(CaptureViewModel.AirFileType.IMAGE, captureTimestamp)
                val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val imageFile = createImageFile(storageDir, imageName)

                imageFile?.let {
                    captureFile = imageFile
                    val photoUri = FileProvider.getUriForFile(
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
        Log.d(TAG,"dispatchTakePictureIntent onActivityResult requestCode ${requestCode}, resultCode $resultCode")
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
                    val airCaptureBitmap = MediaStore.Images.Media.getBitmap(
                        activity?.applicationContext?.contentResolver,
                        uri
                    )
                    if (airCaptureBitmap != null) {
                        // create initialized aircapture object
                        //val airCapture = createAirCapture()
                        val airCapture: AirCapture = AirCapture()
                        //airCapture = AirCapture()
                        // capture image attributes
                        airCapture.timestamp = captureTimestamp
                        airCapture.imagePath = Environment.DIRECTORY_PICTURES
                        airCapture.imageName = airCaptureJson.getAirFilename(CaptureViewModel.AirFileType.IMAGE, captureTimestamp)

                        // update image length & width prior to thumbnail extraction
                        airCapture.imageWidth = airCaptureBitmap.width
                        airCapture.imageHeight = airCaptureBitmap.height
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult width ${airCapture.imageWidth} X height ${airCapture.imageHeight}")

                        /////////////////////////////////////
                        // extractEXIF(photoFile): Boolean
                        val exifExtracted = extractExif(captureFile, airCapture)
                        Log.d(TAG, "dispatchTakePictureIntent onActivityResult exifExtracted $exifExtracted")

                        // captureMeters(airCapture): Boolean
                        val metersCaptured = captureMeters(airCapture)
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult metersCaptured $metersCaptured")

                        // extract thumbnail at scale factor
                        val thumbBitmap = extractThumbnail(currentPhotoPath, airCaptureBitmap, captureViewModel.THUMB_SCALE_FACTOR)
                        // add set to view model
                        addViewModelSet(captureViewModel, captureViewModel.gridView, captureViewModel.imageViewPreview,
                            airCaptureBitmap, thumbBitmap, null, null, airCapture)

                        // write AirCapture
                        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                        val captureRecorded = airCaptureJson.write(storageDir, captureTimestamp, airCapture)
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult captureRecorded $captureRecorded")

                        // refresh viewmodel
                        val refreshResult = refreshViewModel(airCapture)
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult refreshViewModel $refreshResult")
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
    private fun establishGestureDetector(imageView: ImageView) {
        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent?): Boolean {
                Log.i("TAG", "establishGestureDetector onDown: ")
                // don't return false here or else none of the other gestures will work
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                Log.i("TAG", "establishGestureDetector onSingleTapConfirmed...")
                return true
            }

            // LONGPRESS -> delete selected set
            override fun onLongPress(e: MotionEvent?) {
                val x = e?.x?.roundToInt()
                val y = e?.y?.roundToInt()
                Log.i("TAG", "establishGestureDetector onLongPress: x $x, y $y")
                airImageUtil.showDeleteAlertDialog(context!!, activity!!, null, captureViewModel)
                //showAlertDialog()
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                val x = e?.x?.roundToInt()
                val y = e?.y?.roundToInt()
                Log.i("TAG", "establishGestureDetector onDoubleTap: x $x, y $y")
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent?,
                distanceX: Float, distanceY: Float
            ): Boolean {
                Log.i("TAG", "establishGestureDetector nScroll: distanceX $distanceX distanceY $distanceY")
                return true
            }

            // FLING -> refresh from files
            override fun onFling(
                event1: MotionEvent?, event2: MotionEvent?,
                velocityX: Float, velocityY: Float
            ): Boolean {
                Log.d("TAG", "establishGestureDetector onFling: velocityX $velocityX velocityY $velocityY")
                Log.i("TAG", "establishGestureDetector resetting view model...")
                Toast.makeText(context, "Fetching AIR capture files from storage...", Toast.LENGTH_SHORT).show()

                // reset viewmodel grid
                captureViewModel.gridPosition = 0
                captureViewModel.gridCount = 0
                captureViewModel.gridLabelArray.clear()
                captureViewModel.origBitmapArray.clear()
                captureViewModel.zoomBitmapArray.clear()
                captureViewModel.overBitmapArray.clear()
                captureViewModel.airCaptureArray.clear()

                Log.i("TAG", "establishGestureDetector resetting view model...")
                // if unable to restore captureViewModel from previous session
                if (!fetchViewModel()) {
                    // create empty view model
                    createEmptyViewModel()
                }
                else {
                    // re-establish view elements
                    Log.d(TAG, "establishGestureDetector refreshViewModel resetting view model elements...")
                    refreshViewModel(captureViewModel.airCaptureArray[captureViewModel.gridPosition])
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

//    private fun showAlertDialog() {
//        // build alert dialog
//        val dialogBuilder = AlertDialog.Builder(context)
//
//        // set message of alert dialog
//        dialogBuilder.setMessage("Delete AIR capture files?")
//            // if the dialog is cancelable
//            .setCancelable(false)
//            // positive button text and action
//            .setPositiveButton("Delete", DialogInterface.OnClickListener {
//                    dialog, id ->
//                val airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
//                Toast.makeText(context, "Deleting AIR capture files for ${airCapture.timestamp}...", Toast.LENGTH_SHORT).show()
//                Log.d("TAG", "showAlertDialog deleting AIR capture files for ${airCapture.timestamp}")
//                try {
//                    // set path = storage dir + time.jpg
//                    val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
//                    val imagePath = Paths.get(storageDir.toString() + File.separator +
//                            AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp  +
//                            AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT)
//                    val dataPath = Paths.get(storageDir.toString() + File.separator +
//                            AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp  +
//                            AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_DATAFILE_EXT)
//                    val zoomPath = Paths.get(storageDir.toString() + File.separator +
//                            AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp  +
//                            AirConstant.DEFAULT_ZOOM_SUFFIX + AirConstant.DEFAULT_EXTENSION_SEPARATOR +
//                            AirConstant.DEFAULT_IMAGEFILE_EXT)
//                    Log.d("TAG", "showAlertDialog deleting files \n$imagePath \n$dataPath \n$zoomPath")
//                    // Files.deleteIfExists(path)
//                    var result = Files.deleteIfExists(imagePath)
//                    if (result) {
//                        Log.d("TAG", "showAlertDialog delete image $imagePath success...")
//                    } else {
//                        Log.d("TAG", "showAlertDialog delete image $imagePath failed...")
//                    }
//                    result = Files.deleteIfExists(dataPath)
//                    if (result) {
//                        Log.d("TAG", "showAlertDialog delete datafile $dataPath success...")
//                    } else {
//                        Log.d("TAG", "showAlertDialog delete datafile $dataPath failed...")
//                    }
//                    result = Files.deleteIfExists(zoomPath)
//                    if (result) {
//                        Log.d("TAG", "showAlertDialog delete zoomimage $zoomPath success...")
//                    } else {
//                        Log.d("TAG", "showAlertDialog delete zoomimage $zoomPath failed...")
//                    }
//
//                } catch (ioException: IOException) {
//                    ioException.printStackTrace()
//                } finally {
//                }
//
//                dialog.dismiss()
//            })
//            // negative button text and action
//            .setNegativeButton("Cancel", DialogInterface.OnClickListener {
//                    dialog, id -> dialog.cancel()
//            })
//
//        // create dialog box
//        val alert = dialogBuilder.create()
//        // set title for alert dialog box
//        alert.setTitle("AlertDialogExample")
//        // show alert dialog
//        alert.show()
//    }
    /////////////////////////////view model helpers////////////////////////////
    private fun createEmptyViewModel(): Boolean {
        // create empty bitmap to seed grid
        val blankBitmap = createBlankBitmap(
            captureViewModel.DEFAULT_BLANK_GRID_WIDTH,
            captureViewModel.DEFAULT_BLANK_GRID_HEIGHT
        )
        // initialize AirCapture to seed grid
        //val airCapture = createAirCapture()
        val airCapture: AirCapture = AirCapture()
        //airCapture = AirCapture()

        // initialize view
        captureViewModel.gridBitmapArray.add(blankBitmap)
        ++captureViewModel.gridCount
        captureViewModel.gridLabelArray.add("thumb${captureViewModel.gridCount}")
        captureViewModel.origBitmapArray.add(blankBitmap)
        captureViewModel.zoomBitmapArray.add(blankBitmap)
        captureViewModel.overBitmapArray.add(blankBitmap)
        captureViewModel.airCaptureArray.add(airCapture)
        // initialize gridViewAdapter
        captureViewModel.gridPosition = 0
        updateGridViewAdapter(
            captureViewModel.gridView,
            captureViewModel.gridLabelArray,
            captureViewModel.gridBitmapArray
        )

        return true
    }
    // view model defined, update UI
    private fun restoreViewModel(): Boolean {
        // grid view
        updateGridViewAdapter(
            captureViewModel.gridView,
            captureViewModel.gridLabelArray,
            captureViewModel.gridBitmapArray
        )
        // show selected grid thumb in preview
        captureViewModel.imageViewPreview.setImageBitmap(captureViewModel.gridBitmapArray[captureViewModel.gridPosition])
        // restore aircapture data
        val airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
        //airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
        // refresh viewmodel
        Log.d(TAG, "restoreViewModel refreshComplete $airCapture")
        val refreshComplete = refreshViewModel(airCapture)
        Log.d(TAG, "restoreViewModel refreshComplete $refreshComplete")
        Log.d(TAG,"restoreViewModel refreshComplete ${textViewPreview.text} ${textViewDecibel.text} ${textViewAngle.text}")

        return true
    }
    // fetch previous session from storage
    private fun fetchViewModel(): Boolean {
        // get jpg file list by descending time
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        Log.d(TAG, "fetchViewModel storageDir $storageDir")
        val files = storageDir.listFiles()
        for (file in files) {
            val name = file.name
            Log.d(TAG, "fetchViewModel listFiles file name $name")
        }
        // for each name with JPG extension
        File(storageDir.toString()).walk().filter { file-> hasRequiredSuffix(file) }.forEach { it ->
            //println(it)
            //val name = it.name    // full name w/ ext jpg
            val name = it.nameWithoutExtension
            val ext = it.extension
            Log.d(TAG, "fetchViewModel walk file name $name ext $ext")
            val airCaptureName = name  + AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_DATAFILE_EXT
            val airCapturePath = storageDir.toString() + File.separator + airCaptureName
            Log.d(TAG, "fetchViewModel airCaptureName $airCaptureName check...")
            val airCaptureFile = File(airCapturePath)
            //   if name.json exists
            if (airCaptureFile.exists()) {
                Log.d(TAG, "fetchViewModel airCaptureName $airCaptureName exists...")

                val airImageName = name  + AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT
                val airImagePath = storageDir.toString() + File.separator + airImageName
                Log.d(TAG, "fetchViewModel airImageName $airImageName...")
                val airImageFile = File(airImagePath)
                // read json into airCapture
                val airCapture = airCaptureJson.read(airCaptureFile)

                //   read air image into bitmap
                val uri = Uri.fromFile(airImageFile)
                val airCaptureBitmap = MediaStore.Images.Media.getBitmap(
                    activity?.applicationContext?.contentResolver,
                    uri
                )
                // add view model set
                if (airCaptureBitmap != null) {
                    // attempt to open associated zoom image file
                    var zoomBitmap = fetchBitmap(name, AirConstant.DEFAULT_ZOOM_SUFFIX)
                    if (zoomBitmap != null) {
                        Log.d(TAG,"fetchViewModel zoomBitmap w x h = ${zoomBitmap.width} x ${zoomBitmap.height}")
                    }
                    // attempt to open associated over image file
                    var overBitmap = fetchBitmap(name, AirConstant.DEFAULT_OVER_SUFFIX)
                    if (overBitmap != null) {
                        Log.d(TAG,"fetchViewModel overBitmap w x h = ${overBitmap.width} x ${overBitmap.height}")
                    }

                    // extract thumbnail at scale factor
                    val thumbBitmap = extractThumbnail(airImagePath, airCaptureBitmap, captureViewModel.THUMB_SCALE_FACTOR)
                    // add set to view model
                    addViewModelSet(captureViewModel, captureViewModel.gridView, captureViewModel.imageViewPreview,
                        airCaptureBitmap, thumbBitmap, zoomBitmap, overBitmap, airCapture)
                }
            }
        }
        return true
    }

    private fun fetchBitmap(name: String, suffix: String): Bitmap? {
        var zoomBitmap: Bitmap? = null
        try {
            val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val airImageName =
                name + suffix + AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT
            Log.d(TAG, "fetchZoomBitmap airImageName $airImageName...")
            val airImagePath = storageDir.toString() + File.separator + airImageName
            Log.d(TAG, "fetchZoomBitmap airImagePath $airImagePath...")
            val airImageFile = File(airImagePath)

            //   read air image into bitmap
            val uri = Uri.fromFile(airImageFile)
            zoomBitmap = MediaStore.Images.Media.getBitmap(
                activity?.applicationContext?.contentResolver,
                uri
            )
            if (zoomBitmap != null) {
                Log.d(
                    TAG,
                    "fetchZoomBitmap zoomBitmap w x h = ${zoomBitmap.width} x ${zoomBitmap.height}"
                )
            }
        }
        catch (ex: FileNotFoundException) {
            Log.d(TAG, "fetchZoomBitmap FileNotFoundException...")
        }
        catch (ex: Exception) {
            Log.e(TAG, "fetchZoomBitmap exception ${ex.message}")
        }
        return zoomBitmap
    }

    private fun hasRequiredSuffix(file: File): Boolean {
        val requiredSuffixes = listOf(AirConstant.DEFAULT_IMAGEFILE_EXT)
        return requiredSuffixes.contains(file.extension)
    }

    private fun addViewModelSet(captureViewModel: CaptureViewModel,
                                gridView: GridView,
                                imageViewPreview: ImageView,
                                airImageBitmap: Bitmap,
                                thumbBitmap: Bitmap,
                                zoomBitmap: Bitmap?,
                                overBitmap: Bitmap?,
                                airCapture: AirCapture): Boolean {
        // rotate thumb bitmap
        val rotatedThumbBitmap = rotateBitmap(thumbBitmap, airCapture.exifRotation.toFloat())
        imageViewPreview.setImageBitmap(rotatedThumbBitmap)
        // insert thumb in grid view
        ++captureViewModel.gridCount
        captureViewModel.gridLabelArray.add("thumb${captureViewModel.gridCount}")
        captureViewModel.gridBitmapArray.add(0, rotatedThumbBitmap)
        captureViewModel.gridPosition = 0
        // TODO: invoke in onCreate
        updateGridViewAdapter(gridView, captureViewModel.gridLabelArray, captureViewModel.gridBitmapArray)
        // rotate full bitmap
        val fullBitmap = rotateBitmap(airImageBitmap, airCapture.exifRotation.toFloat())
        // add full bitmap into array
        captureViewModel.origBitmapArray.add(0, fullBitmap)
        // if zoom & overlay are copies of the full bitmap, assign rotated full bitmap
        if (zoomBitmap == null) {
            captureViewModel.zoomBitmapArray.add(0, fullBitmap)
        }
        else {
            captureViewModel.zoomBitmapArray.add(0, zoomBitmap)
        }

        if (overBitmap == null) {
            captureViewModel.overBitmapArray.add(0, fullBitmap)
        }
        else {
            captureViewModel.overBitmapArray.add(0, overBitmap)
        }
        // add aircapture
        captureViewModel.airCaptureArray.add(0, airCapture)
        return true
    }

    private fun updateGridViewAdapter(
        gridView: GridView,
        gridLabelArray: ArrayList<String>,
        gridBitmapArray: ArrayList<Bitmap>) {

//        captureViewModel.gridPosition = 0

        val gridViewAdapter = GridViewAdapter(this.requireContext(), gridLabelArray, gridBitmapArray)
        gridView.adapter = gridViewAdapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Toast.makeText(this.context, "Touch at " + gridLabelArray[+position], Toast.LENGTH_SHORT).show()
            captureViewModel.gridPosition = position
            captureViewModel.imageViewPreview.setImageBitmap(gridBitmapArray[captureViewModel.gridPosition])
            // test full bitmap
            //imageViewPreview.setImageBitmap(fullBitmapArray[gridPosition])
            // sync AirCapture to thumb selection
            val airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
            //airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
            Log.d(TAG,"updateGridViewAdapter soundMeter.deriveDecibel db->${airCapture.decibel}")
            refreshViewModel(airCapture)
        }
    }

    private fun refreshViewModel(airCapture: AirCapture): Boolean {
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
            Log.d(TAG, "createImageFile storageDir->$storageDir, name->$imageName")
        } catch (ex: Exception) {
            Log.e(TAG, "createImageFile Exception ${ex.stackTrace}")
        }
        return imageFile
    }

    private fun extractThumbnail(currentPhotoPath: String, imageBitmap: Bitmap, scaleFactor: Int): Bitmap {
        val width = (imageBitmap.width).div(scaleFactor)
        val height = (imageBitmap.height).div(scaleFactor)
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
        val rotatedBitmap: Bitmap?
        val mat = Matrix()
        mat.postRotate(rotationDegrees)
        rotatedBitmap = Bitmap.createBitmap(
            sourceBitmap,
            0,
            0,
            sourceBitmap.width,
            sourceBitmap.height,
            mat,
            true
        )
        return rotatedBitmap
    }

    private fun extractExif(photoFile: File, airCapture: AirCapture): Boolean {
        try {
            // extract EXIF attributes from photoFile
            var rotation = 0

            val exif = ExifInterface(photoFile.absolutePath)

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
            val latlong: FloatArray = floatArrayOf(0.0F, 0.0F)
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
        return true
    }

    private fun captureMeters(airCapture: AirCapture): Boolean {
        // capture Meters in airCapture
        try {
            // capture meters
            airCapture.cameraAngle = angleMeter.getAngle()
            Log.d(TAG,"captureMeters angleMeter.getAngle ->${airCapture.cameraAngle}")
            airCapture.decibel = soundMeter.deriveDecibel(forceFormat = true)
            Log.d(TAG,"captureMeters soundMeter.deriveDecibel db->${airCapture.decibel}")
        } catch (ex: Exception) {
            Log.e(TAG, "captureMeters captureMeters Exception ${ex.stackTrace}")
            return false
        }
        return true
    }
    /////////////////////////////meter handlers////////////////////////////////

}