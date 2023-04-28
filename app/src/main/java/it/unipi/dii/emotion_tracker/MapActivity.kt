package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*


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
        val myRef: DatabaseReference = database.getReference("positions")

        //myRef.setValue("Hello, World!")

        //println("firebase db")

        var position_obtained: Int = 0

        //println("ciclo ${position_obtained}")


        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)


        // Imposta il punto di vista della mappa
        val mapController = map.controller
        mapController.setZoom(7.5)
        val startPoint = GeoPoint(41.8902, 12.4922)
        mapController.setCenter(startPoint)



        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locRequest=LocationRequest.create()
        locRequest.setInterval(10000)
        locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val locCallback=object : LocationCallback(){
            override fun onLocationResult(loc_result: LocationResult) {
                if(loc_result==null){
                    return;
                }
                //for(location: Location in loc_result.locations){
                if(position_obtained==0) {
                    var location: Location = loc_result.locations[0]  //to take only the first one
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("TAG", "Latitude: $latitude, Longitude: $longitude")
                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.get(0)

                    val street = address?.thoroughfare
                    val city = address?.locality


                    // println("lat:${latitude}\nlong:${longitude}\nstreet:${street}\ncity:${city}")

                    val location_cell = LocationCell(latitude, longitude, street, city)
                    myRef.push().setValue(location_cell)

                    position_obtained = 1

                    //println("scrittura ${position_obtained}")

                    /*val marker = Marker(map)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.title = "lat:${latitude}\n" +
                            "long:${longitude}\n" +
                            "street:${street}\n" +
                            "city:${city}"
                    map.overlays.add(marker)*/

                }
                //}
            }
        }


        fusedLocationClient.requestLocationUpdates(
            locRequest,
            locCallback,
            Looper.getMainLooper()
        )

        print_markers(myRef,map)
    }

    private fun print_markers(myRef: DatabaseReference, map: MapView) {

        //TODO it does not work for the moment
        myRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                // Loop through the results and do something with each one
                snapshot.children.forEach { child ->
                    val childData = child.value as HashMap<String,Any>
                    //println(childData!!::class.simpleName)
                    //println(childData)
                    //var childD=HashMap<String, Int>()
                    //childD= childData as HashMap<String, Int>
                    println(childData.get("latitude"))
                    //val jsonData= Json.decodeFromString<LocationCell>(childData.toString())
                    var latitude: Double = childData.get("latitude") as Double
                    var longitude: Double = childData.get("longitude") as Double
                    val marker = Marker(map)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.title = "lat:${childData.get("latitude")}\n" +
                            "long:${childData.get("longitude")}\n" +
                            "street:${childData.get("street")}\n" +
                            "city:${childData.get("city")}"
                    map.overlays.add(marker)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error case
                println("error in retrieving position")
            }
        })
    }
}

class LocationCell(latitude: Double, longitude: Double, street: String?, city: String?) {
        var latitude: Double = latitude
        var longitude: Double = longitude
        var street: String? = street
        var city : String? = city
}