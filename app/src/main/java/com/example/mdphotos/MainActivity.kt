package com.example.mdphotos

import android.content.Intent
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.camera.core.ImageCaptureException
import androidx.core.content.FileProvider
import com.example.mdphotos.databinding.ActivityMainBinding
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var lastItems: Array<String>
    private lateinit var sessionItems: MutableList<String>
    private lateinit var btn0: Button
    private lateinit var btn1: Button
    private lateinit var btn2: Button
    private lateinit var sendButton: Button
    private lateinit var send: Button

    private lateinit var expandButton: Button
    private lateinit var inputField: EditText
    private val fileList = mutableListOf<File>()
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "MyAppPreferences"
    private val KEY_INPUT_TEXT = "input_text"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        lastItems = Array(4) { "..." }

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val savedText = sharedPreferences.getString(KEY_INPUT_TEXT, "")

        val bg = viewBinding.VLL!!
        clearAppCache(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        val directory = viewBinding.directoryText
        btn0 = viewBinding.btn0
        btn1 = viewBinding.btn1
        btn2 = viewBinding.btn2

        sendButton = viewBinding.sendButton!!
        expandButton = viewBinding.expandButton!!
        inputField = viewBinding.textEmailBg!!
        inputField.setText(savedText)
        viewBinding.takePhotoBtn.setOnClickListener {
            takePhoto(directory.text.toString())
            changeLastItems(directory.text.toString())
        }

        btn0.setOnClickListener { directory.setText(btn0.text) }
        btn1.setOnClickListener { directory.setText(btn1.text) }
        btn2.setOnClickListener { directory.setText(btn2.text) }

        expandButton.setOnClickListener {
            if (inputField.visibility == View.GONE) {
                inputField.visibility = View.VISIBLE
                sendButton.visibility = View.VISIBLE
                expandButton.text = getString(R.string.collapse)
                bg.setBackgroundColor(Color.BLACK)
            } else {
                inputField.visibility = View.GONE
                sendButton.visibility = View.GONE
                expandButton.text = getString(R.string.expand)
                bg.setBackgroundColor(Color.TRANSPARENT)

            }
        }

        sendButton.setOnClickListener {
            val text = inputField.text.toString()
            if (text.isNotBlank()) {
                sendPhotos(text)
                inputField.visibility = View.GONE
                sendButton.visibility = View.GONE
                expandButton.text = getString(R.string.expand)
            } else {
                Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        sessionItems = arrayOf<String>().toMutableList()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray("numbers", lastItems)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val numbers = savedInstanceState.getStringArray("numbers")
        if (numbers != null) {
            lastItems = numbers
            changeBtnText()
        }
    }

    private fun changeLastItems(lastItem: String) {
        if (lastItems.contains(lastItem)) {
            return
        } else {
            lastItems[2] = lastItems[1]
            lastItems[1] = lastItems[0]
            lastItems[0] = lastItem
            changeBtnText()
            sessionItems.add(lastItem)
        }
    }

    private fun changeBtnText() {
        btn0.text = lastItems[0]
        btn1.text = lastItems[1]
        btn2.text = lastItems[2]
    }

    private fun takePhoto(directory: String) {
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, directory+"_data_"+name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$directory")
        }
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        val filePath = getFileFromUri(savedUri)
                        if (filePath != null) {
                            fileList.add(filePath)
                        }
                    }
                }
            }
        )
    }

    private fun getFileFromUri(uri: Uri): File? {
        val path = getPathFromUri(uri) ?: return null
        return File(path)
    }
    private fun getPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null).use { cursor ->
            if (cursor != null) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                if (cursor.moveToFirst()) {
                    return cursor.getString(columnIndex)
                }
            }
        }
        return null
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                imageCapture = ImageCapture.Builder().setJpegQuality(60).build()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
        ).toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    @SuppressLint("SetWorldWritable")
    private fun sendPhotos(text: String) {
        if (fileList.isEmpty()) {
            Log.e(TAG, "No files to add to the zip")
            Toast.makeText(this, "No files found to send", Toast.LENGTH_SHORT).show()
            return
        }

        val compressed_photos = File(cacheDir, "photos.zip")
        compressed_photos.setWritable(true, false)
        try {
            if (compressed_photos.createNewFile()) {
                println("File created: $compressed_photos")
            } else {
                if (compressed_photos.delete()) {
                    compressed_photos.createNewFile()
                    compressed_photos.setWritable(true, false)
                    println("File deleted and recreated: $compressed_photos")
                } else {
                    println("File not deleted.")
                    return
                }
            }
        } catch (e: IOException) {
            println("An error occurred.")
            e.printStackTrace()
        }
        compressed_photos.parentFile?.mkdirs()
        createZipFile(fileList, compressed_photos)
        val compressed_photos_uri: Uri = FileProvider.getUriForFile(
            this, "com.example.mdphotos", compressed_photos)
        sendEmail(compressed_photos_uri, text)
    }


    private fun createZipFile(files: List<File>, outputZipFile: File): File {
        ZipOutputStream(FileOutputStream(outputZipFile)).use { zipOut ->
            files.forEach { file ->
                println("Adding file to zip: ${file.absolutePath}")
                FileInputStream(file).use { fis ->
                    val zipEntry = ZipEntry(file.name)
                    zipOut.putNextEntry(zipEntry)
                    fis.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
        return outputZipFile
    }

    private fun sendEmail(uri: Uri, email: String) {
        try {
            val date = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            val subject = "fotos"
            val emailIntent = Intent(Intent.ACTION_SEND)
            emailIntent.type = "plain/text"
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
            emailIntent.putExtra(Intent.EXTRA_TEXT, date)
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            this.startActivity(Intent.createChooser(emailIntent, "Sending email..."))
        } catch (t: Throwable) {
            Toast.makeText(this, "Request failed try again: $t", Toast.LENGTH_LONG).show()
        }
    }
    override fun onPause() {
        super.onPause()
        val editor = sharedPreferences.edit()
        editor.putString(KEY_INPUT_TEXT, inputField.text.toString())
        editor.apply()
    }
    private fun clearAppCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            deleteDirContents(cacheDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteDirContents(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDir(File(dir, child))
                    if (!success) {
                        return false
                    }
                }
            }
        }
        // Do not delete the directory itself
        return true
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            if (children != null) {
                for (child in children) {
                    val success = deleteDir(File(dir, child))
                    if (!success) {
                        return false
                    }
                }
            }
        }
        return dir?.delete() ?: false
    }
    }
