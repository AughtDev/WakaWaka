package com.aught.wakawaka.data

import WakaService
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aught.wakawaka.extras.WakaNotifications
import com.aught.wakawaka.widget.WakaWidget
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
import java.util.Date
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

class Iso8601UtcDateAdapter : JsonAdapter<Date>() {
    @FromJson
    override fun fromJson(reader: JsonReader): Date? {
        val dateAsString = reader.nextString()
        return try {
            val instant = Instant.parse(dateAsString)
            Date.from(instant)
        } catch (e: Exception) {
            println("Error parsing date: $dateAsString, exception: $e")
            return null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value != null) {
            writer.value(value.toInstant().toString())
        }
    }
}

fun calculateDurationStats(dateToDurationMap: Map<String, Int>): DurationStats {
    var today = 0
    var last7Days = 0
    var last30Days = 0
    var lastYear = 0
    var allTime = 0
    val date = LocalDate.now()

    dateToDurationMap.forEach {
        // Convert date string to LocalDate and get the difference in days between the current date and the date in the map
        val recordDate = LocalDate.parse(it.key)
        val daysDifference = java.time.temporal.ChronoUnit.DAYS.between(recordDate, date)
        val duration = it.value

        if (daysDifference == 0L) {
            today = duration
        }
        if (daysDifference <= 7) {
            last7Days += duration
        }
        if (daysDifference <= 30) {
            last30Days += duration
        }
        if (daysDifference <= 365) {
            lastYear += duration
        }
        allTime += duration
    }

    return DurationStats(
        today,
        last7Days,
        last30Days,
        lastYear,
        allTime
    )
}

fun getMapType(): Type {
    return Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        String::class.java
    )
}


class WakaDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val moshi = Moshi.Builder()
        .add(Iso8601UtcDateAdapter())
        .addLast(KotlinJsonAdapterFactory()).build()

    private val wakaNotificationManager = WakaNotifications(appContext)

    //    private val url: String = "https://api.wakatime.com/api/v1/";
    //    private val url: String = "https://wakapi.dev/api/compat/wakatime/v1/";

    override suspend fun doWork(): Result {
        Log.d("WakaDataWorker", "doWork() called")

//        val authToken: String = "REMOVED_WAKATIME_API_KEY"
        val prefs = applicationContext.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        val url: String =
            prefs.getString(WakaHelpers.WAKA_URL, WakaURL.WAKATIME.url) ?: WakaURL.WAKATIME.url

        // if the url is wakapi, the authToken needs to be base 64 encoded
        var authToken: String;
        if (url == WakaURL.WAKAPI.url) {
            authToken = prefs.getString(WakaHelpers.WAKAPI_API, "") ?: "auth_token"
            authToken = WakaHelpers.base64Encode(authToken)
        } else {
            authToken = prefs.getString(WakaHelpers.WAKATIME_API, "") ?: "auth_token"
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

        println("working inside worker")
        return withContext(Dispatchers.IO) {
            try {
                println("Fetching summaries from wakatime")
                val response = service.getSummaries("Last 7 Days")
                updateAppDataWithResponse(applicationContext, response)

                WakaWidget().updateAll(applicationContext)

                Result.success()
            } catch (e: Exception) {
                println("The exception is $e")
                Result.failure()

            }
        }
    }


    private fun calculateDailyStreak(
        data: Map<String, Int>,
        currentStreak: StreakData?,
        target: Float?,
        excludedDays: List<Int>?
    ): StreakData {
        // if there are no target hours, the target is assumed to be anything above 0
        val targetHours = target ?: 0.01f
        val dailyStreakData: StreakData = currentStreak ?: StreakData(0, WakaHelpers.ZERO_DAY)

        // starting from yesterday, we iterate backwards until we find
        // - a date that is not in the data
        // - a date that has not met the target hours
        // - the streak date

        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
        val yesterday = LocalDate.now().minusDays(1)
        var streak = 0;
        var daysAgo = 0;


        while (true) {
            val date = yesterday.minusDays(daysAgo.toLong())
            daysAgo++
            val formattedDate = date.format(dateFormatter)

            if (formattedDate == dailyStreakData.updatedAt) {
                streak += dailyStreakData.count;
                break
            }

            if (excludedDays?.contains(date.dayOfWeek.value) == true) {
                continue
            }
            if (!data.containsKey(formattedDate) || (data[formattedDate]!! / 3600) < targetHours) {
                break
            }
            streak++
        }

        return StreakData(streak, yesterday.format(dateFormatter))
    }

    private fun calculateWeeklyStreak(
        data: Map<String, Int>,
        currentStreak: StreakData?,
        target: Float?
    ): StreakData {
        // if there are no target hours, the target is assumed to be anything above 0
        val targetHours = target ?: 0.01f
        val weeklyStreakData: StreakData = currentStreak ?: StreakData(0, WakaHelpers.ZERO_DAY)

        // starting from last week, we iterate backwards through the first day (monday) of every week until we find

        // - a date that is not in the data
        // - a date that has not met the target hours
        // - the streak date

        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()

        val firstDayOfLastWeek =
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
        var streak = 0;

        while (true) {
            val date = firstDayOfLastWeek.minusWeeks(streak.toLong())
            val formattedDate = date.format(dateFormatter)

            if (formattedDate == weeklyStreakData.updatedAt) {
                streak += weeklyStreakData.count;
                break
            }
            var totalSeconds: Double = 0.0;

            (0..6).forEach {
                val day = date.plusDays(it.toLong())
                val dayFormatted = day.format(dateFormatter)
                if (data.containsKey(dayFormatted)) {
                    totalSeconds += (data[dayFormatted] ?: 0)
                }
            }

            if (!data.containsKey(formattedDate) || totalSeconds / 3600 < targetHours) {
                break
            }
            streak++
        }

        return StreakData(streak, firstDayOfLastWeek.format(dateFormatter))
    }

    private fun updateWakaStatistics(
        context: Context,
        aggregateData: AggregateData,
        projectData: Map<String, ProjectSpecificData>
    ) {
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        val wakaStatisticsAdapter = moshi.adapter(WakaStatistics::class.java)

        val wakaStatistics = WakaStatistics(
            calculateDurationStats(aggregateData.dailyRecords.mapValues { it.value.totalSeconds }),
            projectData.mapValues {
                calculateDurationStats(it.value.dailyDurationInSeconds)
            }
        )

        prefs.edit {
            putString(WakaHelpers.WAKA_STATISTICS, wakaStatisticsAdapter.toJson(wakaStatistics))
        }
    }

    private fun updateAppDataWithResponse(context: Context, data: SummariesResponse) {
        val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())
        val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)
        val projectSpecificDataAdapter = moshi.adapter(ProjectSpecificData::class.java)

        // get the prefs
        val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)

        // get the current aggregate data or assign an empty initial aggregate data instance
        val aggregateData = if (prefs.getString(WakaHelpers.AGGREGATE_DATA, null) != null) {
            aggregateDataAdapter.fromJson(
                prefs.getString(WakaHelpers.AGGREGATE_DATA, null)!!
            )
        } else {
            WakaHelpers.INITIAL_AGGREGATE_DATA
        } ?: return

        // get the current project data or assign an empty map
        val projectDataMap: MutableMap<String, ProjectSpecificData> =
            if (prefs.getString(WakaHelpers.PROJECT_SPECIFIC_DATA, null) != null) {
                val projectDataStringMap = mapAdapter.fromJson(
                    prefs.getString(WakaHelpers.PROJECT_SPECIFIC_DATA, null)!!
                )
                val projectDataMap = mutableMapOf<String, ProjectSpecificData>()
                projectDataStringMap?.forEach {
                    projectDataMap[it.key] = projectSpecificDataAdapter.fromJson(it.value) ?: return
                }
                projectDataMap
            } else {
                emptyMap<String, ProjectSpecificData>().toMutableMap()
            }

        // create a map of project names to their mutable daily duration in seconds
        val projectDailyRecords: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
        projectDataMap.forEach {
            val project = it.value
            val dailyRecords = project.dailyDurationInSeconds.toMutableMap()
            projectDailyRecords[it.key] = dailyRecords
        }

        val updatedAggregateDailyRecords = aggregateData.dailyRecords.toMutableMap()

        // update the map with the new data
        data.data.forEach { it ->
            val date = it.range.date
            val dailyProjectsData: MutableList<ProjectStats> = mutableListOf();

            it.projects.forEach { project ->
                dailyProjectsData.add(ProjectStats(project.name, project.totalSeconds.roundToInt()))

                // update the project data
                if (!projectDailyRecords.containsKey(project.name)) {
                    projectDailyRecords[project.name] = mutableMapOf()
                }
                projectDailyRecords[project.name]?.set(date, project.totalSeconds.roundToInt())

            }

            val dailyAggregateData =
                DailyAggregateData(date, it.grandTotal.totalSeconds.roundToInt(), dailyProjectsData)

            updatedAggregateDailyRecords[date] = dailyAggregateData;
        }


        projectDailyRecords.forEach {
            val name = it.key
            val updatedRecords = it.value.toMap()
            val projectData = projectDataMap[name]
            projectDataMap[name] = ProjectSpecificData(
                it.key,
                projectData?.color ?: WakaHelpers.projectNameToColor(name).toString(),
                it.value.toMap(),
                projectData?.dailyTargetHours,
                projectData?.weeklyTargetHours,
                calculateDailyStreak(
                    updatedRecords,
                    projectData?.dailyStreak,
                    projectData?.dailyTargetHours,
                    projectData?.excludedDaysFromDailyStreak ?: emptyList()
                ),
                calculateWeeklyStreak(
                    updatedRecords,
                    projectData?.weeklyStreak,
                    projectData?.weeklyTargetHours
                ),
                projectData?.excludedDaysFromDailyStreak ?: emptyList()
            )
        }

        val updatedAggregateDailyStreak = calculateDailyStreak(
            updatedAggregateDailyRecords.mapValues { it.value.totalSeconds },
            aggregateData.dailyStreak,
            aggregateData.dailyTargetHours,
            aggregateData.excludedDaysFromDailyStreak
        )
        val updatedWeeklyStreak = calculateWeeklyStreak(
            updatedAggregateDailyRecords.mapValues { it.value.totalSeconds },
            aggregateData.weeklyStreak,
            aggregateData.weeklyTargetHours
        )

        val updatedAggregateData = AggregateData(
            updatedAggregateDailyRecords,
            aggregateData.dailyTargetHours,
            aggregateData.weeklyTargetHours,
            updatedAggregateDailyStreak,
            updatedWeeklyStreak,
            aggregateData.excludedDaysFromDailyStreak
        )

        updateWakaStatistics(context, updatedAggregateData, projectDataMap)

        val projectDataStringMap = mutableMapOf<String, String>()
        projectDataMap.forEach {
            projectDataStringMap[it.key] = projectSpecificDataAdapter.toJson(it.value)
        }

        if (dailyTargetHit(
                updatedAggregateDailyRecords.mapValues { it.value.totalSeconds },
                updatedAggregateData.dailyTargetHours
            )
        ) {
//            wakaNotificationManager.showNotification()
            println("show notification")
        }

        println("saving data")
        // save the map to the prefs
        prefs.edit() {
            putString(WakaHelpers.AGGREGATE_DATA, aggregateDataAdapter.toJson(updatedAggregateData))
            putString(
                WakaHelpers.PROJECT_SPECIFIC_DATA,
                mapAdapter.toJson(projectDataStringMap.toMap())
            )
            println("putting the data (save)")
        }

    }

    companion object {
        fun loadAggregateData(context: Context): AggregateData? {
            // get the prefs and the json aggregate data
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(WakaHelpers.AGGREGATE_DATA, null)

            if (json == null) return null

            // build the adapters to convert between json strings and the data
            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()

            val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)

            return try {
                aggregateDataAdapter.fromJson(json)
            } catch (e: Exception) {
                println("Error loading aggregate data to object, The exception is $e")
                null
            }
        }

        fun saveAggregateData(context: Context, aggregateData: AggregateData) {
            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()
            val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            prefs.edit {
                putString(WakaHelpers.AGGREGATE_DATA, aggregateDataAdapter.toJson(aggregateData))
            }
        }

        fun loadProjectSpecificData(context: Context): Map<String, ProjectSpecificData> {
            // get the prefs and the json aggregate data
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(WakaHelpers.PROJECT_SPECIFIC_DATA, null)

            if (json == null) return mapOf()

            // build the adapters to convert between json strings and the data
            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()

            val projectSpecificDataAdapter = moshi.adapter(ProjectSpecificData::class.java)
            val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())

            val projectDataStringMap = mapAdapter.fromJson(json)
            val projectDataMap = mutableMapOf<String, ProjectSpecificData>()
            projectDataStringMap?.forEach {
                projectDataMap[it.key] =
                    projectSpecificDataAdapter.fromJson(it.value) ?: return@forEach
            }

            return projectDataMap
        }

        fun loadWakaStatistics(context: Context): WakaStatistics {
            // get the prefs and the json aggregate data
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(WakaHelpers.WAKA_STATISTICS, null)
            if (json == null) return WakaHelpers.INITIAL_WAKA_STATISTICS

            // build the adapters to convert between json strings and the data
            val moshi = Moshi.Builder()
                .add(Iso8601UtcDateAdapter())
                .addLast(KotlinJsonAdapterFactory()).build()

            val wakaStatisticsAdapter = moshi.adapter(WakaStatistics::class.java)

            return wakaStatisticsAdapter.fromJson(json) ?: WakaHelpers.INITIAL_WAKA_STATISTICS
        }

        fun dailyTargetHit(dateToDurationMap: Map<String, Int>, targetInHours: Float?): Boolean {
            val date = LocalDate.now()
            val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
            val formattedDate = date.format(dateFormatter)
            val duration = dateToDurationMap[formattedDate] ?: 0


            if (targetInHours == null) {
                return duration > 0
            }
            return (duration.toFloat() / 3600) >= targetInHours
        }

        fun weeklyTargetHit(dateToDurationMap: Map<String, Int>, targetInHours: Float?): Boolean {
            val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
            val firstDayOfLastWeek =
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .minusWeeks(1)
            var totalDuration: Double = 0.0;

            (0..6).forEach {
                val day = firstDayOfLastWeek.plusDays(it.toLong())
                val dayFormatted = day.format(dateFormatter)
                if (dateToDurationMap.containsKey(dayFormatted)) {
                    totalDuration += (dateToDurationMap[dayFormatted] ?: 0)
                }
            }

            if (targetInHours == null) {
                return totalDuration > 0
            }
            return (totalDuration.toFloat() / 3600) >= targetInHours
        }
    }
}
