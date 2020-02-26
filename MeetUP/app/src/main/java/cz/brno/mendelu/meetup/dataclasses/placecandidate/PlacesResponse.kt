package cz.brno.mendelu.meetup.dataclasses.placecandidate


import cz.brno.mendelu.meetup.dataclasses.placecandidate.Candidate

data class PlacesResponse(
    val candidates: List<Candidate>,
    val status: String
)