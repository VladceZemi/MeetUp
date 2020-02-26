package cz.brno.mendelu.meetup

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import cz.brno.mendelu.meetup.R


class FireBaseServer(context : Context) {

    private var databaseReference: DatabaseReference
    private var authReference: FirebaseAuth

    public val auth: FirebaseAuth
        get() = this.authReference

    init {
        FirebaseApp.initializeApp(context)

        databaseReference = FirebaseDatabase.getInstance()
            .getReferenceFromUrl(context.getString(R.string.database_address))
            .child(context.getString(R.string.database_version))
        databaseReference.keepSynced(true)

        authReference = FirebaseAuth.getInstance()
    }


    fun getDatabaseReference(): DatabaseReference {
        return databaseReference
    }

    fun sendDataToFirebase(reference: DatabaseReference, key: String?, objectToSend: Any) {
        var firebaseKey: String? = null

        firebaseKey = key ?: reference.push().key
        reference.child(firebaseKey!!).setValue(objectToSend)
    }

    fun removeDataFromFireBase(reference: DatabaseReference, key: String) {
        reference.child(key).removeValue()
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}