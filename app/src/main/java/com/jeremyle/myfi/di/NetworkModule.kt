package com.jeremyle.myfi.di

import com.jeremyle.myfi.BuildConfig
import com.jeremyle.myfi.data.remote.FinnhubApi
import com.jeremyle.myfi.data.remote.YahooFinanceApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// TODO: Convert to @Module @InstallIn(SingletonComponent::class) once Hilt is added
object NetworkModule {

    // ─── Finnhub (real-time quotes) ───────────────────────────────────────────

    private const val FINNHUB_BASE_URL = "https://finnhub.io/api/v1/"

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
                },
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(FINNHUB_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FinnhubApi::class.java)
    }

    // ─── Yahoo Finance (chart / historical data — free, no key required) ──────

    private const val YAHOO_BASE_URL = "https://query1.finance.yahoo.com/"

    fun provideYahooFinanceApi(): YahooFinanceApi {
        val client = OkHttpClient.Builder()
            // Yahoo occasionally rejects requests without a browser-like User-Agent
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0")
                        .build(),
                )
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BASIC
                    else
                        HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(YAHOO_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YahooFinanceApi::class.java)
    }
}
