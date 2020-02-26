package cz.brno.mendelu.meetup.functionclasses

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import cz.brno.mendelu.meetup.dataclasses.placecandidate.*
import cz.brno.mendelu.meetup.dataclasses.yelpplaces.Businesse
import cz.brno.mendelu.meetup.dataclasses.yelpplaces.Location
import cz.brno.mendelu.meetup.dataclasses.yelpplaces.YelpAPIResponse
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header

// https://api.yelp.com/v3/businesses/search?latitude=49.17461&longitude=16.6275&radius=6000&term=pub


private const val YELP_KEY = "vnFjWRXOOl5uRf6IzCjl-wkSYHLO0-0ptczcYHULy0O8LvZqPoY_PqCHo3cY7NY8fzv9cunV9XG-bL3UfPZlctidDLWHwBZC-UOo4XGPkyOcwhBnL5cwdZdmBwG5XnYx"
const val RADIUS_NUMBER = 500
private const val Client_ID = "xA49K3blROI1aFdTdcHEXg"
const val BASE_URL = "https://api.yelp.com/v3/"


interface YelpAPI {
     @GET("businesses/search")
     fun getPlaces(
         @Header("Authorization") authHeader: String = YELP_KEY,
         @Query("latitude") lattype: Double? = 49.17461,
         @Query("longitude") longtype: Double? = 16.6275,
         @Query("radius") radidustype: Int = RADIUS_NUMBER,
         @Query("term") termtype: String = "pub"
     ): Deferred<YelpAPIResponse>

    companion object {
        operator fun invoke(): YelpAPI {
            val requestInterceptor = Interceptor { chain ->
                val url = chain.request().url().newBuilder().build()
                val request = chain.request().newBuilder().url(url).build()

                return@Interceptor chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder().addInterceptor(requestInterceptor).build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BASE_URL)
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YelpAPI::class.java)
        }
    }



}