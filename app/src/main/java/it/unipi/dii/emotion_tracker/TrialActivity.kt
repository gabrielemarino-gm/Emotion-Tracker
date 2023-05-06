package it.unipi.dii.emotion_tracker


import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.Date

class TrialActivity : AppCompatActivity() {
    private lateinit var username: String
    private var happinessIndex: Double = 0.0
    private lateinit var lastLocations: List<Location>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trial)

        //val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        //username = prefs.getString("username", null) // retrieve the token with the user ID as a prefix

        username = "pippo"

        //query to database to get info about pippo
        //compute happinessIndex and lastLocations

        val timestamp = System.currentTimeMillis() // current timestamp in milliseconds
        val date = Date(timestamp) // create a new Date object from the timestamp
        val format = getDateInstance(DateFormat.DEFAULT) // create a date format
        val dateString = format.format(date) // format the date as a string

        happinessIndex = 0.79
        lastLocations = listOf( Location("Via1", "Pisa", dateString),
                                Location("Via2", "Pisa", dateString),
                                Location("Via3", "Pisa", dateString),
                                Location("Via4", "Pisa", dateString),
                                Location("Via5", "Pisa", dateString)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.location_list)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        val adapter = MyAdapter(lastLocations)
        recyclerView.adapter = adapter

        val text = getString(R.string.profile_title, username)
        val title = findViewById<TextView>(R.id.title)
        title.text = text
    }
}

class MyAdapter(private val myDataset: List<Location>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false) as TextView
        return MyViewHolder(textView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.textView.text = myDataset[position].street
    }

    override fun getItemCount() = myDataset.size
}

data class Location(
    val street: String,
    val city: String,
    val timestamp: String
)