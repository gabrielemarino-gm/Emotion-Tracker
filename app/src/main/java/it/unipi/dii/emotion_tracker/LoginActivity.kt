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

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.button_login);
        registerButton = findViewById(R.id.button_register);

        var login = 0


        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()



            validateLogin(username, password) { loginSuccessful ->
                if (loginSuccessful) {
                    // If authentication succeeds, transition to the main activity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If authentication fails, show an error message
                    usernameEditText.setText("")
                    passwordEditText.setText("")
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        registerButton.setOnClickListener(){
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateLogin(username: String, password: String, callback: (Boolean) -> Unit) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var userExists = false
                snapshot.children.forEach { child ->
                    val childData = child.value as HashMap<String,String>

                    val username_db=childData.get("username")
                    val password_db=childData.get("password")


                    if(username==username_db && password_db?.let { isPasswordMatch(password, it) } == true){
                        userExists = true
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

    private fun isPasswordMatch(inputPassword: String, storedPasswordHash: String): Boolean {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hash = messageDigest.digest(inputPassword.toByteArray(StandardCharsets.UTF_8))
        val hexString = StringBuilder()
        for (byte in hash) {
            val hex = Integer.toHexString(0xff and byte.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString() == storedPasswordHash
    }

}