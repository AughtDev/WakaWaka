package com.aught.wakawaka.data

import SummariesResponse
import WakapiService
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Iso8601UtcDateAdapter : JsonAdapter<Date>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    @FromJson
    override fun fromJson(reader: JsonReader): Date? {
        return try {
            val dateAsString = reader.nextString()
            synchronized(dateFormat) {
                dateFormat.parse(dateAsString)
            }
        } catch (e: Exception) {
            null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value != null) {
            synchronized(dateFormat) {
                writer.value(value.toInstant().toString())
            }
        }
    }
}


class WakaDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val moshi = Moshi.Builder()
        .add(Iso8601UtcDateAdapter())
        .addLast(KotlinJsonAdapterFactory()).build()
    private val url: String = "https://api.wakatime.com/api/v1/";
    private val authToken: String = "AUTH_TOKEN"

    override suspend fun doWork(): Result {
        Log.d("WakaDataWorker", "doWork() called")

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authToken))
            .addInterceptor(logging)
            .build()


        val retrofit =
            Retrofit.Builder().baseUrl(url)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build()

        val service = retrofit.create(WakapiService::class.java)

        println("working inside worker")
        return withContext(Dispatchers.IO) {
            try {
                println("Fetching summaries from wakatime")
                val response = service.getSummaries("Last 7 Days")
                println("The response to wakatime call is $response")
                saveProcessedData(applicationContext, response)

                Result.success()
            } catch (e: Exception) {
                println("The exception is $e")
                Result.failure()

            }
        }
    }

    private fun saveProcessedData(context: Context, data: SummariesResponse) {
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
        prefs.edit() {
            val jsonAdapter = moshi.adapter(SummariesResponse::class.java)
            putString("processed_widget_data", jsonAdapter.toJson(data))
            println("putting the data (save)")
        }

    }

    companion object {
        fun loadProcessedData(context: Context): SummariesResponse? {
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString("processed_widget_data", null)
            println("The json loaded is $json")
            if (json == null) return null

            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()
            val jsonAdapter = moshi.adapter(SummariesResponse::class.java)
            return try {
                jsonAdapter.fromJson(json)
            } catch (e: Exception) {
                println("Error loading processed data to object, The exception is $e")
                null
            }
        }
    }
}
