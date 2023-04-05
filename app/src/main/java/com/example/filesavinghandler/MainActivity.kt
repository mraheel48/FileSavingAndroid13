package com.example.filesavinghandler

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.filesavinghandler.databinding.ActivityMainBinding
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val requestCodePermission = 123
    private val permissionList = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }
    private var workerThread: ExecutorService = Executors.newCachedThreadPool()
    private lateinit var binding: ActivityMainBinding

    //Check Status of Activity Permission
    //1. Permission Access allowed GRANTED
    //2. Permission Access not Allowed
    //3. Permission Access not Allowed permanently DENIED
    var checkStatus: Int = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            Log.d("myPermission", "${checkPermissions()}")
        }

        binding.button2.setOnClickListener {
            if (checkRationale() == 1) {
                workerThread.execute {
                    saveMediaFileToStorage("txt1.txt", readFromAsset("txt1.txt"), "PDF")
                }
            } else {
                Log.d("myFileTag", "checkPermissions not allowed")
            }
        }
    }

    //This method calling after the CheckPermissions
    fun checkRationale(): Int {
        return if (checkPermissions()) {
            1
        } else {
            checkStatus
        }
    }

    private fun readFromAsset(filename: String): InputStream {
        return application.assets.open(filename)
    }

    private fun saveMediaFileToStorage(
        fileName: String,
        inputStream: InputStream,
        dirName: String
    ) {
        //Output stream
        val fos: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveFileToDownloadsQ(fileName, dirName)
        } else {
            FileOutputStream(saveFileToDownloadInternal(fileName, dirName))
        }
        if (fos != null) {
            val bufferNew = ByteArray(1024)
            var len: Int
            while (inputStream.read(bufferNew).also { len = it } > 0) {
                fos.write(bufferNew, 0, len)
            }
            inputStream.close()
            fos.close()
            Log.d("myFileTag", "file is saved")
        } else {
            Log.d("myFileTag", "OutputStream is null file is not save ")
        }
    }

    private fun checkPermissions(): Boolean {
        // Check if all required permissions are granted
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else if (!allPermissionsGranted()) {
            // Request permissions
            ActivityCompat.requestPermissions(this, permissionList, requestCodePermission)
            false
        } else {
            true
        }
    }

    // Check if all required permissions are granted
    private fun allPermissionsGranted() = permissionList.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissionRationale(): Boolean {
        var permissionDenied = false
        permissionList.forEachIndexed { _, permission ->
            if (shouldShowRequestPermissionRationale(permission)) {
                permissionDenied = true
            }
            Log.d("myPermissionRationale", "${shouldShowRequestPermissionRationale(permission)}")
        }
        return permissionDenied
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermission) {
            if (allPermissionsGranted()) {
                // All permissions granted
                Log.d("myRequestPer", "All permissions granted")
                checkStatus = 1
            } else {
                // Permission(s) denied
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkStatus = if (checkPermissionRationale()) {
                        2
                    } else {
                        3
                    }
                    Log.d("myRequestPer", "Some permissions denied ${checkPermissionRationale()}")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveFileToDownloadsQ(filename: String, dirName: String): OutputStream? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/$dirName")
        }
        val resolver = contentResolver ?: return null
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
        )
        return uri?.let { resolver.openOutputStream(it) }
    }

    private fun saveFileToDownloadInternal(filename: String, dirName: String): File {
        val dirDest = File(getRootPath(), dirName)
        if (!dirDest.exists()) {
            dirDest.mkdirs()
        }
        return File(dirDest, filename)
    }

    private val baseLocalPath =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}"

    private fun getRootPath(): String {
        val root = baseLocalPath
        val dirDest = File(root)
        if (!dirDest.exists()) {
            dirDest.mkdirs()
        }
        return root
    }
}