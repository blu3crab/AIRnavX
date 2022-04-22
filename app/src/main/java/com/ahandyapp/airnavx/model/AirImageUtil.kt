package com.ahandyapp.airnavx.model

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class AirImageUtil {
    private val TAG = "AirImageUtil"

    fun convertBitmapToFile(context: Context, bitmap: Bitmap, imageFilename: String ): Boolean {
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!

        try {
            // TODO: generalize name - DEFAULT_ZOOM_SUFFIX
//            val filepath = storageDir.toString() + File.separator + imageFilename +
//                    AirConstant.DEFAULT_ZOOM_SUFFIX + "." + AirConstant.DEFAULT_IMAGEFILE_EXT
            val filepath = storageDir.toString() + File.separator + imageFilename + "." + AirConstant.DEFAULT_IMAGEFILE_EXT
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

}