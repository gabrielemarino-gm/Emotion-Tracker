package it.unipi.dii.emotion_tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.*

class MapActivity: AppCompatActivity()
{
    private val MY_PERMISSIONS_REQUEST_LOCATION = 123


    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().userAgentValue = "com.example.localizecell"


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

        val locationListener = object : LocationListener
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

        // locationManager.removeUpdates(locationListener)
    }
}