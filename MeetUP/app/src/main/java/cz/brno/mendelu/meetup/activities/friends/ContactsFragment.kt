package cz.brno.mendelu.meetup.activities.friends

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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.adapters.UsersAdapter
import cz.brno.mendelu.meetup.dataclasses.User
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_contacts.view.*

class ContactsFragment : Fragment(), ValueEventListener, AddContactDialog.AddContactDialogInterface {
    private val TAG: String = "ContacsFragment"

    private lateinit var firebaseServer: FireBaseServer
    private val contactsList: MutableList<User> = ArrayList<User>()

    private var lastRemovedItem = User()
    private var lastRemovedPosition: Int = 0
    private lateinit var deleteIcon: Drawable
    private lateinit var swipeBackground: ColorDrawable


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val fragmentView = inflater.inflate(R.layout.fragment_contacts, container, false)
        fragmentView.contacts_list.adapter = UsersAdapter(contactsList, false)
        fragmentView.contacts_list.layoutManager = LinearLayoutManager(activity)

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
                this@ContactsFragment.removeItem(viewHolder, fragmentView)
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
        itemTouchHelper.attachToRecyclerView(fragmentView.contacts_list)

        firebaseServer = FireBaseServer(activity!!)
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_friends))
            .child(firebaseServer.auth.uid!!)
            .addValueEventListener(this)

        return fragmentView
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_friends))
            .child(firebaseServer.auth.uid!!)
            .removeEventListener(this)
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        loadingBar.isVisible = false
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
        Log.e(TAG, "Error while getting contacts list")
        Toast.makeText(activity, getText(R.string.cons_error), Toast.LENGTH_LONG).show()
    }

    fun removeItem(viewHolder: RecyclerView.ViewHolder, fragmentView: View){
        val databaseReference = firebaseServer.getDatabaseReference().child("friends").child(firebaseServer.auth.uid!!)
        lastRemovedPosition = viewHolder.adapterPosition
        lastRemovedItem = contactsList[viewHolder.adapterPosition]

        firebaseServer.removeDataFromFireBase(databaseReference, lastRemovedItem.uid!!)

        Snackbar.make(viewHolder.itemView, "Kontakt ${lastRemovedItem.name} byl odstraněn.", Snackbar.LENGTH_LONG)
            .setAction("ZPĚT"){
                contactsList.add(lastRemovedPosition, lastRemovedItem)
                fragmentView.contacts_list.adapter!!.notifyItemInserted(lastRemovedPosition)
                firebaseServer.sendDataToFirebase(databaseReference, lastRemovedItem.uid!!, lastRemovedItem.name!!)
            }.show()

        contactsList.removeAt(viewHolder.adapterPosition)
        fragmentView.contacts_list.adapter!!.notifyItemRemoved(viewHolder.adapterPosition)
    }

    override fun addContact(email: String) {
        // Find if user exists using his email
        val userDatabaseReference: Query = firebaseServer.getDatabaseReference()
            .child("users")
            .orderByChild("email")
            .equalTo(email)

        userDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onDataChange(usersQuery: DataSnapshot) {
                var key: String? = null
                for ( user in usersQuery.children)
                    key = user.key

                if (key != firebaseServer.auth.uid) {
                    if (key != null) {
                        firebaseServer.getDatabaseReference()
                            .child(getString(R.string.table_users))
                            .child(key)
                            .addListenerForSingleValueEvent(object: ValueEventListener {
                                override fun onCancelled(p0: DatabaseError) {}

                                override fun onDataChange(user: DataSnapshot) {
                                    firebaseServer.sendDataToFirebase(
                                        firebaseServer.getDatabaseReference()
                                            .child(getString(R.string.table_friends))
                                            .child(firebaseServer.auth.currentUser!!.uid), key, user.child("name").getValue(String::class.java)!!)
                                }
                            })
                    }
                    else {
                        Toast.makeText(this@ContactsFragment.activity, "Uživatel nenalezen", Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    Toast.makeText(this@ContactsFragment.activity, "To jsi ty", Toast.LENGTH_LONG).show()
                }
            }

        })
    }
}
