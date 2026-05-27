package com.jeremyle.myfi.di

import com.jeremyle.myfi.BuildConfig
import com.jeremyle.myfi.data.remote.FinnhubApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// TODO: Convert to @Module @InstallIn(SingletonComponent::class) once Hilt is added
object NetworkModule {

    private const val BASE_URL = "https://finnhub.io/api/v1/"

    fun provideFinnhubApi(): FinnhubApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Attach the API key to every request via query param
                val url = chain.request().url.newBuilder()
                    .addQueryParameter("token", BuildConfig.FINNHUB_API_KEY)
                    .build()
                chain.proceed(chain.request().newBuilder().url(url).build())
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApi::class.java)
    }
}
