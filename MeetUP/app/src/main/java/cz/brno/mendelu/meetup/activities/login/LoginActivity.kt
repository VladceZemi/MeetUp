package cz.brno.mendelu.meetup.activities.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.functionclasses.DataProcessing

import kotlinx.android.synthetic.main.content_login.*

class LoginActivity : AppCompatActivity() {
    private val TAG: String = "LoginActivity"
    private lateinit var firebaseServer: FireBaseServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseServer = FireBaseServer(this)

        logInButton.setOnClickListener{
            this.onLoginClicked()
        }

        registerButton.setOnClickListener {
            intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (firebaseServer.isUserLoggedIn())
            finish()
    }

    private fun onLoginClicked() {
        val dataProcessing = DataProcessing(this)
        if (!dataProcessing.setErrorIfBadEmail(emailInput) ||
            !dataProcessing.setErrorIfEmpty(passwordInput)) {
            return
        }

        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()
        val auth = firebaseServer.auth

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signIn:success")
                    Toast.makeText(baseContext, "Přihlášeno", Toast.LENGTH_LONG)
                        .show()
                    finish()
                } else {
                    Log.w(TAG, "signIn:failure", task.exception)
                    Toast.makeText(baseContext, task.exception?.message!!, Toast.LENGTH_LONG)
                        .show()
                }
            }
    }
}
