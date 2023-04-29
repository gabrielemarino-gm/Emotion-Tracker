package it.unipi.dii.emotion_tracker

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.classifier.ImageClassifier


class EmotionRecognizer(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    val context: Context,
    val resultsListener: ResultsListener) {

        private val TAG = "EmotionRecognizer"

    init {

        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable: Boolean ->
            val optionsBuilder =
                TfLiteInitializationOptions.builder()
            if (gpuAvailable) {
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }
            TfLite.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            // call to overridden function in CameraActivity
            resultsListener.onInitialized()
        }.addOnFailureListener{
            resultsListener.onError("TfLiteVision failed to initialize: "
                    + it.message)

            Log.e(TAG, "TfLiteVision failed to initialize: " + it.message)
        }
    }

    fun detect(image: Bitmap, imageRotation: Int) {
        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor = ImageProcessor.Builder().add(Rot90Op(-imageRotation / 90))
                                                     .add(ResizeOp(48,48, null))
                                                     .add(TransformToGrayscaleOp())
                                                     .add(CastOp(DataType.FLOAT32)).build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val modelPath = FileUtil.loadMappedFile(context, "CNN_3_with_metadata_2.tflite")
        val model = InterpreterApi.create(modelPath, InterpreterApi.Options().setRuntime(InterpreterApi.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION))
        val results = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)
        model.run(tensorImage.buffer, results.buffer)
        model.close()
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        resultsListener.onResults(
            results.getFloatValue(0),
            inferenceTime,
            tensorImage.height,
            tensorImage.width)
    }

    interface ResultsListener {
        fun onInitialized()
        fun onError(error: String)
        fun onResults(
            results: Float,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}