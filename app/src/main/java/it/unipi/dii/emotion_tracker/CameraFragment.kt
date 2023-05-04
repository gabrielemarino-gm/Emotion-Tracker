package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import it.unipi.dii.emotion_tracker.databinding.FragmentCameraBinding
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.roundToInt


class CameraFragment : Fragment(), EmotionRecognizer.ResultsListener {
    //Binding to layout objects
    private var binding: FragmentCameraBinding? = null
    //Non null reference to binding
    private val fragmentCameraBinding
        get() = binding!!

    //Thread that handles camera activity
    private lateinit var cameraExecutor: ExecutorService
    //Reference to EmotionRecognizer class
    private lateinit var emotionRecognizer: EmotionRecognizer
    //Buffer to store the Bitmap of the last frame from the camera
    private lateinit var bitmapBuffer: Bitmap
    //Reference to MlKit FaceDetection
    private val faceDetector = FaceDetection.getClient()

    //Path to database
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
    private val myRef: DatabaseReference = database.getReference("position_emotion")

    //Accumulators to compute aggregated happiness Index
    private var happinessAccumulator: Double = 0.0
    private var counter: Int = 0

    //Reference to access Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequesterStarted = false
    //Callback to be used when a new location arrives
    private lateinit var locCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        emotionRecognizer = EmotionRecognizer(
            context = requireContext(),
            resultsListener = this)

        //ask for permissions
        val cameraLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted[Manifest.permission.CAMERA]!! && isGranted[Manifest.permission.ACCESS_FINE_LOCATION]!!) {
                launchLocationRequester()
            }
            else {
                activity?.runOnUiThread{
                    Toast.makeText(requireContext(),
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        cameraLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //bind layout to Kotlin objects
        binding = FragmentCameraBinding.inflate(inflater)

        return fragmentCameraBinding.root
    }

    override fun onStart(){
        super.onStart()
        // create background thread that will execute image processing
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
        launchLocationRequester()
    }

    override fun onStop(){
        super.onStop()
        // Shut down background thread
        cameraExecutor.shutdown()
        stopLocationRequester()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()
                .also {
                    it.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
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

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun launchLocationRequester() {

        //if permissions are not granted, location requester is not started
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //TODO maybe we can remove this
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        //Build properties of the location request
        val locRequest = LocationRequest
            .Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        //Define callback
        locCallback = object : LocationCallback(){
            override fun onLocationResult(loc_result: LocationResult) {
                Log.d(TAG, "onLocationResult fired")
                if(loc_result.locations.isEmpty()){
                    return
                }

                //Get last location information
                val location: Location = loc_result.lastLocation!!
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d(TAG, "Latitude: $latitude, Longitude: $longitude")
                if (context == null){
                    return
                }
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.get(0)

                val street = address?.thoroughfare
                val city = address?.locality

                if(counter != 0){
                    //Compute aggregated happiness Index and send it to the database
                    val happinessIndex = happinessAccumulator / counter

                    happinessAccumulator = 0.0
                    counter = 0
                    Log.d("TAG", "HappinessIndex: $happinessIndex")
                    val timestamp = System.currentTimeMillis()
                    val locationCell = LocationCell(latitude, longitude, street, city, happinessIndex, timestamp)
                    myRef.push().setValue(locationCell)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locRequest,
            locCallback,
            Looper.getMainLooper()
        )

        locationRequesterStarted = true

    }

    private fun stopLocationRequester(){
        if (locationRequesterStarted){
            fusedLocationClient.removeLocationUpdates(locCallback)
        }
    }

    private fun detectFaces(bitmapBuffer: Bitmap, imageRotation: Int) {

        val inputImage = InputImage.fromBitmap(bitmapBuffer, imageRotation)

        val result = faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                // Task completed successfully

                for (face in faces){
                    //Get the bounding box of the image
                    val boundingBox = face.boundingBox

                    //Process bounding box information
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
                    if (startingPointTop + boundingBox.height() > bitmapBuffer.height){
                        height = bitmapBuffer.height - startingPointTop
                    }

                    //Create cropped image to get only the face
                    val faceCropImage = Bitmap.createBitmap(bitmapBuffer, startingPointLeft, startingPointTop,
                                                            width, height)
                    activity?.runOnUiThread {
                        val tv1 = fragmentCameraBinding.Bitmap
                        tv1.setImageBitmap(faceCropImage)
                    }

                    //Feed the deep learning model with the cropped image
                    emotionRecognizer.detect(faceCropImage, imageRotation)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Exception:", e.toString())
            }
    }

    companion object {
        private const val TAG = "Emotion-tracker"
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 123
    }

    override fun onResults(happinessValue: Float, inferenceTime: Long, imageHeight: Int, imageWidth: Int){
        // Called when the EmotionRecognizer produces results
        activity?.runOnUiThread {
            val label = fragmentCameraBinding.label
            val roundedHappinessIndex = (happinessValue * 100.0).roundToInt() / 100.0
            label.text = roundedHappinessIndex.toString()
        }

        //Update accumulators
        happinessAccumulator += happinessValue
        counter++
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(),
                "Hello, onError arrived",
                Toast.LENGTH_SHORT).show()
        }
    }
}