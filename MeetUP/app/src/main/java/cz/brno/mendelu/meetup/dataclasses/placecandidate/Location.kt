package cz.brno.mendelu.meetup.dataclasses.placecandidate


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Location(
    val lat: Double,
    val lng: Double
) : Serializable