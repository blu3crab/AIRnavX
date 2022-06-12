package com.ahandyapp.airnavx.model

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.media.ThumbnailUtils.extractThumbnail
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.GridView
import android.widget.ImageView
import android.widget.Toast
import com.ahandyapp.airnavx.model.AirConstant.DEFAULT_EXTENSION_SEPARATOR
import com.ahandyapp.airnavx.ui.capture.CaptureViewModel
import com.ahandyapp.airnavx.ui.gallery.GalleryViewModel
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

class AirImageUtil {
    private val TAG = "AirImageUtil"

    ///////////////////////////////////////////////////////////////////////////
    // convert bitmap & store to file
    fun convertBitmapToFile(context: Context, bitmap: Bitmap, imageFilename: String ): Boolean {
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        try {
            val filepath = storageDir.toString() + File.separator + imageFilename + DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT
            val imageFile = File(filepath)
            imageFile.createNewFile()
            val output = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            output.flush()
            output.close()
            Log.d(TAG, "convertBitmapToFile -> AirImage: stored $filepath...")
        } catch (ex: Exception) {
            Toast.makeText(context, "AirImage: unable to store $imageFilename...", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "convertBitmapToFile -> AirImage: unable to store $imageFilename...\n${ex.message}...")
            return false
        }

        return true
    }
    ///////////////////////////////////////////////////////////////////////////
    // show alert dialog & delete file set if confirmed
    // TODO: untangle dialog & file ops
    fun showDeleteAlertDialog(context: Context, activity: Activity, galleryViewModel: GalleryViewModel?, captureViewModel: CaptureViewModel) {
        // build alert dialog
        val dialogBuilder = AlertDialog.Builder(context)

        // set message of alert dialog
        dialogBuilder.setMessage("Delete AIR capture files?")
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Delete", DialogInterface.OnClickListener {
                    dialog, id ->
                val airCapture = captureViewModel.airCaptureArray[captureViewModel.gridPosition]
                Toast.makeText(context, "Deleting AIR capture files for ${airCapture.timestamp}...", Toast.LENGTH_SHORT).show()
                Log.d("TAG", "showDeleteAlertDialog deleting AIR capture files for ${airCapture.timestamp}")
                try {
                    // set path = storage dir + time.jpg
                    val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                    val imagePath = Paths.get(storageDir.toString() + File.separator +
                            AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp  +
                            AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT)
                    val dataPath = Paths.get(storageDir.toString() + File.separator +
                            AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp  +
                            AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_DATAFILE_EXT)
                    val zoomPath = Paths.get(storageDir.toString() + File.separator +
                            AirConstant.DEFAULT_FILE_PREFIX + airCapture.timestamp  +
                            AirConstant.DEFAULT_ZOOM_SUFFIX + AirConstant.DEFAULT_EXTENSION_SEPARATOR +
                            AirConstant.DEFAULT_IMAGEFILE_EXT)
                    Log.d("TAG", "showDeleteAlertDialog deleting files \n$imagePath \n$dataPath \n$zoomPath")
                    // Files.deleteIfExists(path)
                    var result = Files.deleteIfExists(imagePath)
                    if (result) {
                        Log.d("TAG", "showDeleteAlertDialog delete image $imagePath success...")
                    } else {
                        Log.d("TAG", "showDeleteAlertDialog delete image $imagePath failed...")
                    }
                    result = Files.deleteIfExists(dataPath)
                    if (result) {
                        Log.d("TAG", "showDeleteAlertDialog delete datafile $dataPath success...")
                    } else {
                        Log.d("TAG", "showDeleteAlertDialog delete datafile $dataPath failed...")
                    }
                    result = Files.deleteIfExists(zoomPath)
                    if (result) {
                        Log.d("TAG", "showDeleteAlertDialog delete zoomimage $zoomPath success...")
                    } else {
                        Log.d("TAG", "showDeleteAlertDialog delete zoomimage $zoomPath failed...")
                    }

                } catch (ioException: IOException) {
                    ioException.printStackTrace()
                } finally {
                    Log.d("TAG", "establishGestureDetector refreshing gallery...")
                    val airImageUtil = AirImageUtil()
                    airImageUtil.fetchViewModel(context, activity, captureViewModel)
                    // refresh gallery view if defined
                    if (galleryViewModel != null) {
                        airImageUtil.refreshGalleryView(galleryViewModel, captureViewModel)
                    }

                }

                dialog.dismiss()
            })
            // negative button text and action
            .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("AlertDialogExample")
        // show alert dialog
        alert.show()
    }
    ///////////////////////////////////////////////////////////////////////////
    // fetch air image sets from storage
    fun fetchViewModel(context: Context, activity: Activity, captureViewModel: CaptureViewModel): Boolean {

        var airCaptureJson: AirCaptureJson = AirCaptureJson()

        Toast.makeText(context, "Fetching AIR capture files from storage...", Toast.LENGTH_SHORT).show()

        // reset viewmodel grid
        captureViewModel.gridPosition = 0
        captureViewModel.gridCount = 0
        captureViewModel.gridLabelArray.clear()
        captureViewModel.origBitmapArray.clear()
        captureViewModel.zoomBitmapArray.clear()
        captureViewModel.overBitmapArray.clear()
        captureViewModel.airCaptureArray.clear()

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
                    var zoomBitmap = fetchBitmap(context, activity, name, AirConstant.DEFAULT_ZOOM_SUFFIX)
                    if (zoomBitmap != null) {
                        Log.d(TAG,"fetchViewModel zoomBitmap w x h = ${zoomBitmap.width} x ${zoomBitmap.height}")
                    }
                    // attempt to open associated over image file
                    var overBitmap = fetchBitmap(context, activity, name, AirConstant.DEFAULT_OVER_SUFFIX)
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

    ///////////////////////////////////////////////////////////////////////////
    // test for file suffixes
    private fun hasRequiredSuffix(file: File): Boolean {
        val requiredSuffixes = listOf(AirConstant.DEFAULT_IMAGEFILE_EXT)
        return requiredSuffixes.contains(file.extension)
    }
    ///////////////////////////////////////////////////////////////////////////
    // convert image file to bitmap
    // TODO: rename to compliment convertBitmapToFile
    private fun fetchBitmap(context: Context, activity: Activity, name: String, suffix: String): Bitmap? {
        var zoomBitmap: Bitmap? = null
        try {
            val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val airImageName =
                name + suffix + AirConstant.DEFAULT_EXTENSION_SEPARATOR + AirConstant.DEFAULT_IMAGEFILE_EXT
            Log.d(TAG, "fetchBitmap airImageName $airImageName...")
            val airImagePath = storageDir.toString() + File.separator + airImageName
            Log.d(TAG, "fetchBitmap airImagePath $airImagePath...")
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
                    "fetchBitmap zoomBitmap w x h = ${zoomBitmap.width} x ${zoomBitmap.height}"
                )
            }
        }
        catch (ex: FileNotFoundException) {
            Log.d(TAG, "fetchBitmap FileNotFoundException...")
        }
        catch (ex: Exception) {
            Log.e(TAG, "fetchBitmap exception ${ex.message}")
        }
        return zoomBitmap
    }
    ///////////////////////////////////////////////////////////////////////////
    // extract thumbnail from bitmap
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

    ///////////////////////////////////////////////////////////////////////////
    // set gallery image to selected capture thumb
    fun refreshGalleryView(galleryViewModel: GalleryViewModel, captureViewModel: CaptureViewModel): Boolean {
        Log.d(TAG, "refreshGalleryView grid position ${captureViewModel.gridPosition}, grid count ${captureViewModel.gridCount}")
        // adjust grid position to valid range
        if (captureViewModel.gridCount > 0 && captureViewModel.gridPosition >= captureViewModel.gridCount ) {
            captureViewModel.gridPosition = captureViewModel.gridCount - 1
        }
        if (captureViewModel.gridPosition >= 0 && captureViewModel.gridPosition < captureViewModel.gridCount) {
            galleryViewModel.captureBitmap = captureViewModel.origBitmapArray[captureViewModel.gridPosition]
            galleryViewModel.zoomBitmap = captureViewModel.zoomBitmapArray[captureViewModel.gridPosition]
            galleryViewModel.overBitmap = captureViewModel.overBitmapArray[captureViewModel.gridPosition]
            galleryViewModel.galleryImageView.setImageBitmap(galleryViewModel.overBitmap)
            return true
        }
        return false
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
//        // TODO: invoke in onCreate
//        updateGridViewAdapter(gridView, captureViewModel.gridLabelArray, captureViewModel.gridBitmapArray)
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

}