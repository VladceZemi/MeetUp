package cz.brno.mendelu.meetup.activities.events

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.adapters.UserEventsAdapter
import cz.brno.mendelu.meetup.dataclasses.Event
import cz.brno.mendelu.meetup.dataclasses.User
import cz.brno.mendelu.meetup.dataclasses.UserEvent
import kotlinx.android.synthetic.main.fragment_events.*
import kotlinx.android.synthetic.main.fragment_events.view.*
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

class EventsFragment : Fragment(), ValueEventListener {

    private val TAG: String = "EventsFragment"
    private val eventsList: SortedList<UserEvent> = SortedList<UserEvent>(UserEvent::class.java, UserEventComparator())
    private lateinit var firebaseServer: FireBaseServer

    private var removedEvent = User()
    private var lastRemovedPosition: Int = 0
    private lateinit var deleteIcon: Drawable
    private lateinit var swipeBackground: ColorDrawable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.fragment_events, container, false)
        fragmentView.events_list.adapter = UserEventsAdapter(eventsList)
        fragmentView.events_list.layoutManager = LinearLayoutManager(activity)

        deleteIcon = ContextCompat.getDrawable(fragmentView.context, R.drawable.ic_delete)!!
        swipeBackground = ColorDrawable(ContextCompat.getColor(fragmentView.context, R.color.swipe_remove_background))

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, position: Int) {
                this@EventsFragment.removeItem(viewHolder, fragmentView)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight)/2
                swipeBackground.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                deleteIcon.setBounds(itemView.left + iconMargin,
                    itemView.top + iconMargin,
                    itemView.left + iconMargin + deleteIcon.intrinsicWidth,
                    itemView.bottom - iconMargin)
                swipeBackground.draw(c)
                c.save()
                c.clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
                c.restore()
                deleteIcon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(fragmentView.events_list)

        firebaseServer =
            FireBaseServer(activity!!)
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_user_events))
            .child(firebaseServer.auth.uid!!)
            .addValueEventListener(this)

        return fragmentView
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_user_events))
            .child(firebaseServer.auth.uid!!)
            .removeEventListener(this)
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        loadingBar.visibility = View.GONE
        eventsList.clear()
        for (eventDS in dataSnapshot.children) {
            val userEvent: UserEvent? = eventDS.getValue(UserEvent::class.java)
            userEvent?.let {
                userEvent.eid = eventDS.key
                eventsList.add(userEvent)
            }
        }
        events_list.adapter?.notifyDataSetChanged()
    }

    override fun onCancelled(dataSnapshot: DatabaseError) {
        Log.e(TAG,  getString(R.string.new_event_contacts_list_error))
        Toast.makeText(activity, "Nepodařilo se načíst události", Toast.LENGTH_LONG).show()
    }

    fun removeItem(viewHolder: RecyclerView.ViewHolder, fragmentView: View){
        val removedEvent = eventsList[viewHolder.adapterPosition]
        val eventUsersIds = ArrayList<String>()

        firebaseServer
            .getDatabaseReference()
            .child("events")
            .child(removedEvent.eid!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                }
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val event = dataSnapshot.getValue(Event::class.java)

                    if (event?.eventAdmin == firebaseServer.auth.uid){
                        //Logged in user is creator of event
                        event?.usersLocation?.forEach {
                            eventUsersIds.add(it.key)
                        }

                        eventUsersIds.forEach{
                            val databaseReference = firebaseServer
                                .getDatabaseReference()
                                .child("userEvents")
                                .child(it)
                            firebaseServer.removeDataFromFireBase(databaseReference, removedEvent.eid!!)
                        }

                        val eventToRemoveReference = firebaseServer
                            .getDatabaseReference()
                            .child("events")

                        val archivedEventsReference = firebaseServer
                            .getDatabaseReference()
                            .child("archivedEvents")

                        val key = archivedEventsReference.push().key

                        firebaseServer
                            .getDatabaseReference()
                            .child("events")
                            .child(removedEvent.eid!!)
                            .addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onCancelled(p0: DatabaseError) {
                                }

                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val eventToArchive = dataSnapshot.getValue(Event::class.java)
                                    firebaseServer.sendDataToFirebase(archivedEventsReference, key, eventToArchive!!)
                                }
                            })

                        firebaseServer.removeDataFromFireBase(eventToRemoveReference, removedEvent.eid!!)

                        eventsList.removeItemAt(viewHolder.adapterPosition)
                        fragmentView.events_list.adapter!!.notifyItemRemoved(viewHolder.adapterPosition)

                        Toast.makeText(this@EventsFragment.activity, "Událost byla odstraněna", Toast.LENGTH_LONG)
                            .show()
                    }
                    else{
                        //Logged in user is NOT creator of event
                        val userEventsReference = firebaseServer
                            .getDatabaseReference()
                            .child("userEvents")
                            .child(firebaseServer.auth.uid!!)

                        firebaseServer.removeDataFromFireBase(userEventsReference, removedEvent.eid!!)

                        firebaseServer
                            .getDatabaseReference()
                            .child("events")
                            .child(removedEvent.eid!!)
                            .addListenerForSingleValueEvent(object : ValueEventListener{
                                override fun onCancelled(p0: DatabaseError) {
                                }

                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val event = dataSnapshot.getValue(Event::class.java)

                                    event?.usersLocation?.forEach{
                                        if (it.key == firebaseServer.auth.uid){
                                            val usersLocationReference = firebaseServer
                                                .getDatabaseReference()
                                                .child("events")
                                                .child(removedEvent.eid!!)
                                                .child("usersLocation")

                                            val usersVotesReference = firebaseServer
                                                .getDatabaseReference()
                                                .child("events")
                                                .child(removedEvent.eid!!)
                                                .child("usersVotes")

                                            firebaseServer.removeDataFromFireBase(usersLocationReference, it.key)
                                            firebaseServer.removeDataFromFireBase(usersVotesReference, it.key)
                                        }
                                    }
                                }

                            })

                        Toast.makeText(this@EventsFragment.activity, "Odebral jsi se z události", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } )
    }

    inner class UserEventComparator: SortedList.Callback<UserEvent>() {

        override fun areItemsTheSame(item1: UserEvent, item2: UserEvent): Boolean {
            return item1 === item2
        }

        override fun areContentsTheSame(oldItem: UserEvent, newItem: UserEvent): Boolean {
            return oldItem.eid == newItem.eid
        }

        override fun compare(o1: UserEvent, o2: UserEvent): Int {
            val o1Date = SimpleDateFormat("dd.MM.yyyy HH:mm").parse(o1.eventDate!!)
            val o2Date = SimpleDateFormat("dd.MM.yyyy HH:mm").parse(o2.eventDate!!)
            return o1Date.compareTo(o2Date)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {}

        override fun onChanged(position: Int, count: Int) {}

        override fun onInserted(position: Int, count: Int) {}

        override fun onRemoved(position: Int, count: Int) {}
    }
}
