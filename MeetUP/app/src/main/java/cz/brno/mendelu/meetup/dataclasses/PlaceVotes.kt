package cz.brno.mendelu.meetup.dataclasses

import java.io.Serializable

class PlaceVotes (var name: String?=null, var placeId: String? = null) : Serializable {
    var votesCount : Int? = 0
}