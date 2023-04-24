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

        val button = findViewById<Button>(R.id.btn_toMap)

        button.setOnClickListener {
            val nexPage = Intent(this, MapActivity::class.java)
            startActivity(nexPage)
        }
    }
}