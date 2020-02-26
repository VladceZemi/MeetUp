package cz.brno.mendelu.meetup.dataclasses

class Event(private val nameOfPlace: String? = null) {
    var name: String? = nameOfPlace
    var eid: String? = null
    var nameplace: String? = null
    var datetime: String? = null
    var type: String? = null
    var place: Position? = null
    var usersLocation: HashMap<String, Position>? = HashMap<String, Position>()
    var usersVotes: HashMap<String, String>? = HashMap<String, String>()
    var eventAdmin: String? = null
}