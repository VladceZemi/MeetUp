package cz.brno.mendelu.meetup.dataclasses.yelpplaces


import com.google.gson.annotations.SerializedName

data class YelpAPIResponse(
    val businesses: List<Businesse>,
    val total: Int,
    val region: Region
)