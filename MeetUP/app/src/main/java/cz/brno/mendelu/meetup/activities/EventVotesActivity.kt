package cz.brno.mendelu.meetup.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.adapters.PlacesAdapter
import cz.brno.mendelu.meetup.dataclasses.PlaceVotes
import cz.brno.mendelu.meetup.dataclasses.Position
import cz.brno.mendelu.meetup.dataclasses.placecandidate.Candidate
import kotlinx.android.synthetic.main.content_event_votes.*

class EventVotesActivity : AppCompatActivity(), ValueEventListener, OnMapReadyCallback {


    private val TAG: String = "EventVotesActivity"

    private var placeVotesList: MutableList<PlaceVotes> = ArrayList<PlaceVotes>()
    private lateinit var firebaseServer: FireBaseServer
    private lateinit var googleMap: GoogleMap
    private val extraMarkerInfo: HashMap<String, String> = HashMap<String, String>()
    private var placeCandidatesList: MutableList<Candidate> = ArrayList<Candidate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_votes)

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(
            R.id.votesMap
        ) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val eid = intent.getStringExtra("eid")
        val nameOfEvent = intent.getStringExtra("nameEventForVotes")
        val dateTimeOfEvent = intent.getStringExtra("dateTimeEventForVotes")
        val countOfUsers = intent?.getIntExtra( "countOfUsers",0)
        placeCandidatesList = intent.getSerializableExtra("placeCandidatesList") as ArrayList<Candidate>
        placeVotesList = intent.getSerializableExtra("placeVotesUnBundle") as ArrayList<PlaceVotes>

        eventVotes.adapter = PlacesAdapter(placeVotesList,countOfUsers!!)
        eventVotes.layoutManager = LinearLayoutManager(this)

        eventNameVotes.text = nameOfEvent
        eventDateVotes.text = dateTimeOfEvent

        firebaseServer = FireBaseServer(this)

        firebaseServer.getDatabaseReference()
            .child("events")
            .child(eid!!)
            .addValueEventListener(this)
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        if (dataSnapshot.value == null){
            finish()
            return
        }

        val countOfVotes = dataSnapshot.child("usersVotes").childrenCount
        val adapter: PlacesAdapter = eventVotes.adapter as PlacesAdapter
        adapter.countOfUsers = countOfVotes.toInt()

        val userVotesSnapshot = dataSnapshot.child("usersVotes")

        placeVotesList.forEach {
            it.votesCount = 0
        }

        var maxVotes: Int = -1
        eventVotedPlace.text = ""
        val bestPlaces = ArrayList<PlaceVotes>()
        for (votesDS in userVotesSnapshot.children) {
            for (placeVotes in placeVotesList) {
                if (placeVotes.placeId.toString() == votesDS.value.toString()) {
                    placeVotes.votesCount = placeVotes.votesCount?.plus(1)
                    if (placeVotes.votesCount!! >= maxVotes) {
                        maxVotes = placeVotes.votesCount!!
                    }
                    break
                }
            }
        }

        for (votesDS in userVotesSnapshot.children) {
            for (placeVotes in placeVotesList) {
                if (placeVotes.votesCount!! == maxVotes)
                    bestPlaces.add(placeVotes)
            }
        }

        val sorted = bestPlaces.sortedBy { it.name }.firstOrNull()
        eventVotedPlace.text = sorted?.name
        eventVotes.adapter?.notifyDataSetChanged()

        for (candidate in placeCandidatesList){
            if (candidate.placeId == sorted?.placeId){
                val pos = Position(candidate.geometry.location.lat, candidate.geometry.location.lng)
                zoomToLocation(pos, 15F)

                val markerOptions = MarkerOptions()
                markerOptions
                    .position(LatLng(candidate.geometry.location.lat, candidate.geometry.location.lng))
                    .title(candidate.name)
                    .snippet("Hodnocení: " + candidate.rating.toString())

                googleMap.clear()
                val marker: Marker = googleMap.addMarker(markerOptions)
                marker.showInfoWindow()
            }
        }
    }

    override fun onCancelled(dataSnapshot: DatabaseError) {}

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map!!
    }

    private fun zoomToLocation(locationToZoom: Position, zoomIndex: Float) {
        val location = CameraUpdateFactory.newLatLngZoom(LatLng(locationToZoom.latitude!!, locationToZoom.longitude!!), zoomIndex)
        googleMap.animateCamera(location)
    }

}
