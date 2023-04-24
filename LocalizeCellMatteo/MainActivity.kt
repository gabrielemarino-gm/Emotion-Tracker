package com.example.localizecell

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


class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_LOCATION = 123


    override fun onCreate(savedInstanceState: Bundle?) 
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Configuration.getInstance().userAgentValue = "com.example.localizecell"


        val map = findViewById<org.osmdroid.views.MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
        {
            println("GPS provider is not enabled")
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) 
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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

                /*val lat = location.latitude
                val lon = location.longitude
                val geoPoint = GeoPoint(lat, lon)
                val geocoder = GeocoderNominatim(Locale.getDefault(), "MyUserAgent")
                val addresses = geocoder.getFromLocation(geoPoint, 1)

                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val street = address.thoroughfare
                    val city = address.locality
                    val country = address.countryName

                    // Do something with the address information
                } else {
                    // No address found
                }*/

                val geocoder = Geocoder(applicationContext, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.get(0)

                val street = address?.thoroughfare
                val city = address?.locality


                println("lat:${latitude}\nlong:${longitude}\nstreet:${street}\ncity:${city}")

                val marker = Marker(map)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = "lat:${latitude}\n" +
                        "long:${longitude}\n" +
                        " street:${street}\n" +
                        "city:${city}"
                map.overlays.add(marker)
            }

            override fun onProviderDisabled(provider: String) {}

            override fun onProviderEnabled(provider: String) {
                    println("provider enabled function")
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

        // locationManager.removeUpdates(locationListener)
    }
}

