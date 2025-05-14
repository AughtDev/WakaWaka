package com.aught.wakawaka.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aught.wakawaka.data.AuthInterceptor
import com.aught.wakawaka.data.DailyAggregateData
import com.aught.wakawaka.data.DataDump
import com.aught.wakawaka.data.DataDumpStatus
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaURL
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WakaDataDumpWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val moshi = Moshi.Builder()
        .add(Iso8601UtcDateAdapter())
        .addLast(KotlinJsonAdapterFactory()).build()

    private fun downloadDataDump(url: String): DataDump? {
        val okHttpClient = OkHttpClient.Builder().build()

        val request = Request.Builder().url(url).build()

        return try {
            val response: Response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                println("Download failed: ${response.code}")
            }

            response.body?.source()?.use { source ->
                val dataDump = moshi.adapter(DataDump::class.java).fromJson(source)
                if (dataDump != null) {
                    println("Download successful: ${dataDump.user}")
                } else {
                    println("Failed to parse DataDump")
                }
                dataDump
            }
        } catch (e: IOException) {
            println("Download or parsing error: ${e.message}")
            null
        }

    }

    private fun saveDataDumpToLocalStorage(context: Context,dataDump: DataDump) {
        val projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context)
        val aggregateData = WakaDataFetchWorker.loadAggregateData(context) ?: WakaHelpers.INITIAL_AGGREGATE_DATA

        val updatedAggregateDailyRecords = aggregateData.dailyRecords.toMutableMap()
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

        val url: String =
            prefs.getString(WakaHelpers.Companion.WAKA_URL, WakaURL.WAKATIME.url) ?: WakaURL.WAKATIME.url

        // if the url is wakapi, the authToken needs to be base 64 encoded
        var authToken: String;
        if (url == WakaURL.WAKAPI.url) {
            authToken = prefs.getString(WakaHelpers.Companion.WAKAPI_API, "") ?: "auth_token"
            authToken = WakaHelpers.Companion.base64Encode(authToken)
        } else {
            authToken = prefs.getString(WakaHelpers.Companion.WAKATIME_API, "") ?: "auth_token"
        }

        // a logging interceptor to attach to the http client
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // the http client with an auth interceptor and logging interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authToken))
            .addInterceptor(logging)
            .build()


        // a retrofit instance with a moshi converter and the http client
        val retrofit =
            Retrofit.Builder().baseUrl(url)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build()

        // the wakapi service created from the retrofit instance
        val service = retrofit.create(WakaService::class.java)

        return withContext(Dispatchers.IO) {
            try {
                val response = service.getDataDumps()
                if (response.data.isEmpty()) {
                    return@withContext Result.failure()
                }
                val dataDumpSpecs = response.data[0]
                if (dataDumpSpecs.status != DataDumpStatus.COMPLETED) {
                    return@withContext Result.failure()
                }
                println("The download url is ${dataDumpSpecs.downloadUrl}")

//                val dataDump = downloadDataDump(dataDumpSpecs.downloadUrl)

//                if (dataDump == null) {
//                    return@withContext Result.failure()
//                }
                // fetch the json at the download url
                Result.success()
            } catch (e: Exception) {
                println("The exception is $e")
                Result.failure()

            }
        }
    }
}
