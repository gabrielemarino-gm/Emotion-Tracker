package it.unipi.dii.emotion_tracker


import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginBottom
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.Date

class TrialActivity : AppCompatActivity() {
    private lateinit var username: String
    private var dateOfBirth: String = ""
    private var happinessIndex: Double = 0.0
    private lateinit var lastLocations: List<Location>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trial)

        inflateProfile()


    }

    private fun inflateProfile(){
        //val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        //username = prefs.getString("username", null) // retrieve the token with the user ID as a prefix

        username = "fabio"

        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        myRef.get()
            .addOnSuccessListener { documents ->
                documents.children.forEach { child ->
                    val childData = child.value as HashMap<String, String>

                    val username_db = childData.get("username")
                    if (username_db == username){
                        dateOfBirth = childData.get("date_of_birth")!!
                    }
                    val usernameText = getString(R.string.profile_username, username)
                    findViewById<TextView>(R.id.username_profile).text = usernameText

                    val birthdayText = getString(R.string.profile_birthday, dateOfBirth)
                    findViewById<TextView>(R.id.birthday_profile).text = birthdayText
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ciao", "Error getting documents: ", exception)
            }


        //compute happinessIndex and lastLocations

        val timestamp = System.currentTimeMillis() // current timestamp in milliseconds
        val date = Date(timestamp) // create a new Date object from the timestamp
        val format = getDateInstance(DateFormat.DEFAULT) // create a date format
        val dateString = format.format(date) // format the date as a string

        happinessIndex = 0.79
        lastLocations = listOf( Location("ViaViaviaViaViaViaViaViaViaViVia1", "Pisa", dateString),
            Location("Via2", "Pisa", dateString),
            Location("Via3", "Pisa", dateString),
            Location("Via4", "Pisa", dateString),
            Location("Via5", "Pisa", dateString)
        )

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

        when {
            happinessIndex < 0.25 -> imageHappinessPath = R.drawable.happy_level1
            happinessIndex >= 0.25 && happinessIndex < 0.5 -> imageHappinessPath = R.drawable.happy_level2
            happinessIndex >= 0.5 && happinessIndex < 0.75 -> imageHappinessPath = R.drawable.happy_level3
            happinessIndex >= 0.75 -> imageHappinessPath = R.drawable.happy_level4
        }
        findViewById<ImageView>(R.id.happiness_face).setImageResource(imageHappinessPath)
    }
}

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
    val street: String,
    val city: String,
    val timestamp: String
)