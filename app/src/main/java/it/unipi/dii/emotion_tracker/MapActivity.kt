package it.unipi.dii.emotion_tracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


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


        // Imposta il punto di vista della mappa
        val mapController = map.controller
        mapController.setZoom(7.5)
        val startPoint = GeoPoint(41.8902, 12.4922)
        mapController.setCenter(startPoint)


        print_markers(myRef,map)
    }

    private fun print_markers(myRef: DatabaseReference, map: MapView) {


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
                            "city:${childData.get("city")}" +
                            "emotion:${childData.get("emotion")}"
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

class LocationCell(
    latitude: Double,
    longitude: Double,
    street: String?,
    city: String?,
    emotion: String
) {
        var latitude: Double = latitude
        var longitude: Double = longitude
        var street: String? = street
        var city : String? = city
        var emotion : String = emotion
}