package cz.brno.mendelu.meetup.dataclasses.placecandidate


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Candidate(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("formatted_address")
    val formattedAddress: String,
    val geometry: Geometry,
    val name: String,
    val rating: Double
) : Serializable