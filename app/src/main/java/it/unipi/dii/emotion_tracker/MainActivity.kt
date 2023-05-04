package it.unipi.dii.emotion_tracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val trialButton = findViewById<Button>(R.id.btn_trial)
        val mapButton = findViewById<Button>(R.id.btn_toMap)
        val logoutButton =findViewById<Button>(R.id.logout)

        //necessary to know if the user is logged in, if he makes logout and then presses the button back he would enter in this page (not correct behaviour)
        val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        if(token==null){
            //the user is not logged
            val loginPage = Intent(this, LoginActivity::class.java)
            startActivity(loginPage)
            finish()
        }
        else{
            println("user logged")
        }


        trialButton.setOnClickListener {
            val trialPage = Intent(this, TrialActivity::class.java)
            startActivity(trialPage)
        }
        mapButton.setOnClickListener {
            val mapPage = Intent(this, MapActivity::class.java)
            startActivity(mapPage)
        }
        logoutButton.setOnClickListener{
            //remove token from sharedPreferences
            val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove("token")
            editor.apply()

            val loginPage = Intent(this, LoginActivity::class.java)
            startActivity(loginPage)
            finish()
        }
    }
}