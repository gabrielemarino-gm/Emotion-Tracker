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
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat.isLocationEnabled
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import smile.clustering.DBSCAN
import java.util.*

private lateinit var listCluster: MutableList<ClusterCentroid>
private lateinit var listPrintedCluster: MutableList<ClusterCentroid>
class MapActivity: AppCompatActivity()
{
    private val MY_PERMISSIONS_REQUEST_LOCATION = 123
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var buttonClicked : Boolean = false
    lateinit var toggle: ActionBarDrawerToggle

    init {
        listCluster = mutableListOf<ClusterCentroid>()
        listPrintedCluster = mutableListOf<ClusterCentroid>()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        //FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_map)

        setFragmentRanking()

        Configuration.getInstance().userAgentValue = "it.unipi.dii.emotion_tracker"

        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("position_emotion")


        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set the starting point on the map
        setStartPosition(map)
        // generateClusters(myRef,map)

        // Retrieve username of the logged user
        val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        val username = prefs.getString("username", "")!!
        val clusterRef: DatabaseReference = database.getReference("clusters_${username}")


        // This is useful for make an action when the user scrlol or zoom the map
        map.setMapListener(object: MapListener
        {
            // Handle scroll event
            override fun onScroll(event: ScrollEvent?): Boolean
            {
                println("SCROLL")
                keepPositionVisible(clusterRef,map)
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                println("ZOOM")
                //keepPositionVisible(myRef,map)
                return true
            }
        })

        //keepPositionVisible(clusterRef,map)
        //generateClusters(myRef, map)

// (    MENU
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navView.setNavigationItemSelectedListener {
            when(it.itemId)
            {
                // On Click over the menu's Home Button
                R.id.nav_home -> {
                    val trialPage = Intent(this, MainActivity::class.java)
                    startActivity(trialPage)
                }

                // On Click over the menu's Map Button
                R.id.nav_map -> Toast.makeText(applicationContext,"Already in Map", Toast.LENGTH_SHORT).show()

                // On Click over the menu's Account Button
                R.id.nav_account -> {
                    val accountPage = Intent(this, AccountActivity::class.java)
                    startActivity(accountPage)
                }

                // On Click over the menu's Logout Button
                R.id.nav_logout -> {
                    //remove token from sharedPreferences
                    val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.remove("token")
                    editor.apply()

                    val loginPage = Intent(this, LoginActivity::class.java)
                    startActivity(loginPage)
                    finish()
                }
            }
            true
        }
// )
    }

    private fun setFragmentRanking()
    {
        val button = findViewById<Button>(R.id.ranking_button)
        val fragment_ranking=RankingFragment()
        val parentLayout=findViewById<ConstraintLayout>(R.id.map_page)
        val params=ConstraintLayout.LayoutParams((parentLayout.width * 0.8).toInt(), //width 80% of parent and height the same
            ConstraintLayout.LayoutParams.MATCH_PARENT)
        findViewById<FrameLayout>(R.id.fragment_ranking).layoutParams = params

        button.setOnClickListener {
            val screenWidth = Resources.getSystem().displayMetrics.widthPixels

            val anim = ObjectAnimator.ofFloat(button, "x", button.x, screenWidth*0.8.toFloat())

            if(!buttonClicked)
            {
                buttonClicked = true

                // println("fragment appear")
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
            }
            else
            {
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

    private fun keepPositionVisible(myRef: DatabaseReference, map: MapView)
    {
        val currentTimeMillis = System.currentTimeMillis()
        val twoHoursAgoMillis = currentTimeMillis - (2* 60 * 60 * 1000) //it takes two hour back from the current time

        val coordinates = getActualScreenCoordinatesInterval()  //it gives a vector of four coordinates(minLat,maxLat,minLong,maxLong) which delimitate the visible screen
        //println("coordinates${coordinates[0]}-${coordinates[1]}")

        val latRef=myRef.orderByChild("latitude").startAt(coordinates[0]).endAt(coordinates[1])

        latRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val timeRefs= mutableListOf<HashMap<String,Any>>()
                val clusterList = mutableListOf<ClusterCentroid>()
                val longRefList= mutableListOf<DataSnapshot>()
                snapshot.children.forEach{child ->
                    val childData = child.value as HashMap<String,Any>

                    var longitude: Double = childData.get("longitude") as Double
                    if(longitude>=coordinates[2] && longitude<=coordinates[3]){
                        //so it is between the minLong and maxLong retrieved with getActualScreenCoordinatesInterval function
                        longRefList.add(child)
                    }
                }

                //now we have also the filter on timestamp, and the results on the timeRefList
                //println("listaquery-----------------------------------------")
                for (element in longRefList){
                    val timeRef=element.value as HashMap<String,Any>

                    println("DBG: element = ${timeRef}")
                    val cluster = ClusterCentroid(
                        timeRef.get("latitude") as Double,
                        timeRef.get("longitude") as Double,
                        timeRef.get("street") as String,
                        timeRef.get("city") as String,
                        timeRef.get("emotion") as Double,
                        timeRef.get("numberOfPoints") as Long,
                        timeRef.get("timestampDate") as Long
                    )
                    clusterList.add(cluster)
                    timeRefs.add(timeRef)
                }
                //println("size of list"+clusterList.size)
                printMarkers(clusterList,map)
            }

            override fun onCancelled(error: DatabaseError) {
                println("database error in retrieve this info")

            }
        })
        //return timeRefs
    }

