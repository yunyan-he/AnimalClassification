package com.example.animalclaasification


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.animalclaasification.databinding.ActivityResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ai.onnxruntime.*
import android.widget.ImageView
import org.json.JSONObject
import retrofit2.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.min

/**
 * Displays recognition results for an image, using ONNX models for local inference and Gemini API for alternative results.
 * Allows navigation to an encyclopedia page via WebView.
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var animalModel: OrtSession
    private lateinit var detailedModel: OrtSession

    private var recognitionState: RecognitionState = RecognitionState.Initial
    private val geminiApiKey: String = BuildConfig.GEMINI_API_KEY

    companion object {
        private const val TAG = "ResultActivity"
        private const val CAT_INDEX = 9
        private const val DOG_INDEX = 18
        private const val IMAGE_HEIGHT_DP = 300
        private const val UNRECOGNIZED_INDEX = -1 // Special index for unrecognized animal
        // This is the prompt will send to Gemini
        private const val GEMINI_PROMPT = """
            Identify the animal in this image.
            Respond with only the common name of the animal, for example: 'Lion' or 'Bengal Tiger'.
            Do not add any other text, explanation, or punctuation.
        """
    }

    // UI Setup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeOnnxModels()
        setupClickListeners()
        processIntentData()
    }

    /** Initializes ONNX models for coarse and fine-grained recognition. */
    private fun initializeOnnxModels() {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            animalModel = loadOnnxModel("animal_recognition_model_90animals.onnx")
            detailedModel = loadOnnxModel("animal_recognition_model_dog_cat.onnx")
        } catch (e: Exception) {
            handleError("Failed to load ONNX models: ${e.message}", finishActivity = true)
        }
    }

    /** Sets up click listeners for Gemini API and detail page buttons. */
    private fun setupClickListeners() {
        binding.btnDetailPage.setOnClickListener {
            val url = when (val state = recognitionState) {
                is RecognitionState.Result -> if (state.isShowingRemote) state.remoteDetailUrl else state.localDetailUrl
                else -> null
            }
            url?.let {
                startActivity(Intent(this, InfoActivity::class.java).putExtra("url", it))
            } ?: Toast.makeText(this, "No valid URL available", Toast.LENGTH_SHORT).show()
        }
        binding.btnGeminiApi.setOnClickListener {
            when (val state = recognitionState) {
                is RecognitionState.Initial -> fetchGeminiResult()
                is RecognitionState.Result -> {
                    if (state.remoteAnimalType == null) {
                        fetchGeminiResult()
                    } else {
                        toggleRecognitionResult()
                    }
                }
            }
        }
    }
    /** Switches UI to display Gemini recognition result. */
    private fun toggleRecognitionResult() {
        recognitionState = when (val current = recognitionState) {
            is RecognitionState.Result -> current.copy(isShowingRemote = !current.isShowingRemote)
            else -> current
        }
        showResult()
    }


    /** Fetches Gemini API recognition result for the image. */
    private fun fetchGeminiResult() {
        lifecycleScope.launch {
            binding.loadingLayout.visibility = View.VISIBLE
            try {
                val bitmap = loadBitmapFromUri(Uri.parse(intent.getStringExtra("imageUri")!!))
                    ?: throw Exception("Image load failed")

                // This function is reused from your old code
                val base64Image = encodeImageToBase64(bitmap)

                // 1. Build the Gemini Request Body
                // We send both the prompt and the inline image data
                val geminiRequest = GeminiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = GEMINI_PROMPT),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    )
                )

                // 2. Call the Gemini API (using IO Dispatcher)
                val response = withContext(Dispatchers.IO) {
                    GeminiApiClient.api.generateContent(geminiRequest, geminiApiKey)
                }

                // 3. Parse the Gemini Response
                // The animal name is in the text of the first candidate's first part
                val animalName = response.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()
                    ?.text?.trim() // .trim() is important to remove newlines
                    ?: throw Exception("Gemini returned no text or invalid response format")

                if (animalName.isBlank()) {
                    throw Exception("Gemini returned blank text")
                }

                // 4. (REUSE) Now, fetch the Wikipedia intro for the name Gemini gave us
                val animalIntro = fetchWikiIntro(animalName)
                val wikipediaUrl = "https://en.wikipedia.org/wiki/$animalName"

                // 5. Update the state with the new remote data
                recognitionState = when (val current = recognitionState) {
                    is RecognitionState.Result -> current.copy(
                        remoteAnimalType = animalName,
                        remoteAnimalIntro = animalIntro,
                        remoteDetailUrl = wikipediaUrl,
                        isShowingRemote = true
                    )
                    else -> RecognitionState.Result(
                        remoteAnimalType = animalName,
                        remoteAnimalIntro = animalIntro,
                        remoteDetailUrl = wikipediaUrl,
                        isShowingRemote = true
                    )
                }
                showResult()
                bitmap.recycle()
            } catch (e: Exception) {
                // Error message will now show Gemini errors
                handleError("Gemini API error, please try again later: ${e.message}")
            } finally {
                binding.loadingLayout.visibility = View.GONE
            }
        }
    }

    /** Processes image URI and recognition settings from intent. */
    private fun processIntentData() {
        val imageUriString = intent.getStringExtra("imageUri")
        val enableDetailed = intent.getBooleanExtra("ENABLE_DETAILED_RECOGNITION", false)

        if (imageUriString.isNullOrEmpty()) {
            handleError("No image URI provided", finishActivity = true)
            return
        }

        val imageUri = Uri.parse(imageUriString)
        val bitmap = loadBitmapFromUri(imageUri) ?: run {
            handleError("Failed to load image", finishActivity = true)
            return
        }

        setImageWithProportions(binding.imageView, bitmap)
        processImage(imageUri, enableDetailed)
    }

    /** Sets image to 300dp height while maintaining aspect ratio. */
    private fun setImageWithProportions(imageView: ImageView, bitmap: Bitmap) {
        imageView.setImageBitmap(bitmap)
    }


    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e("ResultActivity", "Failed to load image from Uri: ${e.message}")
            null
        }
    }

    // Image Processing and Recognition
    /** Processes image for recognition, using coarse and optional fine-grained models. */
    private fun processImage(imageUri: Uri, enableDetailed: Boolean) {
        lifecycleScope.launch {
            binding.loadingLayout.visibility = View.VISIBLE
            try {
                val bitmap = loadBitmapFromUri(imageUri) ?: throw Exception("Image load failed")
                val index = recognizeAnimal(bitmap, animalModel, "Coarse-grained recognition stage")
                if (index == UNRECOGNIZED_INDEX) {
                    recognitionState = RecognitionState.Result(
                        localAnimalType = "Could not recognize an animal in the picture",
                        localAnimalIntro = "Could not recognize an animal in the picture",
                        localDetailUrl = null
                    )
                    showResult()
                    bitmap.recycle()
                    return@launch
                }
                val detailedIndex = if (enableDetailed && (index == CAT_INDEX || index == DOG_INDEX)) {
                    recognizeAnimal(bitmap, detailedModel, "Fine-grained recognition stage")
                } else null
                if (detailedIndex == UNRECOGNIZED_INDEX) {
                    recognitionState = RecognitionState.Result(
                        localAnimalType = "Could not recognize an animal in the picture",
                        localAnimalIntro = "Could not recognize an animal in the picture",
                        localDetailUrl = null
                    )
                    showResult()
                    bitmap.recycle()
                    return@launch
                }


                val finalIndex = detailedIndex ?: index
                val animalType = getAnimalName(finalIndex, detailedIndex != null)
                val animalIntro = fetchWikiIntro(animalType)

                recognitionState = RecognitionState.Result(
                    localAnimalType = animalType,
                    localAnimalIntro = animalIntro,
                    localDetailUrl = "https://en.wikipedia.org/wiki/$animalType"
                )
                showResult()
                bitmap.recycle() // Free memory
            } catch (e: Exception) {
                handleError("Recognition failed: ${e.message}")
            } finally {
                binding.loadingLayout.visibility = View.GONE
            }
        }
    }

    /** Recognizes animal in bitmap, returning the predicted class index. */
    private fun recognizeAnimal(bitmap: Bitmap, model: OrtSession, stageName: String): Int {
        val inputArray = preprocessImage(bitmap)
        val inputShape = longArrayOf(1, 3, 224, 224)
        val inputName = model.inputNames.iterator().next()
        // Start timing
        val startTime = System.nanoTime()
        Log.d("ResultActivity", "Starting inference for $stageName")

        try {
            OnnxTensor.createTensor(ortEnv, FloatBuffer.wrap(inputArray), inputShape).use { inputTensor ->
                model.run(Collections.singletonMap(inputName, inputTensor)).use { outputs ->
                    val outputTensor = outputs?.get(0) as? OnnxTensor
                        ?: throw Exception("Output tensor is null")

                    val outputArray = outputTensor.value as? Array<FloatArray>
                        ?: throw Exception("Invalid output format")

                    if (outputArray.isEmpty()) throw Exception("Empty output array")
                    val scores = outputArray[0]
                    if (scores.isEmpty()) throw Exception("Empty scores array")

                    val probabilities = softMax(scores)
                    val maxProbability = probabilities.maxOrNull() ?: 0f
                    Log.d("ResultActivity", "probability : $maxProbability")
                    if (maxProbability < 0.10f) {
                        Log.d(TAG, "$stageName: Max probability ($maxProbability) is less than threshold, returning UNRECOGNIZED_INDEX")
                        return UNRECOGNIZED_INDEX
                    }
                    val top3Indices = getTop3(probabilities)
                    return top3Indices.firstOrNull() ?: throw Exception("No valid prediction")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference error in $stageName: ${e.message}", e)
            throw e // Rethrow to handle in caller
        }finally {
            // End timing and log duration
            val endTime = System.nanoTime()
            val durationMs = (endTime - startTime) / 1_000_000.0 // Convert nanoseconds to milliseconds
            Log.d("ResultActivity", "$stageName inference completed in $durationMs ms")
        }
    }

    /** Preprocesses bitmap to a float array for ONNX model input. */
    private fun preprocessImage(bitmap: Bitmap): FloatArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val pixels = IntArray(224 * 224)
        resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val floatArray = FloatArray(3 * 224 * 224)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            floatArray[i] = ((pixel shr 16 and 0xFF) / 255.0f - mean[0]) / std[0] // R
            floatArray[i + 224 * 224] = ((pixel shr 8 and 0xFF) / 255.0f - mean[1]) / std[1] // G
            floatArray[i + 2 * 224 * 224] = ((pixel and 0xFF) / 255.0f - mean[2]) / std[2] // B
        }
        resizedBitmap.recycle()
        return floatArray
    }

    /** Retrieves animal name from JSON labels based on index and model type. */
    private fun getAnimalName(index: Int, isDetailed: Boolean): String {
        val jsonFile = if (isDetailed) "labels_dog_cat.json" else "labels_90animals.json"
        return try {
            val labelsJson = assets.open(jsonFile).bufferedReader().use { it.readText() }
            JSONObject(labelsJson).optString(index.toString(), "Unknown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load labels: ${e.message}", e)
            "Unknown"
        }
    }




    /** Encodes bitmap to Base64 for Gemini API. */
    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes = stream.toByteArray()
        stream.close()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Fetches encyclopedia introduction for the animal type. */
    private suspend fun fetchWikiIntro(animalName: String): String = withContext(Dispatchers.IO) {
        try {
            // 1. Use the new WikipediaClient
            // 2. Call the new fetchSummary method
            // 3. Get the 'extract' field from the response
            val response = WikiIntroClient.api.fetchSummary(animalName = animalName).await()
            response.extract ?: "No summary available." // Provide a default value in case extract is null
        } catch (e: Exception) {
            // Update the log message
            Log.e(TAG, "Failed to fetch Wikipedia summary: ${e.message}", e)
            "Unable to fetch summary" // Return this message on error
        }
    }

    // UI Updates and Error Handling
    /** Updates UI with current recognition result. */
    private fun showResult() {
        when (val state = recognitionState) {
            is RecognitionState.Result -> {
                if (state.localAnimalType == "Could not recognize an animal in the picture") {
                    binding.recognitionResult.text = "Result: Could not recognize an animal in the picture"
                    binding.GeminiInfo.text = "Could not recognize an animal in the picture"
                    binding.btnGeminiApi.visibility = View.GONE
                    binding.btnDetailPage.visibility = View.GONE
                } else if (state.isShowingRemote && state.remoteAnimalType != null) {
                    binding.recognitionResult.text = "Result: ${state.remoteAnimalType}"
                    binding.GeminiInfo.text = state.remoteAnimalIntro ?: ""
                    binding.btnGeminiApi.text = "View Local Result"
                    binding.btnGeminiApi.visibility = View.VISIBLE
                    binding.btnDetailPage.visibility = View.VISIBLE
                } else if (state.localAnimalType != null) {
                    binding.recognitionResult.text = "Result: ${state.localAnimalType}"
                    binding.GeminiInfo.text = state.localAnimalIntro ?: ""
                    binding.btnGeminiApi.text = "View Gemini Result"
                    binding.btnGeminiApi.visibility = View.VISIBLE
                    binding.btnDetailPage.visibility = View.VISIBLE
                } else {
                    handleError("No recognition result available")
                }
            }
            is RecognitionState.Initial -> handleError("No recognition result available")
        }
        binding.loadingLayout.visibility = View.GONE
    }

    /** Handles errors with logging and user feedback. */
    private fun handleError(message: String, finishActivity: Boolean = false) {
        Log.e(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (finishActivity) finish()
    }

    // ONNX Model Loading
    /** Loads an ONNX model from assets. */
    private fun loadOnnxModel(assetFileName: String): OrtSession {
        val modelPath = assetFilePath(this, assetFileName)
        return ortEnv.createSession(modelPath, OrtSession.SessionOptions())
    }

    /** Copies asset file to internal storage if needed. */
    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return file.absolutePath
    }

    // Resource Cleanup
    override fun onDestroy() {
        super.onDestroy()
        try {
            animalModel.close()
            detailedModel.close()
            ortEnv.close()
            Log.i(TAG, "ONNX resources closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ONNX resources: ${e.message}", e)
        }
    }

    // Utility Functions
    /** Computes softmax probabilities from raw scores. */
    private fun softMax(scores: FloatArray): FloatArray {
        val result = scores.copyOf()
        val max = result.maxOrNull() ?: 0f
        var sum = 0f

        result.indices.forEach { i ->
            result[i] = kotlin.math.exp(result[i] - max)
            sum += result[i]
        }

        if (sum != 0f) result.indices.forEach { i -> result[i] /= sum }
        return result
    }

    /** Returns indices of top 3 probabilities. */
    private fun getTop3(probabilities: FloatArray): List<Int> {
        if (probabilities.isEmpty()) return emptyList()
        return probabilities.indices
            .sortedByDescending { probabilities[it] }
            .take(min(3, probabilities.size))
    }

    // State Management
    /** Represents the current recognition state. */
    private sealed class RecognitionState {
        object Initial : RecognitionState()
        data class Result(
            val localAnimalType: String? = null,
            val localAnimalIntro: String? = null,
            val localDetailUrl: String? = null,
            val remoteAnimalType: String? = null,
            val remoteAnimalIntro: String? = null,
            val remoteDetailUrl: String? = null,
            val isShowingRemote: Boolean = false
        ) : RecognitionState()
    }
}