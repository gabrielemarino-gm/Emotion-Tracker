package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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


class CameraFragment : Fragment() {
    //Binding to layout objects
    private var binding: FragmentCameraBinding? = null
    //Non null reference to binding
    private val fragmentCameraBinding
        get() = binding!!

    //Thread that handles camera activity
    private lateinit var cameraExecutor: ExecutorService
    //Reference to EmotionRecognizer class to detect emotion in detected faces
    private lateinit var emotionRecognizer: EmotionRecognizer
    //Buffer to store the Bitmap of the last frame from the camera
    private lateinit var bitmapBuffer: Bitmap
    //Reference to MlKit FaceDetection to detect faces
    private val faceDetector = FaceDetection.getClient()

    //Path to database
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
    private val myRef: DatabaseReference = database.getReference("position_emotion")
    //Username of the current user
    private lateinit var username: String

    //Accumulators to compute aggregated happiness Index
    private var happinessAccumulator: Double = 0.0
    private var counter: Int = 0

    //variables to implement sliding window to show on screen
    private var windowArray: ArrayList<Float> = ArrayList<Float>()

    //Reference to access Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequesterStarted = false
    //Callback to be used when a new location arrives
    private lateinit var locCallback: LocationCallback
    private var firstCall = true

    companion object {
        private const val TAG = "CameraFragment"
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 123
        // Size of the sliding window
        private const val windowSize = 30
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        emotionRecognizer = EmotionRecognizer(
            context = requireContext(),
            resultsListener = this)

        //Retrieve username of the logged user
        val prefs = requireContext().getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        username = prefs.getString("username", "")!!

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
        // Wanted behaviour is that keyboard popup will overlap the fragment
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
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

            // Manage rotation of the screen
            val windowManager = requireActivity().windowManager
            val rotation = windowManager.defaultDisplay.rotation

            // Preview to show on screen
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
                }

            // Workflow to apply for each frame detected
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Discard frames until the processing of the previous one is not completed
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

                        // bitmap buffer contains the captured image
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
            // Needed for fusedLocationClient to work
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
            return
        }

        // Useful for discarding the first sample, which will contain a low number of frames
        firstCall = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        //Build properties of the location request
        val locRequest = LocationRequest
            .Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        //Define callback to be called every 10 seconds
        locCallback = object : LocationCallback(){
            override fun onLocationResult(loc_result: LocationResult) {
                // The callback gets the average happiness index from the past 10 seconds and the current location of the user and sends it to the database
                Log.d(TAG, "onLocationResult fired")
                if(loc_result.locations.isEmpty()){
                    return
                }

                //Get last location information
                val location: Location = loc_result.lastLocation!!
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d(TAG, "Latitude: $latitude, Longitude: $longitude")

                // Check if context is null
                if (context == null){
                    return
                }

                // get information about current location
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.get(0)

                val street = address?.thoroughfare
                val city = address?.locality

                if(counter != 0 && !firstCall){
                    // Compute aggregated happiness Index and send it to the database
                    val happinessIndex = happinessAccumulator / counter

                    // Reset accumulators
                    happinessAccumulator = 0.0
                    counter = 0

                    // Happiness index that will be sent to the database
                    Log.d("TAG", "HappinessIndex: $happinessIndex")
                    val timestamp = System.currentTimeMillis()

                    val locationCell = LocationCell(latitude, longitude, street, city, happinessIndex, timestamp, username)
                    myRef.push().setValue(locationCell)
                }
                else if(firstCall){
                    firstCall = false
                }
            }
        }


        // Activate the location request
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

        // Detect faces in the current frame (bitmapBuffer)
        val inputImage = InputImage.fromBitmap(bitmapBuffer, imageRotation)

        val result = faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                // Task completed successfully, faces contain the list of bounding boxes enclosing faces

                for (face in faces){
                    // Get the bounding box of the image
                    val boundingBox = face.boundingBox

                    // Rotate bitmap according to imageRotation
                    val rotatedBitmap = rotateBitmap(bitmapBuffer, imageRotation.toFloat())

                    // Process bounding box information
                    var startingPointLeft = boundingBox.left
                    var width = boundingBox.width()
                    if (boundingBox.left < 0){
                        startingPointLeft = 0
                    }
                    if (boundingBox.width() + startingPointLeft > rotatedBitmap.width){
                        width = rotatedBitmap.width - startingPointLeft
                    }

                    var startingPointTop = boundingBox.top
                    var height = boundingBox.height()
                    if (boundingBox.top < 0){
                        startingPointTop = 0
                    }
                    if (startingPointTop + boundingBox.height() > rotatedBitmap.height){
                        height = rotatedBitmap.height - startingPointTop
                    }

                    //Create cropped image to get only the face
                    val faceCropImage = Bitmap.createBitmap(rotatedBitmap, startingPointLeft, startingPointTop,
                                                            width, height)


                    //Feed the deep learning model with the cropped image
                    emotionRecognizer.detect(faceCropImage)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Exception:", e.toString())
            }
    }

    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        // Rotate the source bitmap of angle degrees
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun onResults(happinessValue: Float){
        // Called when the EmotionRecognizer produces results

        //Update accumulators for sending them to the database later
        happinessAccumulator += happinessValue
        counter++

        if(windowArray.size >= windowSize){
            // remove the oldest element from the window
            windowArray.removeFirst()
        }

        windowArray.add(happinessValue)

        // Average happiness Index in the window
        val happinessIndex = windowArray.sum() / windowArray.size

        var imageHappinessPath = 0

        when {
            happinessIndex < 0.1 -> imageHappinessPath = R.drawable.happy_level1
            happinessIndex >= 0.1 && happinessIndex < 0.5 -> imageHappinessPath = R.drawable.happy_level2
            happinessIndex >= 0.5 && happinessIndex < 0.75 -> imageHappinessPath = R.drawable.happy_level3
            happinessIndex >= 0.75 -> imageHappinessPath = R.drawable.happy_level4
        }

        activity?.runOnUiThread {
            // Show prediction on screen and related face image
            val tv1 = fragmentCameraBinding.faceView
            tv1.setImageResource(imageHappinessPath)
            val label = fragmentCameraBinding.label
            val roundedHappinessIndex = (happinessIndex * 100.0).roundToInt() / 100.0
            label.text = roundedHappinessIndex.toString()
        }
    }

    fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(),
                error,
                Toast.LENGTH_SHORT).show()
        }
    }
}