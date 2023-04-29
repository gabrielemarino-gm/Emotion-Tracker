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


class EmotionRecognizer(
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

        //image preprocessing workflow
        val imageProcessor = ImageProcessor.Builder().add(Rot90Op(-imageRotation / 90)) //rotate the image
                                                     .add(ResizeOp(48,48, null)) //resize to 48x48
                                                     .add(TransformToGrayscaleOp()) // transform to grayscale
                                                     .add(CastOp(DataType.FLOAT32)) // cast to float value
                                                     .build()

        // Preprocess the image and convert it into a TensorImage for detection.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        // Obtain the model
        val modelPath = FileUtil.loadMappedFile(context, "CNN_3_with_metadata_2.tflite")
        val model = InterpreterApi.create(modelPath, InterpreterApi.Options().setRuntime(InterpreterApi.Options.TfLiteRuntime.PREFER_SYSTEM_OVER_APPLICATION))

        // Use the model and then close it
        val results = TensorBuffer.createFixedSize(intArrayOf(1, 1), DataType.FLOAT32)
        model.run(tensorImage.buffer, results.buffer)
        model.close()
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        // send results
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