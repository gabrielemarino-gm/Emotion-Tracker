package it.unipi.dii.emotion_tracker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity()
{
    lateinit var toggle: ActionBarDrawerToggle

    private lateinit var sharedPref: SharedPreferences
    private var gpsMessageShown: Boolean = false
    private var isRotated: Boolean = false



    companion object
    {
        private const val gpsMessageShown_KEY = "gpsMessageShown"
        private const val  appPreferences_KEY = "appPreferences"
        private const val isRotated_KEY = "isRotated"
        private const val gpsIsOn_KEY = "GPS is ON"
        private const val gpsIsOFF_KEY = "GPS is OFF"
        private var serviceAlreadyStart: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // val trialButton = findViewById<Button>(R.id.btn_toCamera)
        // val mapButton = findViewById<Button>(R.id.btn_toMap)
        // val logoutButton =findViewById<Button>(R.id.btn_logout)

// (    START CLUSTER SERVICE
        println("DBG: serviceAlreadyStart = $serviceAlreadyStart")
        // I Need this control because otherwise the application recall the service each time the user move in the Home page
        if (!serviceAlreadyStart)
        {
            println("DBG: Starting service")
            startService(Intent(this@MainActivity, ClusterService::class.java))
            serviceAlreadyStart = true
        }
// )

        sharedPref = getSharedPreferences(appPreferences_KEY, Context.MODE_PRIVATE)
        // Check if GPS status has already been displayed
        gpsMessageShown = sharedPref.getBoolean(gpsMessageShown_KEY, false)
        isRotated = savedInstanceState?.getBoolean(isRotated_KEY, false) ?: false

        if (!gpsMessageShown && !isRotated)
        {
            // Check GPS status
            checkGpsStatus()

            // Update flag in SharedPreferences
            sharedPref.edit().putBoolean(gpsMessageShown_KEY, true).apply()
        }


// (    IS USER LOGGED?
//      Necessary to know if the user is logged in,
//      if he makes logout and then presses the button back he would enter in this page (not correct behaviour)
        val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        if(token == null)
        {
            // the user is not logged
            val loginPage = Intent(this, LoginActivity::class.java)
            startActivity(loginPage)
            finish()
        }
// )

// MENU
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
                R.id.nav_home -> Toast.makeText(applicationContext,"Already in Home", Toast.LENGTH_SHORT).show()

                // On Click over the menu's Map Button
                R.id.nav_map -> {
                    if(isLocationEnabled())
                    {
                        val mapPage = Intent(this, MapActivity::class.java)
                        startActivity(mapPage)
                    }
                    else
                    {
                        // Ask to activate the GPS
                        Toast.makeText(this, "Turn GPS on", Toast.LENGTH_SHORT).show()
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                }

                // On Click over the menu's Account Button
                R.id.nav_account -> {
                    val accountPage = Intent(this, AccountActivity::class.java)
                    startActivity(accountPage)
                }

                // On Click over the menu's Logout Button
                R.id.nav_logout -> {
                    //remove token from sharedPreferences
                    val prefs_logout = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
                    val editor = prefs_logout.edit()
                    editor.remove("token")
                    editor.apply()

                    serviceAlreadyStart=false

                    val loginPage = Intent(this, LoginActivity::class.java)
                    startActivity(loginPage)
                    finish()
                }
            }
            true
        }
// )
    }

    override fun onDestroy()
    {
        super.onDestroy()

        // Reset GPS message flag if the app is destroyed
        if (!isChangingConfigurations) {
            sharedPref.edit().putBoolean(gpsMessageShown_KEY, false).apply()
        }

// (    STOP CLUSTER SERVICE
        stopService(Intent(this@MainActivity, ClusterService::class.java))
// )
    }

    private fun checkGpsStatus() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGpsEnabled) {
            // GPS is enabled
            Toast.makeText(this, gpsIsOn_KEY, Toast.LENGTH_SHORT).show()
        } else {
            // GPS is disabled
            Toast.makeText(this, gpsIsOFF_KEY, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save flag to indicate if the phone has been rotated
        outState.putBoolean(isRotated_KEY, true)
    }


    private fun isLocationEnabled(): Boolean
    {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Both GPS and Internet connection must be active
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if(toggle.onOptionsItemSelected(item))
        {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}