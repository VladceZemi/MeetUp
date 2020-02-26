package cz.brno.mendelu.meetup.activities.account

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.activities.PlaceActivity
import cz.brno.mendelu.meetup.dataclasses.Position
import cz.brno.mendelu.meetup.dataclasses.User
import cz.brno.mendelu.meetup.functionclasses.DataProcessing
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.android.synthetic.main.fragment_account.view.*
import java.util.*

class AccountFragment : Fragment(), ValueEventListener {

    private val TAG: String = "AccountFragment"
    private val PLACE_ACTIVITY = 100

    private lateinit var firebaseServer: FireBaseServer
    private var positionUpdated: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.fragment_account, container, false)

        firebaseServer =
            FireBaseServer(activity!!)
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_users))
            .child(firebaseServer.auth.uid!!)
            .addValueEventListener(this)

        fragmentView.accountAddressDefaultInput.setOnClickListener{
            val intent = Intent(this.activity, PlaceActivity::class.java)
            startActivityForResult(intent, PLACE_ACTIVITY)
        }

        val firebaseReference = firebaseServer.getDatabaseReference()
        val auth = firebaseServer.auth
        val currentUser = auth.currentUser

        fragmentView.accountSaveButton.setOnClickListener{
            val dataProcessing = DataProcessing(activity!!)
            if (dataProcessing.setErrorIfBadPhone(accountPhoneInput)){
                firebaseServer.sendDataToFirebase(firebaseReference
                    .child(getString(R.string.table_users))
                    .child(currentUser?.uid!!), "phone", fragmentView.accountPhoneInput.text.toString())
            }
            if (dataProcessing.setErrorIfEmpty(accountNameInput)){
                firebaseServer.sendDataToFirebase(firebaseReference
                    .child("users")
                    .child(currentUser?.uid!!), "name", fragmentView.accountNameInput.text.toString())
            }
            Toast.makeText(activity, getString(R.string.account_saved), Toast.LENGTH_LONG).show()
        }

        return fragmentView
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_users))
            .child(firebaseServer.auth.uid!!)
            .removeEventListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PLACE_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                data?.let {
                    val latitude: Double = it.getDoubleExtra(getString(R.string.ex_latitude), Double.NaN)
                    val longitude: Double = it.getDoubleExtra(getString(R.string.ex_longitude), Double.NaN)
                    if (!latitude.isNaN() && !longitude.isNaN()) {
                        val position = Position(latitude, longitude)
                        firebaseServer.sendDataToFirebase(firebaseServer.getDatabaseReference()
                            .child(getString(R.string.table_users))
                            .child(firebaseServer.auth.currentUser!!.uid),"position", position)
                        positionUpdated = true
                    }
                }
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(activity, getString(R.string.account_position_cancelled), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        loadingBar.isVisible = false
        val user = dataSnapshot.getValue(User::class.java)
        user?.let { itUser ->
            accountNameInput.setText(itUser.name)
            accountPhoneInput.setText(itUser.phone)
            itUser.position?.let { itPosition ->
                val geocoder = Geocoder(activity, Locale.getDefault())
                val addresses = geocoder.getFromLocation(itPosition.latitude!!, itPosition.longitude!!, 1)
                val address: Address = addresses[0]
                val addressLine: String = address.getAddressLine(0)
                accountAddressDefaultInput.setText(addressLine)
                if (positionUpdated) {
                    Toast.makeText(activity, getString(R.string.account_position_saved), Toast.LENGTH_LONG).show()
                    positionUpdated = false
                }
            }
        }
    }

    override fun onCancelled(p0: DatabaseError) {
        Log.e(TAG, "Error while getting user data")
        Toast.makeText(activity, getText(R.string.account_error), Toast.LENGTH_LONG).show()
    }
}
