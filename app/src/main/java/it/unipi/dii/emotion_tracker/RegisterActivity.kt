package it.unipi.dii.emotion_tracker

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import android.widget.*
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

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = Array(currentYear - 1899) { i -> (i + 1900) }
        val months = Array(12) { i -> (i + 1) }
        val days = Array(31) { i -> (i + 1) }

        val spinner_year = findViewById<Spinner>(R.id.spinner3)
        val adapter_year = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
        spinner_year.adapter = adapter_year

        val spinner_month = findViewById<Spinner>(R.id.spinner2)
        val adapter_month = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        spinner_month.adapter = adapter_month

        val spinner_day = findViewById<Spinner>(R.id.spinner)
        val adapter_day = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        spinner_day.adapter = adapter_day

        var selectedDay = findViewById<TextView>(R.id.testViewDay)
        val selectedMonth = findViewById<TextView>(R.id.testViewMonth)
        val selectedYear = findViewById<TextView>(R.id.testViewYear)
        
        setContentSpinner(selectedDay,spinner_day)
        setContentSpinner(selectedMonth,spinner_month)
        setContentSpinner(selectedYear,spinner_year)



        usernameText = findViewById(R.id.username_reg);
        passwordText = findViewById(R.id.password_reg);
        //dateOfBirth = findViewById(R.id.date_of_birth)
        registerButton = findViewById(R.id.register);
        backButton = findViewById(R.id.back_button);

        val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
        val myRef: DatabaseReference = database.getReference("users")

        registerButton.setOnClickListener(){

            // TODO implement the date format if needed
            //val calendar = Calendar.getInstance()
            //calendar.set(selectedDay.text.toString().toInt(), selectedMonth.text.toString().toInt(), selectedYear.text.toString().toInt())
            //val date_of_birth = calendar.time

            controlloUsername(usernameText.text.toString(),myRef) { controllo ->
                if (controllo) {
                    val user = User(
                        usernameText.text.toString(),
                        encryptPassword(passwordText.text.toString()),
                        "${selectedDay.text.toString().toInt()}/${selectedMonth.text.toString().toInt()}/${selectedYear.text.toString().toInt()}"
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

    private fun setContentSpinner(selectedTextView: TextView, spinner: Spinner) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position)
                    selectedTextView.text = selectedItem.toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                //nothing to do
            }
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

class User(usernameText: String, passwordText: String, date_of_birth: String) {
    var username : String =usernameText
    var password : String =passwordText
    var date_of_birth : String = date_of_birth
}
