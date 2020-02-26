package cz.brno.mendelu.meetup.dataclasses

data class User(var name: String? = null) {
    var email: String? = null
    var phone: String? = null
    var picturePath: String? = null
    var uid: String? = null
    var position: Position? = null
}