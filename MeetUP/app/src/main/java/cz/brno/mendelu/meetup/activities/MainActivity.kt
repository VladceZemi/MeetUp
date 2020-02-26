package cz.brno.mendelu.meetup.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.activities.account.AccountFragment
import cz.brno.mendelu.meetup.activities.events.EventsFragment
import cz.brno.mendelu.meetup.activities.events.NewEventActivity
import cz.brno.mendelu.meetup.activities.friends.AddContactDialog
import cz.brno.mendelu.meetup.activities.friends.ContactsFragment
import cz.brno.mendelu.meetup.activities.login.LoginActivity

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val ADD_CONTACT_DIALOG = "ADD_CONTACT_DIALOG"

    public var currentFragment: Fragment = Fragment()

    private var currentFragmentId: Int = R.id.events
    private lateinit var firebaseServer: FireBaseServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNavigation: BottomNavigationView = findViewById(R.id.btm_nav)

        firebaseServer = FireBaseServer(this)

        if (!firebaseServer.isUserLoggedIn()) {
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        fab.setImageDrawable(getDrawable(R.drawable.ic_event_white))

        swapFragment(EventsFragment())

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            currentFragmentId = item.itemId
            when (item.itemId) {
               R.id.events -> {
                    fab.show()
                    fab.setImageDrawable(getDrawable(R.drawable.ic_event_white))
                    swapFragment(EventsFragment())
               }
               R.id.contacts -> {
                    fab.show()
                    fab.setImageDrawable(getDrawable(R.drawable.ic_contacts_white))
                    swapFragment(ContactsFragment())
               }
               R.id.account -> {
                    fab.hide()
                    swapFragment(AccountFragment())
               }
            }
            true
        }

        fab.setOnClickListener {
            when (currentFragmentId) {
                R.id.events -> {
                    val intent: Intent = Intent(this, NewEventActivity::class.java)
                    startActivity(intent)
                }
                R.id.contacts -> {
                    val addContactDialog =
                        AddContactDialog()
                    addContactDialog.show(supportFragmentManager, ADD_CONTACT_DIALOG)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!firebaseServer.isUserLoggedIn()) {
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btm_nav -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun swapFragment(newFragment: Fragment) {
        currentFragment = newFragment
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.frame_layout,
                currentFragment
            )
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }
}
