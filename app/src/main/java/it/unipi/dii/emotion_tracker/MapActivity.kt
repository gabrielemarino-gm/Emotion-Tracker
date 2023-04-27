package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
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

        println("firebase db")


        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)


        // Imposta il punto di vista della mappa
        val mapController = map.controller
        mapController.setZoom(7.5)
        val startPoint = GeoPoint(41.8902, 12.4922)
        mapController.setCenter(startPoint)

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            println("GPS provider is not enabled")
        }

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

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations, this can be null.
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    Log.d("TAG", "Latitude: $latitude, Longitude: $longitude")
                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    val address = addresses?.get(0)

                    val street = address?.thoroughfare
                    val city = address?.locality


                    // println("lat:${latitude}\nlong:${longitude}\nstreet:${street}\ncity:${city}")

                    val location_cell=LocationCell(latitude,longitude,street,city)
                    myRef.push().setValue(location_cell)

                    val marker = Marker(map)
                    marker.position = GeoPoint(latitude, longitude)
                    marker.title = "lat:${latitude}\n" +
                            "long:${longitude}\n" +
                            "street:${street}\n" +
                            "city:${city}"
                    map.overlays.add(marker)

                } else {
                    Log.d("TAG", "No location found")
                }
            }
            .addOnFailureListener { e ->
                Log.d("TAG", "Error getting location: ${e.message}")
            }

        /*val locationListener = object : LocationListener
        {
            override fun onLocationChanged(location: Location)
            {
                val latitude = location.latitude
                val longitude = location.longitude


                val geocoder = Geocoder(applicationContext, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.get(0)

                val street = address?.thoroughfare
                val city = address?.locality


                // println("lat:${latitude}\nlong:${longitude}\nstreet:${street}\ncity:${city}")

                val location_cell=LocationCell(latitude,longitude,street,city)
                myRef.push().setValue(location_cell)

                // delete the older mark into the map
                val oldMarker = map.overlays.firstOrNull { it is Marker } as Marker?
                if (oldMarker != null) {
                    map.overlays.remove(oldMarker)
                }

                val marker = Marker(map)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = "lat:${latitude}\n" +
                        "long:${longitude}\n" +
                        "street:${street}\n" +
                        "city:${city}"
                map.overlays.add(marker)
            }

            override fun onProviderDisabled(provider: String) {}

            override fun onProviderEnabled(provider: String)
            {
                println("provider enabled function")
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

        // locationManager.removeUpdates(locationListener)*/
    }
}

class LocationCell(latitude: Double, longitude: Double, street: String?, city: String?) {
        var latitude: Double = latitude
        var longitude: Double = longitude
        var street: String? = street
        var city : String? = city
}