package it.unipi.dii.emotion_tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    //private lateinit var dateOfBirth: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        usernameText = findViewById(R.id.username_reg);
        passwordText = findViewById(R.id.password_reg);
        //dateOfBirth = findViewById(R.id.date_of_birth)
        registerButton = findViewById(R.id.register);
        backButton = findViewById(R.id.back_button);

        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        registerButton.setOnClickListener(){

            controlloUsername(usernameText.text.toString(),myRef) { controllo ->
                if (controllo) {
                    val user = User(
                        usernameText.text.toString(),
                        encryptPassword(passwordText.text.toString())
                    )
                    myRef.push().setValue(user)

                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    usernameText.setText("")
                    passwordText.setText("")
                    Toast.makeText(this, "Username already used", Toast.LENGTH_SHORT).show()
                }
            }
        }

        backButton.setOnClickListener(){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }


    }

    private fun controlloUsername(username: String, myRef: DatabaseReference, callback: (Boolean) -> Unit){

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var userExists = true
                snapshot.children.forEach { child ->
                    val childData = child.value as HashMap<String,String>

                    val username_db=childData.get("username")

                    if(username==username_db ){
                        userExists = false
                    }
                }
                callback(userExists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error case
                println("error in retrieving users")
                callback(false)
            }
        })

    }


    public fun encryptPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hash = messageDigest.digest(password.toByteArray(StandardCharsets.UTF_8))
        val hexString = StringBuilder()
        for (byte in hash) {
            val hex = Integer.toHexString(0xff and byte.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}

class User(usernameText: String, passwordText:String) {
    var username : String =usernameText
    var password : String =passwordText
    //var date_of_birth : SimpleDateFormat =dateOfBirth
}
