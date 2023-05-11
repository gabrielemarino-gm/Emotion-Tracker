package it.unipi.dii.emotion_tracker

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import smile.clustering.DBSCAN
import java.util.*

private lateinit var listCluster: MutableList<ClusterCentroid>
class MapActivity: AppCompatActivity()
{

    private val MY_PERMISSIONS_REQUEST_LOCATION = 123
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var buttonClicked : Boolean = false

    init {
        listCluster= mutableListOf<ClusterCentroid>()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        //FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_map)

        set_fragment_ranking()

        Configuration.getInstance().userAgentValue = "it.unipi.dii.emotion_tracker"

        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("position_emotion")


        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set the starting point on the map
        setStartPosition(map)
        // val mapController = map.controller
        // mapController.setZoom(7.5)
        // val startPoint = GeoPoint(41.8902, 12.4922)
        // mapController.setCenter(startPoint)

        // This is useful for make an action when the user scrlol or zoom the map
        map.setMapListener(object: MapListener
        {
            // Handle scroll event
            override fun onScroll(event: ScrollEvent?): Boolean
            {
                println("SCROLL")
                keepPositionVisible(myRef,map)
                return true
            }
            override fun onZoom(event: ZoomEvent?): Boolean {
                println("ZOOM")
                //keepPositionVisible(myRef,map)
                return true
            }
        })

        //generateClusters(myRef, map)
    }

