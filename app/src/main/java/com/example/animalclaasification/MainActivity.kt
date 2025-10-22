package com.example.animalclaasification

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.animalclaasification.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoUri: Uri

    // 为什么用object 有什么优点吗 这几个常量是做什么用的？
    companion object {
        private const val EXTRA_IMAGE_URI = "imageUri"
        private const val EXTRA_DETAILED_RECOGNITION = "ENABLE_DETAILED_RECOGNITION"
        private const val FILE_PROVIDER_AUTHORITY = "com.example.animalclaasification.fileprovider"
    }


    // 这种表达式是什么意思？箭头是啥意思？以下这几个方法 是怎么一个调用一个的
    // 说清楚哪些最后调用了系统 哪些是一层层的调用
    // ActivityResultContracts使用这个有什么好处吗？除了这个有哪些方法 为什么不建议其他方法？
    // Modern permission handling
    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            dispatchTakePictureIntent()
        } else {
            showToast("Camera permission not granted, unable to take a picture")
        }
    }

    // Modern camera and gallery handling
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            handleImageResult(photoUri)
        } else {
            showToast("Photo capture cancelled")
        }
    }

    private val pickPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageResult(it) } ?: showToast("Image selection cancelled")
    }

    // binding 为什么用viewbinding 不用r.id 是为什么?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 为什么要savedInstanceState 有什么必要吗 有什么优点？
        // 这个let语句是什么意思
        // Restore state
        savedInstanceState?.getString("photoUri")?.let {
            photoUri = Uri.parse(it)
        }

        // AlertDialog这是啥
        // Set up click listeners
        binding.cameraButton.setOnClickListener { checkCameraPermissionAndTakePhoto() }
        binding.galleryButton.setOnClickListener { dispatchSelectPictureIntent() }
        binding.infoIcon.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("About Detailed Recognition")
                .setMessage("By default, the app performs a basic recognition (e.g., cat/dog). If you check this option, after identifying a cat or dog, the system will further analyze its specific breed.")
                .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::photoUri.isInitialized) {
            outState.putString("photoUri", photoUri.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null // Prevent memory leaks
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            androidx.core.content.ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // 这个函数为什么要这么实现？ 用FileProvider有什么好处 除了这个为什么没有用其他的常见方法
    private fun dispatchTakePictureIntent() {
        try {
            val photoFile: File = ImageUtils.createImageFile(this)
            photoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile)
            takePicture.launch(photoUri)
        } catch (e: Exception) {
            showToast("Could not create image file: ${e.message}")
        }
    }

    private fun dispatchSelectPictureIntent() {
        pickPicture.launch("image/*")
    }

    private fun handleImageResult(imageUri: Uri) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(EXTRA_IMAGE_URI, imageUri.toString())
            putExtra(EXTRA_DETAILED_RECOGNITION, binding.checkboxDetail.isChecked)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

object ImageUtils {
    fun createImageFile(context: Context): File {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: throw IllegalStateException("Failed to get storage directory")
        return File.createTempFile("photo_", ".jpg", storageDir)
    }
}