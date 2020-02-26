package cz.brno.mendelu.meetup.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import cz.brno.mendelu.meetup.R
import kotlinx.android.synthetic.main.content_place.*
import java.util.*


class PlaceActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener{

    private val TAG = "PlaceActivity"
    private val MY_PERMISSION_REQUEST_FINE_LOCATION = 0

    private var googleMap: GoogleMap? = null
    var place: LatLng? = null

    private fun zoomToLocation(toUserLocation: Boolean) {
        if (toUserLocation){
            val client = LocationServices.getFusedLocationProviderClient(this)
            client.lastLocation.addOnSuccessListener {
                if (it != null){
                    val location = CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude,it.longitude), 12F)
                    googleMap?.animateCamera(location)
                }
            }
        }
        else {
            val location = CameraUpdateFactory.newLatLngZoom(LatLng(49.1952550,16.6086069), 11F)
            googleMap?.animateCamera(location)
        }
    }

    override fun onMapReady(p0: GoogleMap?) {
        googleMap = p0

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
                    googleMap?.isMyLocationEnabled = true
                    zoomToLocation(true)
            }
            else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_REQUEST_FINE_LOCATION)
            }

        googleMap?.setOnMapClickListener {
            googleMap?.clear()
            val pos = LatLng(it.latitude, it.longitude)
            val markerOptions = MarkerOptions()
            markerOptions.position(pos)

            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
            if (addresses.size == 0) {
                Toast.makeText(this,"Jasně :)", Toast.LENGTH_SHORT).show()
                placeButton.isInvisible = true
                place = null
            }
            else {
                val address: Address = addresses.get(0)
                val addressLine: String = address.getAddressLine(0)
                markerOptions.title(addressLine)
                place = it
                googleMap?.addMarker(markerOptions)?.showInfoWindow()
                placeButton.isInvisible = false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSION_REQUEST_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    zoomToLocation(true)
                    googleMap?.isMyLocationEnabled = true
                } else {
                    zoomToLocation(false)
                }
                return
            }
            else -> {

            }
        }
    }

    override fun onMapClick(p0: LatLng?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_place)
        val mapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(
            R.id.map
        ) as SupportMapFragment
        mapFragment.getMapAsync(this)

        placeButton.setOnClickListener { view ->
            if (place != null){
                setResult(Activity.RESULT_OK, bundlePlace())
                finish()
            }
            else{
                val client = LocationServices.getFusedLocationProviderClient(this)
                client.lastLocation.addOnSuccessListener {
                    if (it != null) {
                        place = LatLng(it.latitude, it.longitude)
                        setResult(Activity.RESULT_OK, bundlePlace())
                        finish()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun bundlePlace(): Intent {
        val returnIntent = Intent()
        returnIntent.putExtra(getString(R.string.ex_latitude), place!!.latitude)
        returnIntent.putExtra(getString(R.string.ex_longitude), place!!.longitude)
        return returnIntent
    }
}
