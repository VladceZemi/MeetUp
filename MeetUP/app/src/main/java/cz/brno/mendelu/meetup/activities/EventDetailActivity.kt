package cz.brno.mendelu.meetup.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import cz.brno.mendelu.meetup.FireBaseServer
import cz.brno.mendelu.meetup.R
import cz.brno.mendelu.meetup.adapters.UsersAdapter
import cz.brno.mendelu.meetup.dataclasses.Event
import cz.brno.mendelu.meetup.dataclasses.PlaceVotes
import cz.brno.mendelu.meetup.dataclasses.Position
import cz.brno.mendelu.meetup.dataclasses.User
import cz.brno.mendelu.meetup.dataclasses.placecandidate.*
import cz.brno.mendelu.meetup.dataclasses.yelpplaces.Businesse
import cz.brno.mendelu.meetup.functionclasses.YelpAPI
import kotlinx.android.synthetic.main.activity_event_detail.*
import kotlinx.android.synthetic.main.content_event_detail.*
import kotlinx.coroutines.*
import kotlin.collections.ArrayList

class EventDetailActivity : AppCompatActivity(), ValueEventListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {

    private val YELP_API_KEY = "JPhk4rLDDBWMJP1ACqhP39fGu9Vya6xmQ9n8sje4-RqCY-JoKKEkzYFeF_52qigu5Dab5Vxu3t_keQRkDqlFqBlB1A6b2EKWHd5rv0X1mG235EAb1h-SdjCbjtLcXnYx" // YELP API KEY

    private val TAG: String = "EventDetailActivity"

    private lateinit var firebaseServer: FireBaseServer
    private lateinit var googleMap: GoogleMap
    private val eventUsersList: MutableList<User> = ArrayList<User>()
    private val extraMarkerInfo: HashMap<String, String> = HashMap<String, String>()

    private var event = Event()

    private var placeVotesList  = ArrayList<PlaceVotes>()

    private var searchCorutine: Job? = null
    private var SEARCH_RADIUS = 200
    private var zoomIndex = 15F

    private var placeCandidatesList = ArrayList<Candidate>()        // List podniků, třída je podle JSONu z google API places

    private lateinit var dialogLoading: AlertDialog                 // Dialogové okno pro načítání z API
    private lateinit var dialogNothingFound: AlertDialog            // Dialogové okno, které se zobrazí když není nalezen žádný podnik v okruhu menší než 2000 metrů

