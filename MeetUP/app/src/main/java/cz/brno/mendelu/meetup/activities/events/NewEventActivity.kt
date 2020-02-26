package cz.brno.mendelu.meetup.activities.events


import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent

import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.adapters.UsersAdapter
import cz.brno.mendelu.meetup.dataclasses.User
import cz.brno.mendelu.meetup.functionclasses.DataProcessing
import kotlinx.android.synthetic.main.activity_new_event.*

import kotlinx.android.synthetic.main.content_new_event.*
import kotlinx.android.synthetic.main.content_new_event.contacts_list
import kotlinx.android.synthetic.main.item_event.*


import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList


class NewEventActivity : AppCompatActivity(), ValueEventListener {
    private val TAG: String = "NewEventActivity"
    private val NewEventType = 200

    private val contactsList: MutableList<User> = ArrayList<User>()
    private lateinit var firebaseServer: FireBaseServer

    //For transition between activities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_event)

        contacts_list.adapter =  UsersAdapter(contactsList, true)
        contacts_list.layoutManager = LinearLayoutManager(this)

        firebaseServer = FireBaseServer(this)
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_friends))
            .child(firebaseServer.auth.uid!!)
            .addValueEventListener(this)



        // Calendar
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        datumNewEvent.setOnClickListener{
            val cal = Calendar.getInstance()
            val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener{view: DatePicker, mYear, mMonth, mDay ->
                cal.set(Calendar.DAY_OF_MONTH,mDay)
                cal.set(Calendar.MONTH,mMonth)
                cal.set(Calendar.YEAR,mYear)

                val myFormat = getString(R.string.date_format)

                val sdf = SimpleDateFormat(myFormat, Locale.US)
                datumNewEvent.text = sdf.format(cal.time)
            }, year,month,day)
            dpd.show()
        }

        // Time
        timeNewEvent.setOnClickListener{
            val cal = Calendar.getInstance()

            val tpd = TimePickerDialog.OnTimeSetListener{ view: TimePicker, mHour, mMinute ->
                cal.set(Calendar.HOUR_OF_DAY, mHour)
                cal.set(Calendar.MINUTE, mMinute)
                timeNewEvent.text = SimpleDateFormat(getString(R.string.time_pattern)).format(cal.time)
            }
            TimePickerDialog(this,tpd,cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),true).show()
        }

        floatingButtonSendNewEvent.setOnClickListener{
            val dataProcessing = DataProcessing(this)
            if (dataProcessing.setErrorIfEmpty(nameNewEvent) && dataProcessing.setErrorIfBadDate(datumNewEvent) && dataProcessing.setErrorIfBadTime(timeNewEvent)) {
                val selectedDate = SimpleDateFormat("dd.MM.yyyy/HH:mm").parse(datumNewEvent.text.toString() + '/' + timeNewEvent.text.toString()).compareTo(Date())
                if (selectedDate > 0) {
                    val adapter = contacts_list.adapter as UsersAdapter
                    val uids = ArrayList<String>()
                    for (sel in adapter.selected)
                        uids.add(contactsList[sel].uid!!)

                    val intent = Intent(this,
                        NewEventTypeActivity::class.java)
                    intent.putExtra(getString(R.string.new_event_name), nameNewEvent.text.toString())
                    intent.putExtra(getString(R.string.new_event_date), datumNewEvent.text.toString())
                    intent.putExtra(getString(R.string.new_event_time), timeNewEvent.text.toString())
                    intent.putExtra(getString(R.string.new_event_users), uids)
                    intent.putExtra("eventAdmin", firebaseServer.auth.uid)
                    startActivityForResult(intent, NewEventType)
                }
                else {
                    Toast.makeText(this, "Zvolený čas události je v minulosti", Toast.LENGTH_LONG).show()
                }
            }
            else {
                Toast.makeText(baseContext, getText(R.string.new_event_bad_inputs), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_friends))
            .child(firebaseServer.auth.uid!!)
            .removeEventListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            NewEventType -> {
                if (resultCode == Activity.RESULT_OK) {
                    finish()
                }
            }
        }
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        contactsList.clear()
        for (friendDS in dataSnapshot.children) {
            val friendName: String? = friendDS.getValue(String::class.java)
            friendName?.let {
                val user = User(it)
                user.uid = friendDS.key
                contactsList.add(user)
            }
        }
        contacts_list.adapter?.notifyDataSetChanged()
    }

    override fun onCancelled(dataSnapshot: DatabaseError) {
        Log.e(TAG, getString(R.string.new_event_contacts_list_error))
        Toast.makeText(this, getText(R.string.cons_error), Toast.LENGTH_LONG).show()
    }


}
