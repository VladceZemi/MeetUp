package cz.brno.mendelu.meetup.dataclasses.placecandidate


import cz.brno.mendelu.meetup.dataclasses.placecandidate.Northeast
import cz.brno.mendelu.meetup.dataclasses.placecandidate.Southwest
import java.io.Serializable

data class Viewport(
    val northeast: Northeast,
    val southwest: Southwest
) : Serializable