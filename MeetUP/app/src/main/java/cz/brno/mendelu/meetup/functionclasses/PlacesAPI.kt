package cz.brno.mendelu.meetup.functionclasses

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import cz.brno.mendelu.meetup.dataclasses.placecandidate.PlacesResponse
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val API_KEY = "AIzaSyCu965Tz0itGinEHn2dPA40G-B1ZhcfU2M"


 // https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input=hospoda&inputtype=textquery&fields=formatted_address,name,rating,geometry&locationbias=circle:5000@49.195061,16.606836&key=AIzaSyCu965Tz0itGinEHn2dPA40G-B1ZhcfU2M



interface PlacesAPI {

    @GET("json")
    fun getPlaces(
        @Query("input")  type: String = "hospoda",
        @Query("inputtype")  inputType: String = "textquery",
        @Query("fields")  fields: String = "name,geometry,rating,place_id",
        @Query("locationbias")  location: String = "circle:1000@49.195061,16.606836",
        @Query("key")  key: String = API_KEY
    ): Deferred<PlacesResponse>

    companion object {
        operator fun invoke(): PlacesAPI {
            val requestInterceptor = Interceptor {chain ->
                val url = chain.request().url().newBuilder().build()
                val request = chain.request().newBuilder().url(url).build()

                return@Interceptor chain.proceed(request)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(requestInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://maps.googleapis.com/maps/api/place/findplacefromtext/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(PlacesAPI::class.java)
        }
    }
}