    override fun onCreate(savedInstanceState: Bundle?) {
        val builderLoading = AlertDialog.Builder(this)      // Init builderu pro načítačí dialog
        val builderNothingFound = AlertDialog.Builder(this)     // Init builderu pro dialog, který se zobrazí když nejsou nalezeny popdniky

        var inflater: LayoutInflater = this.layoutInflater          // Inflater, který vloží do dialogu požadovaný layout

        builderLoading.setView(inflater.inflate(R.layout.progress_dialog, null))    //Vložení layoutu načítacího dialogu
        builderLoading.setCancelable(false)                                               //Nastavení zrušení dialogu na false -> nejde zrušit

        builderNothingFound.setView(inflater.inflate(R.layout.nothing_found_dialog,null))   //Vložení layoutu dialogu pro nenalezení podniků
        builderNothingFound.setCancelable(true)                                                  //Nastavení zrušení dialogu na trure -> lze zrušit uživatelem

        dialogLoading = builderLoading.create()
        dialogLoading.show() //Zobrazení načítacího dialogu

        dialogNothingFound = builderNothingFound.create()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(
            R.id.eventMap
        ) as SupportMapFragment
        mapFragment.getMapAsync(this)

        eventUsers.adapter =  UsersAdapter(eventUsersList, false)
        eventUsers.layoutManager = LinearLayoutManager(this)

        firebaseServer = FireBaseServer(this)

        fab.setOnClickListener { view ->
            val intentToVotes = Intent(this, EventVotesActivity::class.java)
            intentToVotes.putExtra("eid", intent.getStringExtra("eid"))
            intentToVotes.putExtra("nameEventForVotes", event.name)
            intentToVotes.putExtra("dateTimeEventForVotes", event.datetime?.replace("-", ". "))
            intentToVotes.putExtra("countOfUsers", eventUsersList.size)
            intentToVotes.putExtra("placeVotesUnBundle", placeVotesList)
            intentToVotes.putExtra("placeCandidatesList", placeCandidatesList)
            startActivity(intentToVotes)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_events))
            .child(event.eid!!)
            .removeEventListener(this)
    }

    override fun onCancelled(p0: DatabaseError) {
        Log.e(TAG, "Error while getting event data")
        Toast.makeText(this, getText(R.string.account_error), Toast.LENGTH_LONG).show()
    }

    override fun onDataChange(dataSnapshot: DataSnapshot) {
        voteButton.visibility = View.INVISIBLE
        placeVotesList.clear()

        if (dataSnapshot.value== null){
            Toast.makeText(this, "Událost byla smazána správcem", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        event = dataSnapshot.getValue(Event::class.java)!!
        event.eid = dataSnapshot.key
        val dateTime = event.datetime!!.replace("-", ". ")
        eventName.text = event.name
        eventDate.text = dateTime

        // val apiService = PlacesAPI()
        val yelpSevise = YelpAPI() //Třída pro YELP API interface, který provádí komunikaci s YELP API

        val centerPoint = calculateCenterPoint(event.usersLocation!!)
        //val locationBias = "circle:${SEARCH_RADIUS}@${centerPoint.latitude},${centerPoint.longitude}" // proměnná pro places google API

        if (searchCorutine != null)
            return

        searchCorutine = GlobalScope.launch(Dispatchers.Main) {
            //val places = apiService.getPlaces(type = event.type!!, location = locationBias).await()
            val yelpPlaces = yelpSevise.getPlaces("Bearer $YELP_API_KEY",centerPoint.latitude,centerPoint.longitude,SEARCH_RADIUS,event.type!!).await() //Načtení podniků z YELP API
            var candidates = returnCandidates(yelpPlaces.businesses) //Předělání z YELP API tříd do PLACES API tříd

            while (candidates.size < 5 && SEARCH_RADIUS < 2000) {
                SEARCH_RADIUS += 200
                zoomIndex -= 0.4.toFloat()
                val yelpPlaces = yelpSevise.getPlaces("Bearer $YELP_API_KEY",centerPoint.latitude,centerPoint.longitude,SEARCH_RADIUS,event.type!!).await() //Načtení podniků z YELP API
                candidates = returnCandidates(yelpPlaces.businesses) //Předělání z YELP API tříd do PLACES API tříd
            }

            if (SEARCH_RADIUS >= 2000 && candidates.isEmpty()) {
                dialogNothingFound.show()  //Zobrazení dialogu, když nejsou nalezeny žádné popdniky
            }

            dialogLoading.dismiss() //Zrušení načítacího dialogu
            googleMap.clear()
            drawCenterCircle(centerPoint)
            zoomToLocation(centerPoint, zoomIndex)
            drawUsersPosition(event.usersLocation!!)

            extraMarkerInfo.clear()
            for (candidate in candidates) {
                setMarker(candidate, event)
                placeVotesList.add(PlaceVotes(candidate.name,candidate.placeId))
                placeCandidatesList.add(candidate)
            }

            var maxVotes = -1
            eventPlace.text = ""
            val bestPlaces = ArrayList<PlaceVotes>()
            bestPlaces.clear()
            event.usersVotes?.forEach {
                for (placeVotes in placeVotesList) {
                    if (placeVotes.placeId.toString() == it.value) {
                        placeVotes.votesCount = placeVotes.votesCount?.plus(1)
                        if (placeVotes.votesCount!! >= maxVotes) {
                            maxVotes = placeVotes.votesCount!!
                        }
                        break
                    }
                }
            }

            for (placeVotes in placeVotesList){
                if (placeVotes.votesCount!! == maxVotes)
                    bestPlaces.add(placeVotes)
            }

            val sorted = bestPlaces.sortedBy { it.name }.firstOrNull()
            eventPlace.text = sorted?.name
            searchCorutine = null
        }

        // Gather users participating in event
        event.let {
            val usersLocation = event.usersLocation
            eventUsersList.clear()
            usersLocation?.forEach {
                firebaseServer
                    .getDatabaseReference()
                    .child(getString(R.string.table_users))
                    .child(it.key)
                    .addListenerForSingleValueEvent( object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(userDS: DataSnapshot) {
                        val user = userDS.getValue(User::class.java)
                        eventUsersList.add(user!!)
                        eventUsers.adapter?.notifyDataSetChanged()
                    }
                })
            }
        }

        //Log.e(TAG, event.usersVotes.toString())

    }

    override fun onMapReady(map: GoogleMap?) {
        voteButton.visibility = View.INVISIBLE
        googleMap = map!!
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener(this)
        val eid = intent.getStringExtra("eid")!!
        firebaseServer.getDatabaseReference()
            .child(getString(R.string.table_events))
            .child(eid)
            .addValueEventListener(this)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        val placeId = extraMarkerInfo[marker?.id]

        marker?.showInfoWindow()
        voteButton.visibility = View.VISIBLE
        voteButton.text = "Hlasovat pro: " + marker?.title

        voteButton.setOnClickListener{
            firebaseServer.sendDataToFirebase(
                firebaseServer.getDatabaseReference()
                    .child(getString(R.string.table_events))
                    .child(intent.getStringExtra("eid")!!)
                    .child("usersVotes"),
                firebaseServer.auth.currentUser!!.uid,
                placeId!!
            )
        }
        return true
    }


    private fun calculateCenterPoint(positions: Map<String, Position>): Position {
        var sumLatitude = 0.0
        var sumLongitude = 0.0

        positions.forEach {
            sumLatitude += it.value.latitude!!
            sumLongitude += it.value.longitude!!
        }

        sumLatitude /= positions.size
        sumLongitude /= positions.size

        return Position(sumLatitude, sumLongitude)
    }

    private fun zoomToLocation(locationToZoom: Position, zoomIndex: Float) {
        val location = CameraUpdateFactory.newLatLngZoom(LatLng(locationToZoom.latitude!!, locationToZoom.longitude!!), zoomIndex)
        googleMap.animateCamera(location)
    }

    private fun setMarker(candidate: Candidate, event: Event) {
        val location = Position(candidate.geometry.location.lat, candidate.geometry.location.lng)

        val markerOptions = MarkerOptions()
        markerOptions
            .position(LatLng(location.latitude!!, location.longitude!!))
            .title(candidate.name)
            .snippet("Hodnocení: " + candidate.rating.toString())

        val marker: Marker = googleMap.addMarker(markerOptions)
        extraMarkerInfo[marker.id] = candidate.placeId
    }

    private fun drawCenterCircle(location: Position) {
        val circleOptions = CircleOptions()
        circleOptions
            .center(LatLng(location.latitude!!, location.longitude!!))
            .radius(SEARCH_RADIUS.toDouble() * 1.6)
            .strokeWidth(0f)
            .fillColor(0x400192CB)
        googleMap.addCircle(circleOptions)
    }

    private fun drawUsersPosition(positions: Map<String, Position>) {
        positions.forEach {
            val circleOptions = CircleOptions()
            circleOptions
                .center(LatLng(it.value.latitude!!, it.value.longitude!!))
                .radius(10.0)
                .strokeWidth(5f)
                .fillColor(0x55FF0000)
            googleMap.addCircle(circleOptions)
        }
    }

    private fun fromBussinesToCandidate(businesse: Businesse): Candidate {
        val formatedAddress = businesse.location.address1 + ", " + businesse.location.zipCode + " " + businesse.location.city

        val northeast = Northeast(businesse.coordinates.latitude,businesse.coordinates.longitude)
        val southwest = Southwest(businesse.coordinates.latitude,businesse.coordinates.longitude)
        val viewport = Viewport(northeast,southwest)

        val location = cz.brno.mendelu.meetup.dataclasses.placecandidate.Location(businesse.coordinates.latitude,businesse.coordinates.longitude)
        val geometry = Geometry(location, viewport)

        val candidate = Candidate(businesse.id,formatedAddress,geometry, businesse.name, businesse.rating)

        return candidate
    }

    private fun returnCandidates(listOfBusinesse: List<Businesse>): List<Candidate> {
        val listOfCandidates: MutableList<Candidate> = ArrayList()

        for(businesses in listOfBusinesse){
            listOfCandidates.add(fromBussinesToCandidate(businesses))
        }

        val sortedCandidates = listOfCandidates.sortedBy { it.rating }

        return sortedCandidates.takeLast(5)
    }

    override fun onMapClick(p0: LatLng?) {
          voteButton.visibility = View.INVISIBLE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
