package com.aught.wakawaka.data

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
import com.squareup.moshi.Types
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
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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

    private fun getMapType(): Type {
        return Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            String::class.java
        )
    }

    private fun updateDailyStreak(context: Context, data: Map<String, WakaData>) {
        // get the prefs and the json streak data and the target daily hours
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(WakaHelpers.DAILY_STREAK, null)
        val targetHours = prefs.getFloat(WakaHelpers.DAILY_TARGET_HOURS, 0f)

        // set up the waka streak adapter and fetch the streak data or generate it if not there
        val streakAdapter = moshi.adapter(WakaStreak::class.java)
        val dailyStreakData: WakaStreak =
            if (json != null && streakAdapter.fromJson(json) != null) {
                streakAdapter.fromJson(json)
            } else {
                WakaStreak(0, "2000-09-30")
            }!!

        // starting from yesterday, we iterate backwards until we find
        // - a date that is not in the data
        // - a date that has not met the target hours
        // - the streak date

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val yesterday = LocalDate.now().minusDays(1)
        var streak = 0;


        while (true) {
            val date = yesterday.minusDays(streak.toLong())
            var formattedDate = date.format(dateFormatter)

            if (formattedDate == dailyStreakData.updatedAt) {
                streak += dailyStreakData.count;
                break
            }
            if (data.containsKey(formattedDate) == false || (data[formattedDate]!!.totalSeconds) / 3600 < targetHours) {
                break
            }
            streak++
        }

        val updatedDailyStreakData = WakaStreak(streak, yesterday.format(dateFormatter))

        prefs.edit {
            putString(WakaHelpers.DAILY_STREAK, streakAdapter.toJson(updatedDailyStreakData))
        }
    }

    private fun updateWeeklyStreak(context: Context, data: Map<String, WakaData>) {
        // get the prefs and the json streak data and the target weekly hours
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(WakaHelpers.WEEKLY_STREAK, null)
        val targetHours = prefs.getFloat(WakaHelpers.WEEKLY_TARGET_HOURS, 0f)

        // set up the waka streak adapter and fetch the streak data or generate it if not there
        val streakAdapter = moshi.adapter(WakaStreak::class.java)
        val weeklyStreakData: WakaStreak =
            if (json != null && streakAdapter.fromJson(json) != null) {
                streakAdapter.fromJson(json)
            } else {
                // a monday
                WakaStreak(0, "2000-10-02")
            }!!


        // starting from last week, we iterate backwards through the first day (monday) of every week until we find

        // - a date that is not in the data
        // - a date that has not met the target hours
        // - the streak date

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val firstDayOfLastWeek =
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
        var streak = 0;

        while (true) {
            val date = firstDayOfLastWeek.minusWeeks(streak.toLong())
            var formattedDate = date.format(dateFormatter)

            if (formattedDate == weeklyStreakData.updatedAt) {
                streak += weeklyStreakData.count;
                break
            }
            if (data.containsKey(formattedDate) == false || (data[formattedDate]!!.totalSeconds) / 3600 < targetHours) {
                break
            }
            streak++
        }

        val updatedWeeklyStreakData = WakaStreak(streak, firstDayOfLastWeek.format(dateFormatter))

        prefs.edit {
            putString(WakaHelpers.WEEKLY_STREAK, streakAdapter.toJson(updatedWeeklyStreakData))
        }
    }

    private fun saveProcessedData(context: Context, data: SummariesResponse) {
        // get the prefs and the json coding data
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(WakaHelpers.CODING_DATA, null)

        // get the adapters to convert between json strings and the data
        val wakaDataAdapter = moshi.adapter(WakaData::class.java)
        val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())

        // convert the json to a map or create a new one if it doesn't exist
        val streakDataStringMap: MutableMap<String, String> =
            if (json != null && mapAdapter.fromJson(json) != null) {
                mapAdapter.fromJson(json)!!.toMutableMap()
            } else {
                mutableMapOf()
            }

        val streakDataMap: MutableMap<String, WakaData> = mutableMapOf();

        streakDataStringMap.forEach {
            streakDataMap[it.key] = wakaDataAdapter.fromJson(it.value) ?: return
        }


        // update the map with the new data
        data.data.forEach {
            // save the data at the appropriate date
            val date = it.range.date

            val dailyProjectsData: List<WakaProjectData> = it.projects.map {
                WakaProjectData(it.name, it.totalSeconds)
            }

            val dailyWakaData = WakaData(date, it.grandTotal.totalSeconds, dailyProjectsData)

            streakDataMap[date] = dailyWakaData;
        }

        updateDailyStreak(context, streakDataMap)
        updateWeeklyStreak(context, streakDataMap)

        // convert the map to a json string and save it
        streakDataStringMap.clear()
        streakDataMap.forEach {
            streakDataStringMap[it.key] = wakaDataAdapter.toJson(it.value)
        }

        // save the map to the prefs
        prefs.edit() {
            putString(WakaHelpers.CODING_DATA, mapAdapter.toJson(streakDataStringMap.toMap()))
            println("putting the data (save)")
        }

    }

    companion object {
        private fun getMapType(): Type {
            return Types.newParameterizedType(
                MutableMap::class.java,
                String::class.java,
                String::class.java
            )
        }

        fun loadProcessedData(context: Context): Map<String, WakaData>? {
            // get the prefs and the json coding data
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(WakaHelpers.CODING_DATA, null)

            println("The json loaded is $json")
            if (json == null) return null

            // build the adapters to convert between json strings and the data
            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()

            val dailyDataAdapter = moshi.adapter(WakaData::class.java)
            val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())


            // convert the json to a map and return
            val prefsMap = mapAdapter.fromJson(json)
            val processedData = mutableMapOf<String, WakaData>()

            prefsMap?.forEach {
                processedData[it.key] = dailyDataAdapter.fromJson(it.value) ?: return null
            }

            return try {
                return processedData.toMap()
            } catch (e: Exception) {
                println("Error loading processed data to object, The exception is $e")
                null
            }
        }

        fun loadStreak(context: Context, mode: GraphMode): Int {
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            val prefKey = when (mode) {
                GraphMode.Daily -> WakaHelpers.DAILY_STREAK
                GraphMode.Weekly -> WakaHelpers.WEEKLY_STREAK
            }
            val json = prefs.getString(prefKey, null)

            // build the adapters to convert between json strings and the data
            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()

            val streakAdapter = moshi.adapter(WakaStreak::class.java)

            return if (json != null && streakAdapter.fromJson(json) != null) {
                streakAdapter.fromJson(json)!!.count
            } else {
                0
            }
        }
    }
}
