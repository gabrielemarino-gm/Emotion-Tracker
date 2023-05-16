package it.unipi.dii.emotion_tracker

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import it.unipi.dii.emotion_tracker.databinding.FragmentChangePasswordBinding
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HashMap

class ChangePasswordFragment(
    private var parentActivity : AccountActivity,
    private var username: String
) : Fragment() {
    //constructor
    constructor() : this(AccountActivity(),"")

    private lateinit var changePasswordButton: Button
    private lateinit var goBackButton: Button
    //Binding to layout objects
    private var binding: FragmentChangePasswordBinding? = null
    //Non null reference to binding
    private val fragmentChangePasswordBinding
        get() = binding!!

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://emotion-tracker-48387-default-rtdb.europe-west1.firebasedatabase.app/")
    private val usersRef: DatabaseReference = database.getReference("users")
    // rotation related
    private lateinit var oldPasswordEditText:EditText
    private lateinit var newPasswordEditText : EditText
    companion object{
        private const val CHANGE_PASSWORD_CONTAINER_KEY ="CHANGE_PASSWORD_KEY"
        private const val CAMERA_FRAGMENT_KEY ="CAMERA_FRAGMENT_KEY"
        private const val LATEST_LOCATIONS_KEY="LATEST_LOCATIONS_KEY"
        private const val OLD_PASSWORD_KEY ="OLD_PASSWORD_KEY"
        private const val NEW_PASSWORD_KEY ="NEW_PASSWORD_KEY"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //bind layout to Kotlin objects
        binding = FragmentChangePasswordBinding.inflate(inflater)

        goBackButton = fragmentChangePasswordBinding.backButton

        // access the text values of old and new password fields then save them to the bundle
        oldPasswordEditText = fragmentChangePasswordBinding.oldPassword
        newPasswordEditText = fragmentChangePasswordBinding.newPassword

        // Retrieving saved values (if any) from the bundle (happens when rotating the screen)
        if (savedInstanceState != null) {
            val oldPasswordSaved = savedInstanceState.getString(OLD_PASSWORD_KEY)
            oldPasswordEditText.setText(oldPasswordSaved)
            val newPasswordSaved = savedInstanceState.getString(NEW_PASSWORD_KEY)
            newPasswordEditText.setText(newPasswordSaved)
            username = savedInstanceState.getString("username")!!
        }

        goBackButton.setOnClickListener(){
            parentFragmentManager.beginTransaction().remove(this).commit()
            // remove the fragment from back stack (pressing back button doesn't create it again)
            parentFragmentManager.popBackStack()
        }

        changePasswordButton = fragmentChangePasswordBinding.changePassword
        changePasswordButton.setOnClickListener {
             val oldPassword = fragmentChangePasswordBinding.oldPassword.text.toString()
             val newPassword = encryptPassword(fragmentChangePasswordBinding.newPassword.text.toString())

            // check whether oldPassword is correct or not
            validatePassword(username, oldPassword) { childId ->
                if (childId != null) {
                    // oldPassword is the correct one, password is changed
                    usersRef.child(childId).child("password").setValue(newPassword)
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Password successfully changed", Toast.LENGTH_SHORT).show()
                    }
                    parentFragmentManager.beginTransaction().remove(this).commit()
                    // remove the fragment from back stack (pressing back button doesn't create it again)
                    parentFragmentManager.popBackStack()
                } else {
                    // oldPassword is not the correct one
                    fragmentChangePasswordBinding.oldPassword.setText("")
                    fragmentChangePasswordBinding.newPassword.setText("")
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Inserted password is not correct", Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }

        // Set changePasswordButton to invisible
        parentActivity.setChangePasswordButtonToInvisible()
        return fragmentChangePasswordBinding.root
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(OLD_PASSWORD_KEY, oldPasswordEditText.toString())
        outState.putString(NEW_PASSWORD_KEY, newPasswordEditText.toString())
        outState.putString("username", username)
    }

    private fun encryptPassword(password: String): String {
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

    private fun validatePassword(username: String, password: String, callback: (String?) -> Unit) {
        var childId : String? = null
        usersRef.get()
            .addOnSuccessListener { documents ->
                documents.children.forEach { child ->
                    val childData = child.value as HashMap<String, String>

                    val usernameDB=childData.get("username")
                    val passwordDB=childData.get("password")

                    if(username==usernameDB && passwordDB == encryptPassword(password)){
                        childId = child.key
                    }
                }
                callback(childId)
            }
            .addOnFailureListener { exception ->
                Log.e("Password-check", "Error getting documents: ", exception)
            }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // set changePasswordButton to visible
        parentActivity.setChangePasswordButtonToVisible()
    }

    fun changeParentActivity(accountActivity: AccountActivity) {
        // change parent activity (rotation-related issue), in case container activity is recreated and fragment is not
        parentActivity = accountActivity
    }

}