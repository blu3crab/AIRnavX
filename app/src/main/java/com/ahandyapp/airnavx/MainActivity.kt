///////////////////////////////////////////////////////////////////////////////
package com.ahandyapp.airnavx

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ahandyapp.airnavx.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
//    val TAG: String = MainActivity::class.java.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val PERMISSIONS_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val storageDir = this?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        Log.d(TAG, "onCreate storageDir = $storageDir...")
//        val jpegPath = "$storageDir/AIR-20220302_165123.jpg"
//        Log.d(TAG, "onCreate jpegPath = $jpegPath...")
//        val jpegFile = File(jpegPath)

        if(storageDir.exists()) {
            binding.appBarMain.fab.setOnClickListener { view ->
                val authority = this.applicationContext.packageName.toString()
                Log.d(TAG, "onShare authority = $authority...")

                val fileList = storageDir.listFiles()
                var uriList = ArrayList<Uri>()
                for (file in fileList) {
                    var name = file.name
                    Log.d(TAG, "onShare listFiles file name $name")
                    val airPath = "$storageDir/$name"
                    Log.d(TAG, "onCreate airPath = $airPath...")
                    val airFile = File(airPath)

                    val uri = FileProvider.getUriForFile(
                        this,
                        this.applicationContext.packageName.toString(),
                        airFile
                    )
                    uriList.add(uri)
                }
                // TODO: share succeeds - triggers permission exceptions - why?
                val intentShareFile = Intent(Intent.ACTION_SEND_MULTIPLE)
                intentShareFile.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                intentShareFile.type = "image/jpeg";
                startActivity(Intent.createChooser(intentShareFile, "Share AIR Files"));
            }
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                // Sense & Inspect -> top arrow nav to Capture
                // Gallery -> top menu hamburger
                R.id.nav_capture, R.id.nav_gallery
                //R.id.nav_capture, R.id.nav_gallery, R.id.nav_inspect
                //R.id.nav_capture, R.id.nav_inspect, R.id.nav_gallery, R.id.nav_sense
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (!hasPermissions(this, PERMISSIONS_REQUIRED)) {
            Log.d(TAG, "onCreate hasPermissions FALSE...")
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        private val PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.CAMERA)
    }
    // util method
    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
        Log.d(TAG, "it->$it")
        Log.d(TAG, "checkSelf->${ActivityCompat.checkSelfPermission(context, it)}")
        Log.d(TAG, "GRANTED->${PackageManager.PERMISSION_GRANTED}")
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
///////////////////////////////////////////////////////////////////////////////
