package it.unipi.dii.emotion_tracker

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<Button>(R.id.btn_toCamera)
        val mapButton = findViewById<Button>(R.id.btn_toMap)


        cameraButton.setOnClickListener {
            val cameraPage = Intent(this, CameraActivity::class.java)
            startActivity(cameraPage)
        }
        mapButton.setOnClickListener {
            val mapPage = Intent(this, MapActivity::class.java)
            startActivity(mapPage)
        }
    }
}