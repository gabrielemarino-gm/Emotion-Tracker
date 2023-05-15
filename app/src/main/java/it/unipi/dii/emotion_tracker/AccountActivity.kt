package it.unipi.dii.emotion_tracker


import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.util.Date
import kotlin.math.roundToInt

class AccountActivity : AppCompatActivity()
{
    private var changePasswordButton: Button? = null
    private lateinit var username: String
    private var dateOfBirth: String = ""
    private var happinessIndex: Double = 0.0
    private lateinit var lastLocations: List<Location>
    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        //Retrieve username of the logged user
        val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        username = prefs.getString("username", "")!!

        retrieveDataFromDB()

        changePasswordButton = findViewById(R.id.change_password_button)
        changePasswordButton?.setOnClickListener(){
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            val changePasswordFragment = ChangePasswordFragment(this, username)
            transaction.add(R.id.change_password_container, changePasswordFragment, "change_password")
            transaction.addToBackStack(null)
            transaction.setReorderingAllowed(true)
            transaction.commit()
        }

        //If fragment already exists, retrieve it and update its parent activity (rotation-related issue)
        if(supportFragmentManager.findFragmentByTag("change_password") != null){
            val changePasswordFragment: ChangePasswordFragment? = supportFragmentManager.findFragmentByTag("change_password") as ChangePasswordFragment?
            changePasswordFragment?.changeParentActivity(this)
            //Also set button to change password as invisible
            changePasswordButton?.visibility = INVISIBLE
        }

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
                R.id.nav_account -> Toast.makeText(applicationContext,"Already in Account", Toast.LENGTH_SHORT).show()


                // On Click over the menu's Logout Button
                R.id.nav_logout -> {
                    //remove token from sharedPreferences
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

    private fun retrieveDataFromDB() {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val posRef: DatabaseReference = database.getReference("position_emotion")

        // retrieve all the position_emotion records belonging to logged user
        val userRef = posRef.orderByChild("username").equalTo(username)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var posUserList= mutableListOf<LocationCell>()

                var happinessAccumulator = 0.0

                snapshot.children.forEach{child ->
                    val childData = child.value as java.util.HashMap<String, Any>

                    val positionRecord=LocationCell(
                        childData["latitude"] as Double,
                        childData["longitude"] as Double,childData["street"] as String,childData["city"] as String,childData["emotion"] as Double,
                        childData["timestamp"] as Long,childData["username"] as String)

                    // increase happiness accumulator
                    happinessAccumulator += positionRecord.emotion
                    posUserList.add(positionRecord)
                }

                if (posUserList.isEmpty()){
                    // no locations were recorded for this users, profile is empty
                    inflateProfile(0.0, listOf())
                    return
                }
                posUserList = posUserList.sortedByDescending { it.timestamp } as MutableList<LocationCell>

                val happinessMean = happinessAccumulator / posUserList.size

                // now we want to obtain the 5 latest location

                // list that will contain the latest locations
                val lastLocationsList= mutableListOf<LocationCell>()
                lastLocationsList.add(posUserList[0])

                // actual number of elements in the list
                var numElements = 1
                var i = 1

                // we scan the list until the end, or until we've got 5 locations
                while(i < posUserList.size && numElements < 5){
                    val nextLocation = posUserList.get(i)
                    if (nextLocation.city.equals(lastLocationsList.last().city) &&
                        nextLocation.street.equals(lastLocationsList.last().street)){
                        // location is the same as before, skip it
                    }
                    else{
                        // add location to the latest locations
                        lastLocationsList.add(nextLocation)
                        numElements++
                    }
                    i++
                }

                //list that will contain the relevant information about the latest locations
                val listToReturn : ArrayList<Location> = ArrayList(numElements)

                for (j in 0 until numElements){
                    // inflate listToReturn
                    val date = Date(posUserList[j].timestamp) // create a new Date object from the timestamp
                    val format = getDateInstance(DateFormat.DEFAULT) // create a date format
                    val dateString = format.format(date) // format the date as a string
                    listToReturn.add(Location(lastLocationsList[j].street, lastLocationsList[j].city, dateString))
                }

                // inflate profile with the retrieved information
                inflateProfile(happinessMean,listToReturn)

            }

            override fun onCancelled(error: DatabaseError) {
                println("database error in retrieving this info")
            }
        })
    }

    private fun inflateProfile(happinessMean: Double, posList: List<Location>) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        // get date of birth
        myRef.get()
            .addOnSuccessListener { documents ->
                documents.children.forEach { child ->
                    val childData = child.value as HashMap<String, String>

                    val usernameDB = childData.get("username")
                    if (usernameDB == username){
                        dateOfBirth = childData.get("date_of_birth")!!
                    }
                    val usernameText = getString(R.string.profile_username, username)
                    findViewById<TextView>(R.id.username_profile).text = usernameText

                    val birthdayText = getString(R.string.profile_birthday, dateOfBirth)
                    findViewById<TextView>(R.id.birthday_profile).text = birthdayText
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Profile", "Error getting documents: ", exception)
            }


        // round happinessMean to get happiness Index for this user
        happinessIndex=(happinessMean * 100.0).roundToInt() / 100.0

        lastLocations=posList

        // create list of locations
        val recyclerView = findViewById<RecyclerView>(R.id.location_list)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        val adapter = LocationListAdapter(lastLocations)
        recyclerView.adapter = adapter

        val titleText = getString(R.string.profile_title, username)
        findViewById<TextView>(R.id.title).text = titleText

        val happinessIndexText = getString(R.string.happiness_index_value, happinessIndex.toString())
        findViewById<TextView>(R.id.happiness_index_value).text = happinessIndexText

        var imageHappinessPath = 0

        // set image
        when {
            happinessIndex < 0.1 -> imageHappinessPath = R.drawable.happy_level1
            happinessIndex >= 0.1 && happinessIndex < 0.5 -> imageHappinessPath = R.drawable.happy_level2
            happinessIndex >= 0.5 && happinessIndex < 0.75 -> imageHappinessPath = R.drawable.happy_level3
            happinessIndex >= 0.75 -> imageHappinessPath = R.drawable.happy_level4
        }
        findViewById<ImageView>(R.id.happiness_face).setImageResource(imageHappinessPath)

    }

    // method to set change password button visible, called from ChangePasswordFragment
    fun resetButton() {
        if (changePasswordButton != null){
            changePasswordButton?.visibility = VISIBLE
        }
    }

    // method to set change password button invisible, called from ChangePasswordFragment
    fun setButtonToInvisible(){
        if(changePasswordButton != null){
            changePasswordButton?.visibility = INVISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(toggle.onOptionsItemSelected(item))
        {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun isLocationEnabled(): Boolean
    {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Both GPS and internet access must be enabled
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }
}

// class to manage the list of latest locations
class LocationListAdapter(private val locationList: List<Location>) :
    RecyclerView.Adapter<LocationListAdapter.LocationListViewHolder>() {

    class LocationListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textView: TextView = itemView.findViewById(R.id.item)

        fun bind(text: String) {
            textView.text = text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.last_location_list_item, parent, false)
        return LocationListViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationListViewHolder, position: Int) {
        val text = holder.itemView.context.getString(R.string.last_location_item, locationList[position].street, locationList[position].city, locationList[position].timestamp)
        holder.bind(text)
    }

    override fun getItemCount() = locationList.size
}

data class Location(
    val street: String?,
    val city: String?,
    val timestamp: String
)