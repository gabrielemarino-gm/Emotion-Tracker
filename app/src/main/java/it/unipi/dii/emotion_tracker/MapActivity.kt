package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import smile.clustering.DBSCAN


class MapActivity: AppCompatActivity()
{
    private val MY_PERMISSIONS_REQUEST_LOCATION = 123
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        //FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_map)

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
        //map.setMapListener(object: MapListener
        //{
        //    // Handle scroll event
        //    override fun onScroll(event: ScrollEvent?): Boolean
        //    {
        //        println("SCROLL")
        //        return true
        //    }
        //    override fun onZoom(event: ZoomEvent?): Boolean {
        //        TODO("Not yet implemented")
        //    }
        //})

        print_markers(myRef, map)
    }

    private fun print_markers(myRef: DatabaseReference, map: MapView)
    {
        // Need to retrive only the point inside the screen of the user and in given time interval.
        // Retrive point and recompte clustering each time the user move the map.
        myRef.addListenerForSingleValueEvent(
            object : ValueEventListener
            {
                override fun onDataChange(snapshot: DataSnapshot)
                {
                    val data = mutableListOf<List<Double>>()
                    val labeledClass = mutableListOf<Double>()

                    // ( Loop through the results and do something with each one
                    snapshot.children.forEach{ child ->

                        val childData = child.value as HashMap<String, Any>

                        data.add(listOf(childData.get("latitude") as Double, childData.get("longitude") as Double))
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
                    for (i in dataArray.indices)
                    {
                        val label = labels[i]
                        val point = dataArray[i].toList()
                        val score = scoreArray[i]

                        scorePointCluster.getOrPut(label, { mutableListOf() }).add(listOf(score))
                        clusterPoints.getOrPut(label, { mutableListOf() }).add(point)
                    }
                    // )


                    // Find the centroid: the means of all the points inside a single cluster
                    for (i in clusterPoints.keys)
                    {
                        // println("DBG: Cluster $i: $clusterPoints[i]")
                        var lat: Double = 0.0
                        var lon: Double = 0.0
                        var sco: Double = 0.0

                        for(point in clusterPoints[i]!!)
                        {
                            lat = lat + point[0]
                            lon = lon + point[1]
                        }

                        var scoreIndx: Int = 0
                        while(scoreIndx < scorePointCluster[i]!!.size)
                        {
                            // println("DBG : scorePointCluster[i]!![scoreIndx] = ${scorePointCluster[i]!![scoreIndx]}")
                            sco = sco + scorePointCluster[i]!![scoreIndx][0]
                            scoreIndx++
                        }

                        //for (score in scorePointCluster[i]!!)
                        //{
                        //    sco = sco + score
                        //    println("DBG: $score")
                        //}

                        lat = lat/ clusterPoints[i]?.size!!
                        lon = lon/clusterPoints[i]?.size!!
                        sco = sco/scorePointCluster[i]?.size!!

                        // val currentCluster =  ClusterInfo(sco, lat, lon)
                        // clusters = clusters.plus(currentCluster)

                        // println("DBG: Cluster $i: $lat, $lon, score = $sco")


                        // I found the centroid of the cluster, I can print the mark
                        var marker = Marker(map)
                        marker.position = GeoPoint(lat, lon)
                        marker.title = "Latitude:${lat}\n" +
                                "Longitude:${lon}\n" +
                                "Mean Emotion:${sco}"

                        if(sco > 0.50)
                        {
                            val icon = BitmapFactory.decodeResource(resources, R.drawable.smile_green_face)
                            marker.icon = BitmapDrawable(resources, icon)

                        }
                        else
                        {
                            val icon = BitmapFactory.decodeResource(resources, R.drawable.sad_red_face)
                            marker.icon = BitmapDrawable(resources, icon)
                        }
                        map.overlays.add(marker)

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error case
                    println("error in retrieving position")
                }
            }
        )
    }

//    private fun print_markers(myRef: DatabaseReference, map: MapView) {
//
//        myRef.addListenerForSingleValueEvent(object : ValueEventListener{
//            override fun onDataChange(snapshot: DataSnapshot) {
//                // Loop through the results and do something with each one
//                snapshot.children.forEach { child ->
//                    val childData = child.value as HashMap<String,Any>
//                    //println(childData!!::class.simpleName)
//                    //println(childData)
//                    //var childD=HashMap<String, Int>()
//                    //childD= childData as HashMap<String, Int>
//                    //println(childData.get("latitude"))
//                    //val jsonData= Json.decodeFromString<LocationCell>(childData.toString())
//                    var latitude: Double = childData.get("latitude") as Double
//                    var longitude: Double = childData.get("longitude") as Double
//                    val marker = Marker(map)
//                    marker.position = GeoPoint(latitude, longitude)
//                    marker.title = "lat:${childData.get("latitude")}\n" +
//                            "long:${childData.get("longitude")}\n" +
//                            "street:${childData.get("street")}\n" +
//                            "city:${childData.get("city")}\n" +
//                            "emotion:${childData.get("emotion")}"
//
//                    val emotion_level = childData.get("emotion").toString()
//                    //println(emotion_level.toDouble())
//                    if(emotion_level.toDouble() >0.50){
//                        //println("maggiore di 0.50")
//                        val icon = BitmapFactory.decodeResource(resources, R.drawable.smile_green_face)
//                        marker.icon = BitmapDrawable(resources, icon)
//
//                    }
//                    else{
//                        //println("minore di 0.50")
//                        val icon = BitmapFactory.decodeResource(resources, R.drawable.sad_red_face)
//                        marker.icon = BitmapDrawable(resources, icon)
//                    }
//                    map.overlays.add(marker)
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                // Handle error case
//                println("error in retrieving position")
//            }
//        })
//    }

    private fun setStartPosition(map: MapView)
    {
        // Controllare se l'utente ha dato il permesso per avere la localizzazione
        if(checkPermission())
        {
            // Controllare il GPS è attivo
            if(isLocationEnabled())
            {
                // Controllo permessi
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions()
                    return
                }


                // Abbiamo finalmente accesso a latitudine e longitudine
                fusedLocationClient.lastLocation.addOnCompleteListener(this)
                {
                        task ->
                    val location: Location?= task.result
                    if(location == null)
                    {
                        Toast.makeText(this, "Posizione nulla", Toast.LENGTH_SHORT).show()
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
                // Chiedere di attivare il GPS
                Toast.makeText(this, "Accendere il GPS", Toast.LENGTH_SHORT).show()
                val intnet = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
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
}

class LocationCell(
    latitude: Double,
    longitude: Double,
    street: String?,
    city: String?,
    emotion: Double
) {
    var latitude: Double = latitude
    var longitude: Double = longitude
    var street: String? = street
    var city : String? = city
    var emotion : Double = emotion
}