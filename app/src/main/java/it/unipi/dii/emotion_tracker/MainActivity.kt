package it.unipi.dii.emotion_tracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val trialButton = findViewById<Button>(R.id.btn_trial)
        val mapButton = findViewById<Button>(R.id.btn_toMap)


        trialButton.setOnClickListener {
            val trialPage = Intent(this, TrialActivity::class.java)
            startActivity(trialPage)
        }
        mapButton.setOnClickListener {
            val mapPage = Intent(this, MapActivity::class.java)
            startActivity(mapPage)
        }
    }
}