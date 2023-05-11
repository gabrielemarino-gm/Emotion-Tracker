package it.unipi.dii.emotion_tracker


import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.util.Date
import kotlin.math.roundToInt

class TrialActivity : AppCompatActivity() {
    private lateinit var changePasswordButton: Button
    private lateinit var username: String
    private var dateOfBirth: String = ""
    private var happinessIndex: Double = 0.0
    private lateinit var lastLocations: List<Location>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trial)

        //Retrieve username of the logged user
        val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        username = prefs.getString("username", "")!!

        retrieveHappiness()
        //inflateProfile()

        changePasswordButton = findViewById(R.id.change_password_button)
        changePasswordButton.setOnClickListener(){
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            val changePasswordFragment = ChangePasswordFragment(this, username)
            transaction.add(R.id.change_password_container, changePasswordFragment, "change_password")
            transaction.addToBackStack(null)
            transaction.setReorderingAllowed(true)
            transaction.commit()
            changePasswordButton.visibility = INVISIBLE
        }
    }

    private fun retrieveHappiness() {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val posRef: DatabaseReference = database.getReference("position_emotion")
        val userRef=posRef.orderByChild("username").equalTo(username)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var posUserList= mutableListOf<LocationCell>()

                snapshot.children.forEach{child ->
                    val childData = child.value as java.util.HashMap<String, Any>

                    val positionRecord=LocationCell(
                        childData["latitude"] as Double,
                        childData["longitude"] as Double,childData["street"] as String,childData["city"] as String,childData["emotion"] as Double,
                        childData["timestamp"] as Long,childData["username"] as String)

                    posUserList.add(positionRecord)
                }

                posUserList= posUserList.sortedByDescending { it.timestamp } as MutableList<LocationCell>


                var sommaHappiness=0.0
                var count=0

                for(pos in posUserList){
                    sommaHappiness+=pos.emotion
                    count+=1
                }

                val happinessMean=sommaHappiness/count

                //now we want to obtain the 5 latest location
                posUserList= posUserList.take(5) as MutableList<LocationCell>

                val timestampLast= mutableListOf<Long>()
                val dateLast= mutableListOf<String>()
                for(i in 0 until 5){
                    timestampLast.add(posUserList[i].timestamp)
                    val date = Date(timestampLast[i]) // create a new Date object from the timestamp
                    val format = getDateInstance(DateFormat.DEFAULT) // create a date format
                    val dateString = format.format(date) // format the date as a string
                    dateLast.add(dateString)
                }

                val posList = listOf( Location(posUserList[0].street, posUserList[0].city, dateLast[0]),
                    Location(posUserList[1].street, posUserList[1].city, dateLast[1]),
                    Location(posUserList[2].street, posUserList[2].city, dateLast[2]),
                    Location(posUserList[3].street, posUserList[3].city, dateLast[3]),
                    Location(posUserList[4].street, posUserList[4].city, dateLast[4])
                )
                /*
                var posList= mutableListOf<LocationCell>()
                posList.add(posUserList[0])
                var numElements = 1
                var i = 1
                while(i <= posUserList.size && numElements < 5){
                    var nextLocation = posUserList.get(i)
                    if (nextLocation.city.equals(posList.last().city) &&
                        nextLocation.street.equals(posList.last().street)){
                        i++
                        continue
                    }
                    else{
                        i++
                        posList.add(nextLocation)
                        numElements++
                    }
                }

                val listToReturn : ArrayList<Location> = ArrayList(numElements)

                for (j in 0 until numElements){
                    val date = Date(posUserList[j].timestamp) // create a new Date object from the timestamp
                    val format = getDateInstance(DateFormat.DEFAULT) // create a date format
                    val dateString = format.format(date) // format the date as a string
                    listToReturn.add(Location(posList[j].street, posList[j].city, dateString))
                }
                */

                inflateProfile(happinessMean,posList)

            }

            override fun onCancelled(error: DatabaseError) {
                println("database error in retrieve this info")

            }
        })
    }

    private fun inflateProfile(happinessMean: Double, posList: List<Location>) {
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
                Log.e("Profile", "Error getting documents: ", exception)
            }


        //compute happinessIndex and lastLocations

        happinessIndex=(happinessMean * 100.0).roundToInt() / 100.0
        lastLocations=posList

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

    fun resetButton() {
        changePasswordButton.visibility = VISIBLE
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
    val street: String?,
    val city: String?,
    val timestamp: String
)