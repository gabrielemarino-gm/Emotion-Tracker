package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import it.unipi.dii.emotion_tracker.databinding.ActivityCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity(), EmotionRecognizer.ResultsListener {
    private lateinit var viewBinding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var model: EmotionRecognizer
    private lateinit var bitmapBuffer: Bitmap
    private val detector = FaceDetection.getClient()

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        //bind layout to Kotlin objects
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        model = EmotionRecognizer(
            context = this,
            resultsListener = this)

        // create thread that will execute image processing
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewBinding.viewFinder.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(viewBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        // Copy out RGB bits to the shared bitmap buffer
                        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

                        val imageRotation = image.imageInfo.rotationDegrees

                        detectFaces(bitmapBuffer, imageRotation)
                    }
                }

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun detectFaces(bitmapBuffer: Bitmap, imageRotation: Int) {

        val inputImage = InputImage.fromBitmap(bitmapBuffer, imageRotation)

        val result = detector.process(inputImage)
            .addOnSuccessListener { faces ->
                // Task completed successfully

                for (face in faces){
                    val boundingBox = face.boundingBox
                    var startingPointLeft = bitmapBuffer.width - boundingBox.bottom
                    var width = boundingBox.width()
                    if (bitmapBuffer.width - boundingBox.bottom < 0){
                        startingPointLeft = 0
                    }
                    if (boundingBox.width() > boundingBox.bottom){
                        width = boundingBox.bottom
                    }

                    var startingPointTop = boundingBox.left
                    var height = boundingBox.height()
                    if (boundingBox.left < 0){
                        startingPointTop = 0
                    }
                    if (boundingBox.left + boundingBox.height() > bitmapBuffer.height){
                        height = bitmapBuffer.height - boundingBox.left
                    }
                    val faceCropImage = Bitmap.createBitmap(bitmapBuffer, startingPointLeft, startingPointTop,
                                                            width, height)
                    runOnUiThread {
                        val tv1 = viewBinding.Bitmap
                        tv1.setImageBitmap(faceCropImage)
                    }
                    model.detect(faceCropImage, imageRotation)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Exception:", e.toString())
            }
    }

    companion object {
        private const val TAG = "Emotion-tracker"
        private const val REQUEST_CODE_PERMISSION = 10
    }

    override fun onResults(results: Float, inferenceTime: Long, imageHeight: Int, imageWidth: Int){
        // Called when the EmotionRecognizer produces results
        runOnUiThread {
            val label = viewBinding.label
            label.text = results.toString()
        }

        // TODO send happiness_value and current location to database
    }

    override fun onError(error: String) {
        Toast.makeText(this,
            "Hello, onError arrived",
            Toast.LENGTH_SHORT).show()
    }

    override fun onInitialized() {
        // Request camera permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSION)
        }
    }
}