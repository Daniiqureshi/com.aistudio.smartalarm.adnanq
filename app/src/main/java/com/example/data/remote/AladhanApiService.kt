package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface AladhanApiService {

    @GET("timings/{date}")
    suspend fun getTimingsByCoordinates(
        @Path("date") date: String, // DD-MM-YYYY
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = 3, // 3 = Muslim World League, 2 = ISNA, 4 = Makkah, 1 = Karachi
        @Query("school") school: Int = 0 // 0 = Shafi, 1 = Hanafi
    ): AladhanResponse

    @GET("timingsByCity/{date}")
    suspend fun getTimingsByCity(
        @Path("date") date: String,
        @Query("city") city: String,
        @Query("country") country: String = "",
        @Query("method") method: Int = 3,
        @Query("school") school: Int = 0
    ): AladhanResponse

    companion object {
        private const val BASE_URL = "https://api.aladhan.com/v1/"

        fun create(): AladhanApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(AladhanApiService::class.java)
        }
    }
}
