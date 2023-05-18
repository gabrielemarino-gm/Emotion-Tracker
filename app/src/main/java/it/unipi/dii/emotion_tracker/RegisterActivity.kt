package it.unipi.dii.emotion_tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Year
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: Button
    private lateinit var datePicker: DatePicker
    //
    companion object {
        private const val USERNAME_KEY ="USERNAME_KEY"
        private const val PASSWORD_KEY ="PASSWORD_KEY"
        private const val YEAR_KEY ="YEAR_KEY"
        private const val MONTH_KEY ="MONTH_KEY"
        private const val DAY_KEY ="DAY_KEY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        usernameText = findViewById(R.id.username_x)
        passwordText = findViewById(R.id.password_x)
        registerButton = findViewById(R.id.register_x)
        backButton = findViewById(R.id.back_x)
        //
        datePicker = findViewById(R.id.datePicker)
        // Here is the age restriction: that spans 1900 and 2006
        // We assume that the user is of say age X years old such that: 18<= X <= 123 years
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 1900)
        val minDate = calendar.timeInMillis
        calendar.set(Calendar.YEAR, 2006)
        val maxDate = calendar.timeInMillis
        datePicker.minDate = minDate
        datePicker.maxDate = maxDate

        var year = datePicker.year
        var month = datePicker.month+1
        var day = datePicker.dayOfMonth

        datePicker.init(year, month, day) { view, yearOfDOB, monthOfDOB, dayOfDOB ->

            year  = yearOfDOB
            month = monthOfDOB + 1
            day   = dayOfDOB
        }
        // Retrieve the values from the <<Bundle>> for the username, password and the DOB values for any
        // orientation change of the phone (Portrait or LandScape )
        if (savedInstanceState != null) {
            usernameText.setText(savedInstanceState.getString(USERNAME_KEY))
            passwordText.setText(savedInstanceState.getString(PASSWORD_KEY))
            val year = savedInstanceState.getInt(YEAR_KEY)
            val month = savedInstanceState.getInt(MONTH_KEY)
            val day = savedInstanceState.getInt(DAY_KEY)
            datePicker.updateDate(year, month, day)
        }
        val database:FirebaseDatabase= FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef:DatabaseReference =  database.getReference("users")
        //
        registerButton.setOnClickListener(){
            controlloUsername(usernameText.text.toString(),myRef) { controllo ->
                if (controllo) {

                    val user = User(
                        usernameText.text.toString(),
                        encryptPassword(passwordText.text.toString()),
                        "${year}/${month}/${day}"
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
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    public fun encryptPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hash = messageDigest.digest(password.toByteArray(StandardCharsets.UTF_8))
        val hexString = StringBuilder()
        //
        for (byte in hash) {
            val hex = Integer.toHexString(0xff and byte.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(USERNAME_KEY, usernameText.text.toString())
        outState.putString(PASSWORD_KEY, passwordText.text.toString())
        outState.putInt(YEAR_KEY, datePicker.year)
        outState.putInt(MONTH_KEY, datePicker.month)
        outState.putInt(DAY_KEY, datePicker.dayOfMonth)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        //
        val savedUsername = savedInstanceState.getString(USERNAME_KEY)
        val savedPassword = savedInstanceState.getString(PASSWORD_KEY)
        //datePicker
        val savedYear = savedInstanceState.getInt(YEAR_KEY)
        val savedMonth = savedInstanceState.getInt(MONTH_KEY)
        val savedDay = savedInstanceState.getInt(DAY_KEY)
        // Update the views with the saved values
        usernameText.setText(savedUsername)
        passwordText.setText(savedPassword)
        datePicker.init(savedYear, savedMonth, savedDay, null)
    }
}
class User(usernameText: String, passwordText: String, date_of_birth: String) {
    var username : String =usernameText
    var password : String =passwordText
    var date_of_birth : String = date_of_birth
}