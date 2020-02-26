package cz.brno.mendelu.meetup.activities.events

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.dataclasses.Event
import cz.brno.mendelu.meetup.dataclasses.Position
import cz.brno.mendelu.meetup.dataclasses.UserEvent

import kotlinx.android.synthetic.main.activity_new_event_type.*
import kotlinx.android.synthetic.main.content_new_event_type.*
import kotlinx.android.synthetic.main.content_new_event_type.newEventText

class NewEventTypeActivity : AppCompatActivity() {
    private val TAG: String = "RegistrationActivity"
    private lateinit var firebaseServer: FireBaseServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_event_type)
        firebaseServer = FireBaseServer(this)

        val nameOfEvent = intent.getStringExtra(getString(R.string.new_event_name))
        val dateOfEvent = intent.getStringExtra(getString(R.string.new_event_date))
        val timeOfEvent = intent.getStringExtra(getString(R.string.new_event_time))
        val userUids = intent.getSerializableExtra(getString(R.string.new_event_users)) as ArrayList<String>
        val eventAdmin = intent.getStringExtra("eventAdmin")

        var typeOfEvent: String = ""
        var isSelected: Boolean = false

        newEventText.setText(nameOfEvent)


        pubButton.setOnClickListener(){
            pubButton.isSelected = true
            restaurantButton.isSelected = false
            teaHouseButton.isSelected = false
            typeOfEvent = getString(R.string.event_type_one)
            isSelected = true
        }

        restaurantButton.setOnClickListener(){
            pubButton.isSelected = false
            restaurantButton.isSelected = true
            teaHouseButton.isSelected = false
            typeOfEvent = getString(R.string.event_type_two)
            isSelected = true
        }

        teaHouseButton.setOnClickListener(){
            pubButton.isSelected = false
            restaurantButton.isSelected = false
            teaHouseButton.isSelected = true
            typeOfEvent = getString(R.string.event_type_three)
            isSelected = true
        }

        fab_type.setOnClickListener {
            if (nameOfEvent != null && timeOfEvent != null && dateOfEvent!= null && isSelected){
                 this.registerEvent(nameOfEvent, dateOfEvent, timeOfEvent, typeOfEvent, userUids, eventAdmin)
            }
            else{
                Toast.makeText(this,getString(R.string.type_not_selected_error_mesage),Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerEvent(nameOfEvent:String?,dateOfEvent:String?,timeOfEvent:String?,typeOfEvent:String?, userUids: ArrayList<String>?, eventAdmin: String?) {
        val auth = firebaseServer.auth

        val firebaseReference = firebaseServer.getDatabaseReference()

        val dateTime = "$dateOfEvent $timeOfEvent"
        val position = Position(0.0, 0.0)

        val event = Event(nameOfEvent)
        event.datetime = dateTime
        event.type = typeOfEvent
        event.place = position
        event.nameplace = ""
        event.eventAdmin = eventAdmin

        val currentUser = auth.currentUser

        val key = firebaseReference.push().key

        firebaseServer.sendDataToFirebase(firebaseReference.child(getString(R.string.table_events)),key, event)

        // Current user location sending
        firebaseServer.getDatabaseReference().child(getString(R.string.table_users))
            .child(currentUser!!.uid).child(getString(R.string.attribute_user_position)).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val positionSend = dataSnapshot.getValue(Position::class.java)
                    val userSend = Position(positionSend?.latitude!!, positionSend.longitude!!)
                    firebaseServer.sendDataToFirebase(firebaseReference.child(getString(R.string.table_events)).child(key!!).child(getString(R.string.attribute_event_user_location)), currentUser!!.uid, userSend)
                }
            })

        // Friends locations sending
        userUids?.let {
            for (uid in it) {
                firebaseServer.getDatabaseReference().child(getString(R.string.table_users))
                    .child(uid).child(getString(R.string.attribute_user_position)).addListenerForSingleValueEvent(object :
                        ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                        }

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val positionSend = dataSnapshot.getValue(Position::class.java)
                            val userSend = Position(positionSend?.latitude!!, positionSend.longitude!!)
                            firebaseServer.sendDataToFirebase(firebaseReference.child(getString(R.string.table_events)).child(key!!).child(getString(R.string.attribute_event_user_location)), uid, userSend)
                        }
                    })
            }
        }

        // User Events table update
        val userEvent: UserEvent = UserEvent(event.name, event.datetime)
        userEvent.eid = event.eid

        firebaseServer.sendDataToFirebase(firebaseReference.child(getString(R.string.table_user_events)).child(currentUser?.uid!!), key, userEvent)

        userUids?.let {
            for (uid in it) {
                firebaseServer.sendDataToFirebase (
                    firebaseReference
                        .child(getString(R.string.table_user_events))
                        .child(uid),
                    key,
                    userEvent
                )
            }

            Toast.makeText(baseContext, getText(R.string.new_event_successful), Toast.LENGTH_SHORT)
                .show()

            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
