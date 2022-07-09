package com.ahandyapp.airnavx.ui.capture

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
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
import android.view.*
import android.widget.*
import androidx.fragment.app.activityViewModels
import com.ahandyapp.airnavx.R
import com.ahandyapp.airnavx.databinding.FragmentCaptureBinding
import com.ahandyapp.airnavx.model.AirConstant
import com.ahandyapp.airnavx.model.AirImageUtil
import com.ahandyapp.airnavx.ui.grid.GridViewAdapter
import kotlin.math.roundToInt


class CaptureFragment : Fragment() {

    private val TAG = "CaptureFragment"
    // test thumb only image capture
    private val TEST_THUMBNAIL_ONLY = false

    // property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // view model
    private lateinit var captureViewModel: CaptureViewModel
    private var _binding: FragmentCaptureBinding? = null
    // view elements
    private lateinit var textViewPreview: TextView
    private lateinit var textViewDecibel: TextView
    private lateinit var textViewAngle: TextView

    //////////////////
    // angle & sound meters
    private var angleMeter = AngleMeter()
    private var soundMeter = SoundMeter()

    // image capture ->
    //      dispatchTakePictureIntent - create image capture file & timestamp
    //      onActivityResult - pipeline image capture data stream
    private lateinit var captureFile: File                  // capture file
    private var captureTimestamp: String = AirConstant.DEFAULT_STRING   // capture file creation timestamp

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
            if (!airImageUtil.fetchViewModel(context!!, activity!!, captureViewModel)) {
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
        // update gridview
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
                val airCapture = AirCapture()
                captureTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val imageName = airCapture.getAirFilename(CaptureViewModel.AirFileType.IMAGE, captureTimestamp)
                val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val imageFile = airImageUtil.createImageFile(storageDir, imageName)

                imageFile?.let {
                    captureFile = imageFile
                    val photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.ahandyapp.airnavx",
                        captureFile
                    )

                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, AirConstant.REQUEST_IMAGE_CAPTURE)
                }
            } catch (ex: Exception) {
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "dispatchTakePictureIntent -> NO camera launch Exception ${ex.message}...")
            }
        } else {  // THUMBNAIL_ONLY
            // for thumbnail only, do NOT supply image file URI
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                startActivityForResult(takePictureIntent, AirConstant.REQUEST_IMAGE_CAPTURE)
            } catch (ex: Exception) {
                Toast.makeText(this.context, "NO camera launch...", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "dispatchTakePictureIntent -> NO camera launch Exception ${ex.stackTrace}...")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG,"dispatchTakePictureIntent onActivityResult requestCode ${requestCode}, resultCode $resultCode")
        // request code match & result OK
        if (requestCode == AirConstant.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
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
                        val airCapture = AirCapture()
                        // capture image attributes
                        airCapture.timestamp = captureTimestamp
                        airCapture.imagePath = Environment.DIRECTORY_PICTURES
                        airCapture.imageName = airCapture.getAirFilename(CaptureViewModel.AirFileType.IMAGE, captureTimestamp)

                        // update image length & width prior to thumbnail extraction
                        airCapture.imageWidth = airCaptureBitmap.width
                        airCapture.imageHeight = airCaptureBitmap.height
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult width ${airCapture.imageWidth} X height ${airCapture.imageHeight}")

                        /////////////////////////////////////
                        // extractEXIF(photoFile): Boolean
                        val exifExtracted = airImageUtil.extractExif(captureFile, airCapture)
                        Log.d(TAG, "dispatchTakePictureIntent onActivityResult exifExtracted $exifExtracted")

                        // captureMeters(airCapture): Boolean
                        val metersCaptured = captureMeters(airCapture)
                        Log.d(TAG,"dispatchTakePictureIntent onActivityResult metersCaptured $metersCaptured")

                        // extract thumbnail at scale factor
                        val thumbBitmap = airImageUtil.extractThumbnail(currentPhotoPath, airCaptureBitmap, captureViewModel.THUMB_SCALE_FACTOR)
                        // add set to view model
                        airImageUtil.addViewModelSet(captureViewModel, captureViewModel.gridView, captureViewModel.imageViewPreview,
                            airCaptureBitmap, thumbBitmap, null, null, airCapture)

                        // update grid view adapter
                        updateGridViewAdapter(captureViewModel.gridView, captureViewModel.gridLabelArray, captureViewModel.gridBitmapArray)

                        // write AirCapture
                        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                        val captureRecorded = airCapture.write(storageDir, captureTimestamp, airCapture)
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
        } else if (requestCode == AirConstant.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_CANCELED) {
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

                Log.i("TAG", "establishGestureDetector resetting view model...")
                // if unable to restore captureViewModel from previous session
                if (!airImageUtil.fetchViewModel(context!!, activity!!, captureViewModel)) {
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

    /////////////////////////////view model helpers////////////////////////////
    private fun createEmptyViewModel(): Boolean {
        // create empty bitmap to seed grid
        val blankBitmap = airImageUtil.createBlankBitmap(
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
        captureViewModel.zoomDirtyArray.add(false)
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
        // refresh viewmodel
        Log.d(TAG, "restoreViewModel refreshComplete $airCapture")
        val refreshComplete = refreshViewModel(airCapture)
        Log.d(TAG, "restoreViewModel refreshComplete $refreshComplete")
        Log.d(TAG,"restoreViewModel refreshComplete ${textViewPreview.text} ${textViewDecibel.text} ${textViewAngle.text}")

        return true
    }

    // update grid view adapter
    private fun updateGridViewAdapter(
        gridView: GridView,
        gridLabelArray: ArrayList<String>,
        gridBitmapArray: ArrayList<Bitmap>) {

        val gridViewAdapter = GridViewAdapter(this.requireContext(), gridLabelArray, gridBitmapArray)
        gridView.adapter = gridViewAdapter
        gridView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Toast.makeText(this.context, "Touch at " + gridLabelArray[+position], Toast.LENGTH_SHORT).show()
            captureViewModel.gridPosition = position
            captureViewModel.imageViewPreview.setImageBitmap(gridBitmapArray[captureViewModel.gridPosition])
            // sync AirCapture to thumb selection
            val airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
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