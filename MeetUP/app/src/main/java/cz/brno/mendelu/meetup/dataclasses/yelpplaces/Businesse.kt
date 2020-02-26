package cz.brno.mendelu.meetup.dataclasses.yelpplaces


import com.google.gson.annotations.SerializedName

data class Businesse(
    val id: String,
    val alias: String,
    val name: String,
    @SerializedName("image_url")
    val imageUrl: String,
    @SerializedName("is_closed")
    val isClosed: Boolean,
    val url: String,
    @SerializedName("review_count")
    val reviewCount: Int,
    val categories: List<Category>,
    val rating: Double,
    val coordinates: Coordinates,
    val transactions: List<Any>,
    val location: Location,
    val phone: String,
    @SerializedName("display_phone")
    val displayPhone: String,
    val distance: Double,
    val price: String
)