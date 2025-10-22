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
            showToast("未授予相机权限，无法拍照")
        }
    }

    // Modern camera and gallery handling
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            handleImageResult(photoUri)
        } else {
            showToast("拍照取消")
        }
    }

    private val pickPicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageResult(it) } ?: showToast("选图取消")
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
                .setTitle("深入识别说明")
                .setMessage("默认情况下，系统会先进行粗略识别（如猫/狗）。如果勾选此选项，识别出猫或狗后，系统会进一步分析猫或狗的具体品种。")
                .setPositiveButton("明白了") { dialog, _ -> dialog.dismiss() }
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
            showToast("无法创建图片文件: ${e.message}")
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
            ?: throw IllegalStateException("无法获取存储目录")
        return File.createTempFile("photo_", ".jpg", storageDir)
    }
}