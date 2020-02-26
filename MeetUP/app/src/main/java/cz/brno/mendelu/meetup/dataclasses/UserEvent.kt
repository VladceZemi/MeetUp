package cz.brno.mendelu.meetup.dataclasses

data class UserEvent(
    var eventName: String? = null,
    var eventDate: String? = null
) {
    var eid: String? = null
}