    private fun set_fragment_ranking() {

        val button = findViewById<Button>(R.id.ranking_button)
        val fragment_ranking=RankingFragment()
        val parentLayout=findViewById<ConstraintLayout>(R.id.map_page)
        val params=ConstraintLayout.LayoutParams((parentLayout.width * 0.8).toInt(), //width 80% of parent and height the same
            ConstraintLayout.LayoutParams.MATCH_PARENT)
        findViewById<FrameLayout>(R.id.fragment_ranking).layoutParams = params

        button.setOnClickListener {
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels

            val anim = ObjectAnimator.ofFloat(button, "x", button.x, screenWidth*0.8.toFloat())

            if(!buttonClicked) {

                buttonClicked=true

                println("fragment appear")
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {


                            val fragment = RankingFragment()
                            supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_ranking, fragment)
                                .commit()

                    }
                })
                // Start the animation
                anim.start()
            } else {
                println("fragment disappear")
                buttonClicked=false
                val anim = ObjectAnimator.ofFloat(button, "x", button.x, 0f)
                //val animFragment = ObjectAnimator.ofFloat(fragment_ranking.view, "x", button.x, 0f)
                //val animSet = AnimatorSet()
                //animSet.playTogether(anim, animFragment)

                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {


                        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_ranking)
                        fragment?.let {
                            supportFragmentManager.beginTransaction().remove(it).commit()
                        }

                    }
                })

                anim.start()
            }


        }
    }

    private fun keepPositionVisible(myRef: DatabaseReference, map: MapView){

        val currentTimeMillis = System.currentTimeMillis()
        val twoHoursAgoMillis = currentTimeMillis - (2* 60 * 60 * 1000) //it takes two hour back from the current time

        val coordinates = getActualScreenCoordinatesInterval()  //it gives a vector of four coordinates(minLat,maxLat,minLong,maxLong) which delimitate the visible screen
        println("coordinates${coordinates[0]}-${coordinates[1]}")

        val latRef=myRef.orderByChild("latitude").startAt(coordinates[0]).endAt(coordinates[1])

        //val longRef=latRef.orderByChild("longitude").startAt(coordinates[2]).endAt(coordinates[3])

        //val  timeRef= longRef.orderByChild("timestamp").startAt(twoHoursAgoMillis.toDouble()).endAt(currentTimeMillis.toDouble())

        //val timeRefs= mutableListOf<DatabaseReference>()

        latRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val timeRefs= mutableListOf<HashMap<String,Any>>()
                val longRefList= mutableListOf<DataSnapshot>()
                snapshot.children.forEach{child ->
                    val childData = child.value as HashMap<String,Any>

                    var longitude: Double = childData.get("longitude") as Double
                    if(longitude>=coordinates[2] && longitude<=coordinates[3]){
                        //so it is between the minLong and maxLong retrieved with getActualScreenCoordinatesInterval function
                        longRefList.add(child)
                    }
                }
                //here we have the data filtered by latitude and also longitude
                /*val timeRefList= mutableListOf<DataSnapshot>()

                for(element in longRefList){
                    val point=element.value as HashMap<String,Any>
                    var timeOfPoint : Long = 0
                    if(point.get("timestamp")!=null) {
                        timeOfPoint = point.get("timestamp") as Long
                    }
                    if(timeOfPoint>twoHoursAgoMillis){
                        timeRefList.add(element)
                    }
                }*/
                //now we have also the filter on timestamp, and the results on the timeRefList
                //println("listaquery-----------------------------------------")
                for (element in longRefList){
                    val timeRef=element.value as HashMap<String,Any>

                    timeRefs.add(timeRef)
                    //println(timeRef)
                }
                generateClusters(timeRefs, map)
            }

            override fun onCancelled(error: DatabaseError) {
                println("database error in retrieve this info")

            }
        })
        //return timeRefs
    }

    private fun generateClusters(myRef: MutableList<HashMap<String, Any>>, map: MapView)
    {
        // Need to retrive only the point inside the screen of the user and in given time interval.
        // Retrive point and recompte clustering each time the user move the map.

                if(myRef.isNotEmpty()) {
                    val data = mutableListOf<List<Double>>()
                    val labeledClass = mutableListOf<Double>()

                    // ( Loop through the results and do something with each one
                    for (childData in myRef) {

                        //val childData = child as HashMap<String, Any>
                        //println("elem$childData")

                        data.add(
                            listOf(
                                childData.get("latitude") as Double,
                                childData.get("longitude") as Double
                            )
                        )
                        labeledClass.add(((childData.get("emotion") as Double)))

                    }
                    // )


                    // Convert the list in array, because the class DBSCAN accept only this type
                    val dataArray = Array(data.size) { i -> data[i].toDoubleArray() }
                    val scoreArray = Array(labeledClass.size) { i -> labeledClass[i] }



                    // Create the DBSCAN model
                    val dbscan = DBSCAN.fit(dataArray, 7, 0.001)

                    // Execution of the cluster
                    val labels = dbscan.y

                    // (    Discover the points of each cluster.
                    val clusterPoints = mutableMapOf<Int, MutableList<List<Double>>>()
                    val scorePointCluster = mutableMapOf<Int, MutableList<List<Double>>>()
                    for (i in dataArray.indices) {
                        val label = labels[i]
                        val point = dataArray[i].toList()
                        val score = scoreArray[i]

                        scorePointCluster.getOrPut(label, { mutableListOf() }).add(listOf(score))
                        clusterPoints.getOrPut(label, { mutableListOf() }).add(point)
                    }
                    // )

                    val clusterList = mutableListOf<ClusterCentroid>()

                    // Find the centroid: the means of all the points inside a single cluster
                    for (i in clusterPoints.keys) {
                        // println("DBG: Cluster $i: $clusterPoints[i]")
                        var lat: Double = 0.0
                        var lon: Double = 0.0
                        var sco: Double = 0.0
                        var numberOfPointsInCluster = 0

                        for (point in clusterPoints[i]!!) {
                            lat = lat + point[0]
                            lon = lon + point[1]

                            numberOfPointsInCluster++  //count the number of points in the cluster
                        }

                        var scoreIndx: Int = 0
                        while (scoreIndx < scorePointCluster[i]!!.size) {
                            // println("DBG : scorePointCluster[i]!![scoreIndx] = ${scorePointCluster[i]!![scoreIndx]}")
                            sco = sco + scorePointCluster[i]!![scoreIndx][0]
                            scoreIndx++
                        }

                        //for (score in scorePointCluster[i]!!)
                        //{
                        //    sco = sco + score
                        //    println("DBG: $score")
                        //}

                        lat = lat / clusterPoints[i]?.size!!
                        lon = lon / clusterPoints[i]?.size!!
                        sco = sco / scorePointCluster[i]?.size!!

                        val geocoder = Geocoder(applicationContext, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lon, 1)
                        val address = addresses?.get(0)

                        val street = address?.thoroughfare
                        val city = address?.locality

                        // val currentCluster =  ClusterInfo(sco, lat, lon)
                        // clusters = clusters.plus(currentCluster)

                        //println("DBG: Cluster $i: $lat, $lon, score = $sco")

                        val timestamp = System.currentTimeMillis()

                        val cluster = ClusterCentroid(
                            lat,
                            lon,
                            street,
                            city,
                            sco,
                            numberOfPointsInCluster,
                            Date(timestamp)
                        )
                        clusterList.add(cluster)

                    }
                    listCluster=clusterList
                    print_markers(clusterList, map)
                }
    }

    private fun print_markers(clusterList: MutableList<ClusterCentroid>, map: MapView)
    {
        try
        {
            for(cluster in clusterList)
            {
                var latitude: Double = cluster.latitude
                var longitude: Double = cluster.longitude

                //println("DBG: Cluster $latitude -- $longitude")
                val marker = Marker(map)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = "lat:${cluster.latitude}\n" +
                        "long:${cluster.longitude}\n" +
                        "street:${cluster.street}\n" +
                        "city:${cluster.city}\n" +
                        "emotion:${cluster.emotion}\n" +
                        "date:${cluster.date}\n" +
                        "numberPoints:${cluster.numberOfPoints}"

                val emotion_level = cluster.emotion
                //println(emotion_level.toDouble())
                if (emotion_level > 0.50)
                {
                    //println("maggiore di 0.50")
                    val icon = BitmapFactory.decodeResource(resources, R.drawable.smile_green_face)
                    marker.icon = BitmapDrawable(resources, icon)

                }
                else
                {
                    //println("minore di 0.50")
                    val icon = BitmapFactory.decodeResource(resources, R.drawable.sad_red_face)
                    marker.icon = BitmapDrawable(resources, icon)
                }

                map.overlays.add(marker)
            }
        }
        catch (e: java.lang.Exception)
        {
            println("ERR: " + e.message)
            return
        }
    }

    private fun setStartPosition(map: MapView)
    {
        // Controllare se l'utente ha dato il permesso per avere la localizzazione
        if(checkPermission())
        {
            // Check if the GPS is active
            //if (!isLocationEnabled())
            //{
            //    // Ask to activate the GPS
            //    Toast.makeText(this, "Turn GPS on", Toast.LENGTH_SHORT).show()
            //    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            //    startActivity(intent)
            //}

            // TODO Need to be implemented
            // if (!isLocationEnabled())
            // {
            //     // If the user didn't activate the GPS, go in the home page or find a way to don't close the app
            // }

            if (isLocationEnabled())
            {
                // Controllo permessi
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }

                // We finally have access to latitude and longitude
                fusedLocationClient.lastLocation.addOnCompleteListener(this)
                {
                        task ->
                    val location: Location? = task.result

                    if (location == null) {
                        Toast.makeText(this, "Null Position", Toast.LENGTH_SHORT).show()
                        val mapController = map.controller
                        mapController.setZoom(7.5)
                        //println("DBG: " + location.latitude.toString() + "\t\t\t" + location.latitude.toString())

                        // if the location is null, the starting point of the map is the Colosseo
                        val startPoint = GeoPoint(41.89025,12.49228)
                        mapController.setCenter(startPoint)
                    }
                    else
                    {

                        val mapController = map.controller
                        mapController.setZoom(15.5)
                        //println("DBG: " + location.latitude.toString() + "\t\t\t" + location.latitude.toString())
                        val startPoint = GeoPoint(location.latitude, location.longitude)
                        mapController.setCenter(startPoint)
                    }
                }
            }
            else
            {
                // Ask to activate the GPS
                Toast.makeText(this, "Turn GPS on", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else
        {
            // Chiedere di dare i permessi per GPS
            requestPermissions()
        }
    }


    private fun isLocationEnabled(): Boolean
    {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Devono essere attivi sia il GPS che la connessione a Internet
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }


    private fun checkPermission(): Boolean
    {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }


    private fun requestPermissions()
    {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == MY_PERMISSIONS_REQUEST_LOCATION)
        {
            if (grantResults.isNotEmpty()
                &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun getActualScreenCoordinatesInterval(): DoubleArray
    {
        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)

        // Get the position of the center of the map and the current zoom scale
        val mapCenter = map.mapCenter
        val mapZoom = map.zoomLevelDouble

        // Calculate the range of coordinates
        val mapWidth = map.width
        val mapHeight = map.height
        val latSpan = mapHeight * (360.0 / (256.0 * Math.pow(2.0, mapZoom)))
        val lngSpan = mapWidth * (360.0 / (256.0 * Math.pow(
            2.0,
            mapZoom
        )) / Math.cos(Math.toRadians(mapCenter.latitude)))

        val minLat = mapCenter.latitude - latSpan / 2
        val maxLat = mapCenter.latitude + latSpan / 2
        val minLng = mapCenter.longitude - lngSpan / 2
        val maxLng = mapCenter.longitude + lngSpan / 2

        // Return the results
        return doubleArrayOf(minLat, maxLat, minLng, maxLng)
    }

    fun getData(): Any {

        return listCluster.sortedByDescending { it.emotion }
    }
}

class LocationCell(
    var latitude: Double,
    var longitude: Double,
    var street: String?,
    var city: String?,
    var emotion: Double,
    var timestamp: Long,
    var username: String
) {
}

class ClusterCentroid(
    var latitude: Double,
    var longitude: Double,
    var street: String?,
    var city: String?,
    var emotion: Double,
    var numberOfPoints: Int,
    var date: Date
){}