package it.unipi.dii.emotion_tracker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Geocoder
import android.os.IBinder
import com.google.firebase.database.*
import smile.clustering.DBSCAN
import java.util.*

class ClusterService(): Service()
{
    companion object // static
    {
        private lateinit var prefs: SharedPreferences
        private lateinit var username: String
        private lateinit var clusterRef: DatabaseReference
        private lateinit var myRef: DatabaseReference

        fun initialize(context: Context)
        {
            prefs = context.getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
            username = prefs.getString("username", "")!!
            val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
            clusterRef = database.getReference("clusters_$username")
            myRef = database.getReference("position_emotion")
        }
    }

    override fun onBind(intent: Intent): IBinder? { return null }

    // On start, this service have to create the clusters and save them into the Database
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        // println("DBG: onStartCommand()") // OKAY
        println("DBG: ClusterService.onStartCommand()")
        generateClusters()
        return START_STICKY
    }

    // On destroy the service need to delete clusters
    override fun onDestroy()
    {
        super.onDestroy()
    }

    override fun onCreate()
    {
        super.onCreate()
        initialize(applicationContext)
    }


    // Read all the points from the DB, generate clusters and then save clusters again into the DB
    private fun generateClusters()
    {
        println("DBG: ClusterService.generateClusters()")
        val listCluster: MutableList<ClusterCentroid> = mutableListOf<ClusterCentroid>()

        myRef.addListenerForSingleValueEvent(object : ValueEventListener
        {
            override fun onDataChange(snapshot: DataSnapshot)
            {
                val data = mutableListOf<List<Double>>()
                val labeledClass = mutableListOf<Double>()

                snapshot.children.forEach { child ->
                    val childData = child.value as HashMap<String, Any>
                    //println("elem$childData")

// (    Loop through the results and take just the information for make the clusters
                    data.add(
                        listOf(
                            childData.get("latitude") as Double,
                            childData.get("longitude") as Double
                        )
                    )
                    labeledClass.add(((childData.get("emotion") as Double)))
// )
                }

                println("DBG: generateClusters().data = $data")
                // Convert the list in array, because the class DBSCAN accept only this type
                val dataArray = Array(data.size) { i -> data[i].toDoubleArray() }
                val scoreArray = Array(labeledClass.size) { i -> labeledClass[i] }

                // Create the DBSCAN model
                val dbscan = DBSCAN.fit(dataArray, 7, 0.00075)
                println("DBG: after DBSCAN")
                // Execution of the cluster
                val labels = dbscan.y

                // (    Discover the points of each cluster.
                val clusterPoints = mutableMapOf<Int, MutableList<List<Double>>>()
                val scorePointCluster = mutableMapOf<Int, MutableList<List<Double>>>()

                for (i in dataArray.indices) {
                    val label = labels[i]
                    val point = dataArray[i].toList()
                    val score = scoreArray[i]

                    scorePointCluster.getOrPut(label, { mutableListOf() }).add(listOf(score))
                    clusterPoints.getOrPut(label, { mutableListOf() }).add(point)
                }
                // )

                // Find the centroid: the means of all the points inside a single cluster
                for (i in clusterPoints.keys) {
                    var lat: Double = 0.0
                    var lon: Double = 0.0
                    var sco: Double = 0.0
                    var numberOfPointsInCluster: Long = 0

                    for (point in clusterPoints[i]!!) {
                        lat = lat + point[0]
                        lon = lon + point[1]
                        numberOfPointsInCluster++  //count the number of points in the cluster
                    }

                    var scoreIndx: Int = 0
                    while (scoreIndx < scorePointCluster[i]!!.size) {
                        sco += scorePointCluster[i]!![scoreIndx][0]
                        scoreIndx++
                    }

                    lat /= clusterPoints[i]?.size!!
                    lon /= clusterPoints[i]?.size!!
                    sco /= scorePointCluster[i]?.size!!

                    val geocoder = Geocoder(applicationContext, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val address = addresses?.get(0)

                    val street = address?.thoroughfare
                    val city = address?.locality

                    val timestamp = System.currentTimeMillis()

                    val cluster = ClusterCentroid(
                        lat,
                        lon,
                        street,
                        city,
                        sco,
                        numberOfPointsInCluster,
                        timestamp
                    )

                    // Add the single cluster into the list
                    listCluster.add(cluster)
                }

                println("DBG: invoco writeClusterDB()")
                writeClusterDB(listCluster)
            }

            override fun onCancelled(error: DatabaseError) {
                println("error")
            }

        })
    }

    // This method start the connection to the DataBase for write the collection Clusters
    private fun writeClusterDB(listClusterLoc: MutableList<ClusterCentroid>)
    {
        // Before, we need to delete all the old data
        clusterRef.removeValue()

        // Now we can finally write
        for (cluster in listClusterLoc)
            clusterRef.push().setValue(cluster)
    }
}