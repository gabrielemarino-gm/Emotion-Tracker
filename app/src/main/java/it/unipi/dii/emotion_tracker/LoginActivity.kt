package it.unipi.dii.emotion_tracker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    companion object{
        private const val LOGGED_USERNAME_KEY ="LOGGED_USERNAME_KEY"
        private const val LOGGED_USER_PASSWORD_KEY ="LOGGED_USER_PASSWORD_KEY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //set the setContentView based on the phone's orientation
        //getViewBasedOnOrientation()
        setContentView(R.layout.activity_login)
        usernameEditText = findViewById(R.id.username_login)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.button_login_l)
        registerButton = findViewById(R.id.button_register_l)

        var login = 0

        // Restore the state of the UI elements if savedInstanceState is not null
        if (savedInstanceState != null) {
            val username = savedInstanceState.getString(LOGGED_USERNAME_KEY)
            usernameEditText.setText(username)

            val password = savedInstanceState.getString(LOGGED_USER_PASSWORD_KEY)
            passwordEditText.setText(password)
        }
        checkActiveSession() //to avoid to make login with username and password
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            validateLogin(username, password) { loginSuccessful ->
                if (loginSuccessful) {
                    val token = UUID.randomUUID().toString()
                    storeTokenLocally(this, token, username)
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

        registerButton.setOnClickListener() {
            val intent = Intent(this, RegisterActivity::class.java)
            //  val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of the UI elements into the bundle
        outState.putString(LOGGED_USERNAME_KEY, usernameEditText.text.toString())
        outState.putString(LOGGED_USER_PASSWORD_KEY, passwordEditText.text.toString())
    }

    private fun getViewBasedOnOrientation() {
        when (getPhoneOrientation()) {
            Configuration.ORIENTATION_PORTRAIT -> {
                setContentView(R.layout.activity_login)
            }

            Configuration.ORIENTATION_LANDSCAPE -> {
                setContentView(R.layout.activity_login)
            }
        }
    }

    private fun getPhoneOrientation(): Int {
        return resources.configuration.orientation
    }


    private fun checkActiveSession() {


        val prefs = getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)
        val token =
            prefs.getString("token", null) // retrieve the token with the user ID as a prefix


        if (token != null) {
            //the user was previously logged
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            println("user not logged")
        }

    }

    private fun storeTokenLocally(
        context: Context,
        token: String,
        username: String
    ) {
        val sharedPreferences =
            context.getSharedPreferences("myemotiontrackerapp", Context.MODE_PRIVATE)

        println("storing token locally")

        with(sharedPreferences.edit()) {
            putString("token", token)
            putString("username", username)
            apply()
        }
    }

    private fun validateLogin(username: String, password: String, callback: (Boolean) -> Unit) {
        val database: FirebaseDatabase =
            FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                var userExists = false
                snapshot.children.forEach { child ->
                    val childData = child.value as HashMap<String, String>

                    val username_db = childData.get("username")
                    val password_db = childData.get("password")


                    if (username == username_db && password_db?.let {
                            isPasswordMatch(
                                password,
                                it
                            )
                        } == true) {
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