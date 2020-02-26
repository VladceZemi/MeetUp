package cz.brno.mendelu.meetup.activities.login

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.activities.PlaceActivity
import cz.brno.mendelu.meetup.dataclasses.Position
import cz.brno.mendelu.meetup.dataclasses.User
import cz.brno.mendelu.meetup.functionclasses.DataProcessing
import kotlinx.android.synthetic.main.content_registration.*
import java.util.*
import java.util.regex.Pattern
import java.util.regex.Matcher as Matcher

class RegistrationActivity : AppCompatActivity() {

    private val TAG: String = "RegistrationActivity"
    private val PLACE_ACTIVITY = 100

    private lateinit var firebaseServer: FireBaseServer

    private var selectedDefaultPosition: Position? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        firebaseServer = FireBaseServer(this)

        registerDefaultAddress.setOnClickListener {
            val et: EditText = it as EditText
            et.error = null
            val intent: Intent = Intent(this, PlaceActivity::class.java)
            startActivityForResult(intent, PLACE_ACTIVITY)
        }

        registerButton.setOnClickListener {
            val dataProcessing = DataProcessing(this)
            if (dataProcessing.setErrorIfEmpty(nameInput) &&
                dataProcessing.setErrorIfBadPhone(phoneInput) &&
                dataProcessing.setErrorIfBadEmail(emailInput) &&
                dataProcessing.setErrorIfBadPassword(passwordInput) &&
                dataProcessing.setErrorIfEmpty(registerDefaultAddress)) {
                this.registerUser(emailInput.text.toString(), passwordInput.text.toString(), nameInput.text.toString(), phoneInput.text.toString())
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PLACE_ACTIVITY -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        val latitude: Double = it.getDoubleExtra(getString(R.string.ex_latitude), Double.NaN)
                        val longitude: Double = it.getDoubleExtra(getString(R.string.ex_longitude), Double.NaN)
                        if (!latitude.isNaN() && !longitude.isNaN()) {
                            selectedDefaultPosition = Position(latitude, longitude)
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(
                                selectedDefaultPosition!!.latitude!!,
                                selectedDefaultPosition!!.longitude!!,
                                1
                            )
                            val address: Address = addresses[0]
                            val addressLine: String = address.getAddressLine(0)
                            registerDefaultAddress.setText(addressLine)
                        }
                    }
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, getString(R.string.account_position_saved), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getErrorString(message :String): String {
        val m: Matcher = Pattern.compile("\\[([^\\]]+)\\]").matcher(message)
        if (m.find()) {
            return m.group(1)
        }
        return message
    }

    private fun registerUser(email: String, password: String, name: String, phone: String) {
        val firebaseReference = firebaseServer.getDatabaseReference()
        val auth = firebaseServer.auth

        //ošetřit vstupy
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val currentUser = auth.currentUser

                    val user = User(name)
                    user.email = email
                    user.phone = phone
                    user.position = selectedDefaultPosition

                    firebaseServer.sendDataToFirebase(firebaseReference.child(getString(
                        R.string.table_users
                    )), currentUser?.uid!!, user)

                    Toast.makeText(baseContext, getText(R.string.register_successful),
                        Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, task.exception?.message!!)

                    Toast.makeText(baseContext, getErrorString(task.exception?.message!!),
                        Toast.LENGTH_SHORT).show()
                }
            }

    }

}
