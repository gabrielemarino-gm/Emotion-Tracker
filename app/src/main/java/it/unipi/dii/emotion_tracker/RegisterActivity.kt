package it.unipi.dii.emotion_tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    //private lateinit var dateOfBirth: EditText
    private lateinit var registerButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameText = findViewById(R.id.username_reg);
        passwordText = findViewById(R.id.password_reg);
        //dateOfBirth = findViewById(R.id.date_of_birth)
        registerButton = findViewById(R.id.register);

        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        registerButton.setOnClickListener(){
            val user= User(usernameText.text.toString(),passwordText.text.toString())
            myRef.push().setValue(user)

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

class User(usernameText: String, passwordText:String) {
    var username : String =usernameText
    var password : String =passwordText
    //var date_of_birth : SimpleDateFormat =dateOfBirth
}
