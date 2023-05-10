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
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameText: EditText
    private lateinit var passwordText: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: Button
    private lateinit var datePicker: DatePicker
    //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        usernameText = findViewById(R.id.username_x)
        passwordText = findViewById(R.id.password_x)
        registerButton = findViewById(R.id.register_x)
        backButton = findViewById(R.id.back_x)

        datePicker = findViewById(R.id.datePicker)
        // limit the years from 2006 and 2006
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, 1900)
        val minDate = calendar.timeInMillis
        calendar.set(Calendar.YEAR, 2006)
        val maxDate = calendar.timeInMillis
        datePicker.minDate = minDate
        datePicker.maxDate = maxDate

        //
        // usernameText, passwordText, datePicker
        val year = datePicker.year
        val month = datePicker.month + 1
        val day = datePicker.dayOfMonth
        //
        if (savedInstanceState != null) {
            usernameText.setText(savedInstanceState.getString("username"))
            passwordText.setText(savedInstanceState.getString("password"))
            val year = savedInstanceState.getInt("year")
            val month = savedInstanceState.getInt("month")
            val day = savedInstanceState.getInt("day")
            datePicker.updateDate(year, month, day)
        }
        val database:FirebaseDatabase= FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef:DatabaseReference =  database.getReference("users")
        //
        registerButton.setOnClickListener(){
            controlloUsername(usernameText.text.toString(),myRef) { controllo ->
                if (controllo) {
                    val user = User(
                        usernameText.text.toString().trim(),
                        encryptPassword(passwordText.text.toString().trim()),
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
        outState.putString("username", usernameText.text.toString())
        outState.putString("password", passwordText.text.toString())
        outState.putInt("year", datePicker.year)
        outState.putInt("month", datePicker.month)
        outState.putInt("day", datePicker.dayOfMonth)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        //
        val savedUsername = savedInstanceState.getString("username")
        val savedPassword = savedInstanceState.getString("password")
        //datePicker
        val savedYear = savedInstanceState.getInt("year")
        val savedMonth = savedInstanceState.getInt("month")
        val savedDay = savedInstanceState.getInt("day")
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