    private fun printMarkers(clusterList: MutableList<ClusterCentroid>, map: MapView)
    {
        try
        {
            //println("printing markers")
            listCluster = clusterList
            for(cluster in clusterList)
            {
                //println("clusterization$cluster")
                //println("size of list$listPrintedCluster")
                if (clusterNotPrinted(cluster)) {
                    val latitude: Double = cluster.latitude
                    val longitude: Double = cluster.longitude

                    //println("DBG: Cluster $latitude -- $longitude")
                    val marker = Marker(map)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.title = "lat:${cluster.latitude}\n" +
                            "long:${cluster.longitude}\n" +
                            "street:${cluster.street}\n" +
                            "city:${cluster.city}\n" +
                            "emotion:${cluster.emotion}\n" +
                            "date:${Date(cluster.timestampDate)}\n" +
                            "numberPoints:${cluster.numberOfPoints}"

                    val emotion_level = cluster.emotion
                    //println(emotion_level.toDouble())
                    var icon = BitmapFactory.decodeResource(resources, R.drawable.smile_green_face)
                    when {
                        emotion_level < 0.25 -> icon = BitmapFactory.decodeResource(resources, R.drawable.happy_level1)
                        emotion_level >= 0.25 && emotion_level < 0.5 -> icon = BitmapFactory.decodeResource(resources, R.drawable.happy_level2)
                        emotion_level >= 0.5 && emotion_level < 0.75 -> icon = BitmapFactory.decodeResource(resources, R.drawable.happy_level3)
                        emotion_level >= 0.75 -> icon = BitmapFactory.decodeResource(resources, R.drawable.happy_level4)
                    }

                    marker.icon = BitmapDrawable(resources, icon)
                    map.overlays.add(marker)
                    //println("printed markers")
                    map.invalidate()
                    listPrintedCluster.add(cluster) //added cluster to the list of the cluster already printed on the map
                }
            }
        }
        catch (e: java.lang.Exception)
        {
            println("ERR: " + e.message)
            return
        }
    }

    private fun clusterNotPrinted(cluster: ClusterCentroid): Boolean {
            for(element in listPrintedCluster){
                if(cluster.latitude==element.latitude && cluster.longitude==element.longitude){
                        return false  //it is already printed, it should not be printed
                }
            }
            return true  //is not printed, it can go with the printing
    }

    private fun setStartPosition(map: MapView)
    {
        // Check if the user has given permission to have the location
        if(checkPermission())
        {
            // Check if the GPS is active
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

                        // if the location is null, the starting point of the map is the Colosseo
                        val startPoint = GeoPoint(41.89025,12.49228)
                        mapController.setCenter(startPoint)
                    }
                    else
                    {

                        val mapController = map.controller
                        mapController.setZoom(15.5)
                        // println("DBG: " + location.latitude.toString() + "\t\t\t" + location.latitude.toString())
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
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item))
        {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}