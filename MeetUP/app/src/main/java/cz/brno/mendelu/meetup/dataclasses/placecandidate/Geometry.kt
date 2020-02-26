package cz.brno.mendelu.meetup.dataclasses.placecandidate

import java.io.Serializable


data class Geometry(
    val location: Location,
    val viewport: Viewport?
) : Serializable