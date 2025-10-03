package com.aught.wakawaka.workers

import com.aught.wakawaka.data.WakaService
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.AuthInterceptor
import com.aught.wakawaka.data.DailyAggregateData
import com.aught.wakawaka.data.DataRequest
import com.aught.wakawaka.data.DurationStats
import com.aught.wakawaka.data.NotificationData
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.ProjectStats
import com.aught.wakawaka.data.SettingsData
import com.aught.wakawaka.data.StreakData
import com.aught.wakawaka.data.SummariesResponse
import com.aught.wakawaka.data.TimePeriod
import com.aught.wakawaka.data.WakaDataHandler
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaStatistics
import com.aught.wakawaka.data.WakaURL
import com.aught.wakawaka.extras.WakaNotifications
import com.aught.wakawaka.utils.ColorUtils
import com.aught.wakawaka.utils.JSONDateAdapter
import com.aught.wakawaka.utils.getMoshi
import com.aught.wakawaka.widget.aggregate.WakaAggregateWidget
import com.aught.wakawaka.widget.project.WakaProjectWidget
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt


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
        val daysDifference = ChronoUnit.DAYS.between(recordDate, date)
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


class WakaDataFetchWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val moshi = getMoshi()

    private val wakaNotificationManager = WakaNotifications(appContext)

    //    private val url: String = "https://api.wakatime.com/api/v1/";
    //    private val url: String = "https://wakapi.dev/api/compat/wakatime/v1/";

    private fun getRange(durationSinceLastFetch: Long): String {
        val now = java.time.ZonedDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay(now.zone)
        val millisSinceMidnight = java.time.Duration.between(midnight, now).toMillis()

        return if (durationSinceLastFetch < millisSinceMidnight) {
            "Today"
        } else {
            "Last 7 Days"
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(
            WakaHelpers.Companion.PREFS,
            Context.MODE_PRIVATE
        )

        // get last fetch timestamp
        val lastFetchTimestamp = prefs.getLong(WakaHelpers.LAST_FETCH_TIMESTAMP, 0)
        val currTimestamp = System.currentTimeMillis()

        val durationSinceLastFetch = currTimestamp - lastFetchTimestamp

        // if the last fetch was less than WakaHelpers.MIN_FETCH_INTERVAL ago, return success else update the timestamp
        if (durationSinceLastFetch < WakaHelpers.MIN_FETCH_INTERVAL) {
            println("Last fetch was less than ${WakaHelpers.MIN_FETCH_INTERVAL} ms ago, skipping fetch")
            // wait for 2 seconds
            kotlinx.coroutines.delay(2000)
            return Result.success()
        } else {
            prefs.edit {
                putLong(WakaHelpers.LAST_FETCH_TIMESTAMP, currTimestamp)
            }
        }

        val url: String =
            prefs.getString(WakaHelpers.Companion.WAKA_URL, WakaURL.WAKATIME.url)
                ?: WakaURL.WAKATIME.url

        // if the url is wakapi, the authToken needs to be base 64 encoded
        var authToken: String;
        if (url == WakaURL.WAKAPI.url) {
            authToken = prefs.getString(WakaHelpers.Companion.WAKAPI_API, "") ?: "auth_token"
            authToken = WakaHelpers.Companion.base64Encode(authToken)
        } else {
            authToken = prefs.getString(WakaHelpers.Companion.WAKATIME_API, "") ?: "auth_token"
        }

        // a logging interceptor to attach to the http client
//        val logging = HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        }

        // the http client with an auth interceptor and logging interceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authToken))
//            .addInterceptor(logging)
            .build()


        // a retrofit instance with a moshi converter and the http client
        val retrofit =
            Retrofit.Builder().baseUrl(url)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(okHttpClient)
                .build()

        // the wakatime service created from the retrofit instance
        val service = retrofit.create(WakaService::class.java)

        return withContext(Dispatchers.IO) {
            try {
                val response = service.getSummaries(getRange(durationSinceLastFetch))
                updateAppDataWithResponse(applicationContext, response)

                WakaAggregateWidget().updateAll(applicationContext)
                WakaProjectWidget().updateAll(applicationContext)

                Result.success()
            } catch (e: Exception) {
                Result.failure()

            }
        }
    }


    private fun handleNotifications(
        prefs: SharedPreferences,
        aggregateData: AggregateData,
        projectData: Map<String, ProjectSpecificData>
    ) {
        val notificationDataAdapter = moshi.adapter(NotificationData::class.java)
        val notificationDataString =
            prefs.getString(WakaHelpers.Companion.NOTIFICATION_DATA_KEY, null)

        val notificationData: NotificationData = notificationDataString?.let {
            (runCatching { notificationDataAdapter.fromJson(it) }.getOrNull()
                ?: WakaHelpers.Companion.INITIAL_NOTIFICATION_DATA)
        } ?: WakaHelpers.Companion.INITIAL_NOTIFICATION_DATA

        var updatedLastAggDailyTgtNotifDate =
            notificationData.lastAggregateDailyTargetNotificationDate
        var updatedLastAggWeeklyTgtNotifDate =
            notificationData.lastAggregateWeeklyTargetNotificationDate

        val today = LocalDate.now()
//        wakaNotificationManager.showNotification(
//            "Daily Target Hit",
//            "Congratulations! You have hit your daily target of ${
//                WakaHelpers.Companion.durationInSecondsToDurationString(
//                    ((aggregateData.dailyTargetHours ?: (0f * 3600f))).roundToInt(),
//                    " hours", " minutes"
//                )
//            }", 112
//        )

        if (
            aggregateData.dailyTargetHours != null &&
            dailyTargetHit(
                aggregateData.dailyRecords.mapValues { it.value.totalSeconds },
                aggregateData.dailyTargetHours
            ) &&
            WakaHelpers.Companion.yyyyMMDDToDate(notificationData.lastAggregateDailyTargetNotificationDate)
                .isBefore(today)
        ) {
            wakaNotificationManager.showNotification(
                "Daily Target Hit",
                "Congratulations! You have hit your daily target of ${
                    WakaHelpers.Companion.durationInSecondsToDurationString(
                        (aggregateData.dailyTargetHours * 3600f).roundToInt(),
                        " hours", " minutes"
                    )
                }", 112
            )
            updatedLastAggDailyTgtNotifDate = WakaHelpers.Companion.dateToYYYYMMDD(today)
        }

        val firstDateThisWeek =
            LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        if (
            aggregateData.weeklyTargetHours != null &&
            weeklyTargetHit(
                aggregateData.dailyRecords.mapValues { it.value.totalSeconds },
                aggregateData.weeklyTargetHours
            ) &&
            WakaHelpers.Companion.yyyyMMDDToDate(notificationData.lastAggregateWeeklyTargetNotificationDate)
                .isBefore(firstDateThisWeek)
        ) {
            wakaNotificationManager.showNotification(
                "Weekly Target Hit",
                "Congratulations! You have hit your weekly target of ${
                    WakaHelpers.Companion.durationInSecondsToDurationString(
                        (aggregateData.weeklyTargetHours * 3600f).roundToInt(),
                        " hours", " minutes"
                    )
                }", 111
            )
            updatedLastAggWeeklyTgtNotifDate =
                WakaHelpers.Companion.dateToYYYYMMDD(firstDateThisWeek)
        }

        // save the updated notification data
        val updatedNotificationData = NotificationData(
            updatedLastAggDailyTgtNotifDate,
            updatedLastAggWeeklyTgtNotifDate,
            notificationData.lastProjectDailyNotificationDates,
            notificationData.lastProjectWeeklyNotificationDates
        )

        prefs.edit {
            putString(
                WakaHelpers.Companion.NOTIFICATION_DATA_KEY,
                notificationDataAdapter.toJson(updatedNotificationData)
            )
        }
    }

    private fun updateAppDataWithResponse(context: Context, data: SummariesResponse) {
        val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())
        val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)
        val projectSpecificDataAdapter = moshi.adapter(ProjectSpecificData::class.java)

        // get the prefs
        val prefs = context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

        // get the current aggregate data or assign an empty initial aggregate data instance
        val aggregateData =
            if (prefs.getString(WakaHelpers.Companion.AGGREGATE_DATA_KEY, null) != null) {
                aggregateDataAdapter.fromJson(
                    prefs.getString(WakaHelpers.Companion.AGGREGATE_DATA_KEY, null)!!
                )
            } else {
                WakaHelpers.Companion.INITIAL_AGGREGATE_DATA
            } ?: return

        // get the current project data or assign an empty map
        val projectDataMap: MutableMap<String, ProjectSpecificData> =
            if (prefs.getString(WakaHelpers.Companion.PROJECT_SPECIFIC_DATA_KEY, null) != null) {
                val projectDataStringMap = mapAdapter.fromJson(
                    prefs.getString(WakaHelpers.Companion.PROJECT_SPECIFIC_DATA_KEY, null)!!
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
                projectData?.color
                    ?: ColorUtils.colorToHex(WakaHelpers.Companion.projectNameToColor(name)),
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
        handleNotifications(prefs, updatedAggregateData, projectDataMap)

        val projectDataStringMap = mutableMapOf<String, String>()
        projectDataMap.forEach {
            projectDataStringMap[it.key] = projectSpecificDataAdapter.toJson(it.value)
        }


        // save the map to the prefs
        prefs.edit() {
            putString(
                WakaHelpers.Companion.AGGREGATE_DATA_KEY,
                aggregateDataAdapter.toJson(updatedAggregateData)
            )
            putString(
                WakaHelpers.Companion.PROJECT_SPECIFIC_DATA_KEY,
                mapAdapter.toJson(projectDataStringMap.toMap())
            )
        }
    }

    companion object {
        //region CALCULATIONS

        fun calculateDailyStreak(
            data: Map<String, Int>,
            currentStreak: StreakData?,
            target: Float?,
            excludedDays: List<Int>?
        ): StreakData {
            // if there are no target hours, the target is assumed to be anything above 0
            val targetHours = target
            val dailyStreakData: StreakData =
                currentStreak ?: StreakData(0, WakaHelpers.Companion.ZERO_DAY)

            // starting from yesterday, we iterate backwards until we find
            // - a date that is not in the data
            // - a date that has not met the target hours
            // - the streak date

            val dateFormatter = WakaHelpers.Companion.getYYYYMMDDDateFormatter()
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

                if (
                    !data.containsKey(formattedDate) ||
                    (targetHours == null && data[formattedDate]!! == 0) ||
                    (targetHours != null && data[formattedDate]!! < targetHours * 3600)
                ) {
                    if (excludedDays?.contains(date.dayOfWeek.value) == true) {
                        continue
                    }
                    break
                }
                streak++
            }


            return StreakData(streak, yesterday.format(dateFormatter))
        }

        fun calculateWeeklyStreak(
            data: Map<String, Int>,
            currentStreak: StreakData?,
            target: Float?
        ): StreakData {
            // if there are no target hours, the target is assumed to be anything above 0
            val targetHours = target
            val weeklyStreakData: StreakData =
                currentStreak ?: StreakData(0, WakaHelpers.Companion.ZERO_DAY)

            // starting from last week, we iterate backwards through the first day (monday) of every week until we find

            // - a date that is not in the data
            // - a date that has not met the target hours
            // - the streak date

            val dateFormatter = WakaHelpers.Companion.getYYYYMMDDDateFormatter()

            val firstDayOfLastWeek =
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .minusWeeks(1)
            var streak = 0;

            while (true) {
                val date = firstDayOfLastWeek.minusWeeks(streak.toLong())
                val formattedDate = date.format(dateFormatter)

                if (formattedDate == weeklyStreakData.updatedAt) {
                    streak += weeklyStreakData.count;
                    break
                }
                var totalSeconds: Long = 0;

                (0..6).forEach {
                    val day = date.plusDays(it.toLong())
                    val dayFormatted = day.format(dateFormatter)
                    if (data.containsKey(dayFormatted)) {
                        totalSeconds += (data[dayFormatted] ?: 0)
                    }
                }

                if ((targetHours == null && totalSeconds == 0L) ||
                    (targetHours != null && totalSeconds < targetHours * 3600)
                ) {
                    break
                }
                streak++
            }

            return StreakData(streak, firstDayOfLastWeek.format(dateFormatter))
        }


        fun dailyTargetHit(dateToDurationMap: Map<String, Int>, targetInHours: Float?): Boolean {
            val date = LocalDate.now()
            val dateFormatter = WakaHelpers.Companion.getYYYYMMDDDateFormatter()
            val formattedDate = date.format(dateFormatter)
            val duration = dateToDurationMap[formattedDate] ?: 0


            if (targetInHours == null) {
                return duration > 0
            }
            return (duration.toFloat() / 3600) >= targetInHours
        }

        fun weeklyTargetHit(dateToDurationMap: Map<String, Int>, targetInHours: Float?): Boolean {
            val dateFormatter = WakaHelpers.Companion.getYYYYMMDDDateFormatter()
            val firstDayOfThisWeek =
                LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            var totalDuration: Double = 0.0;

            (0..6).forEach {
                val day = firstDayOfThisWeek.plusDays(it.toLong())
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

        //endregion

        //region LOAD DATA

        private val emptyAggregateData = AggregateData(
            emptyMap(),
            null, null, null, null,
            emptyList()
        )

        fun loadAggregateData(context: Context): AggregateData {
            // get the prefs and the json aggregate data
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)
            val json =
                prefs.getString(WakaHelpers.Companion.AGGREGATE_DATA_KEY, null)
                    ?: return emptyAggregateData

            // build the adapters to convert between json strings and the data
            val moshi = getMoshi()

            val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)

            return (try {
                aggregateDataAdapter.fromJson(json)
            } catch (e: Exception) {
                println("Error loading aggregate data to object, The exception is $e")
                null
            } ?: emptyAggregateData)
        }

        fun loadProjectSpecificData(context: Context): Map<String, ProjectSpecificData> {
            // get the prefs and the json aggregate data
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(WakaHelpers.Companion.PROJECT_SPECIFIC_DATA_KEY, null)

            if (json == null) return mapOf()

            // build the adapters to convert between json strings and the data
            val moshi = getMoshi()

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
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)
            val json = prefs.getString(WakaHelpers.Companion.WAKA_STATISTICS_KEY, null)
            if (json == null) return WakaHelpers.Companion.INITIAL_WAKA_STATISTICS

            // build the adapters to convert between json strings and the data
            val moshi = getMoshi()

            val wakaStatisticsAdapter = moshi.adapter(WakaStatistics::class.java)

            return wakaStatisticsAdapter.fromJson(json)
                ?: WakaHelpers.Companion.INITIAL_WAKA_STATISTICS
        }

        fun loadSettingsData(context: Context): SettingsData {
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

            val aggregateData = loadAggregateData(context)

            return SettingsData(
                prefs.getString(WakaHelpers.WAKATIME_API, "") ?: "",
                aggregateData.dailyTargetHours,
                aggregateData.weeklyTargetHours,
                aggregateData.excludedDaysFromDailyStreak
            )
        }

        fun loadNotificationData(context: Context): NotificationData {
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)
            val moshi = getMoshi()

            val notificationDataAdapter = moshi.adapter(NotificationData::class.java)
            val notificationDataString =
                prefs.getString(WakaHelpers.Companion.NOTIFICATION_DATA_KEY, null)

            val notificationData: NotificationData = notificationDataString?.let {
                (runCatching { notificationDataAdapter.fromJson(it) }.getOrNull()
                    ?: WakaHelpers.Companion.INITIAL_NOTIFICATION_DATA)
            } ?: WakaHelpers.Companion.INITIAL_NOTIFICATION_DATA

            return notificationData
        }

        fun loadWakatimeAPI(context: Context): String {
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)
            return prefs.getString(WakaHelpers.Companion.WAKATIME_API, "") ?: ""
        }

        fun loadProjectAssignedToWidget(context: Context): String? {
            val prefs = context.getSharedPreferences(WakaHelpers.PREFS, Context.MODE_PRIVATE)
            return prefs.getString(WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET, null)
        }

        //endregion

        //region SAVE DATA

        fun saveAggregateData(context: Context, aggregateData: AggregateData) {
            val moshi = getMoshi()
            val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)
            prefs.edit {
                putString(
                    WakaHelpers.Companion.AGGREGATE_DATA_KEY,
                    aggregateDataAdapter.toJson(aggregateData)
                )
            }
        }

        fun saveProjectDataMap(context: Context, projectDataMap: Map<String, ProjectSpecificData>) {
            val moshi = getMoshi()
            val projectSpecificDataAdapter = moshi.adapter(ProjectSpecificData::class.java)
            val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())

            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

            val projectDataStringMap = projectDataMap.mapValues {
                projectSpecificDataAdapter.toJson(it.value)
            }

            prefs.edit {
                putString(
                    WakaHelpers.Companion.PROJECT_SPECIFIC_DATA_KEY,
                    mapAdapter.toJson(projectDataStringMap)
                )
            }
        }

        fun saveProjectData(
            context: Context,
            projectName: String,
            projectData: ProjectSpecificData
        ) {
            val moshi = getMoshi()
            val projectSpecificDataAdapter = moshi.adapter(ProjectSpecificData::class.java)
            val mapAdapter = moshi.adapter<Map<String, String>>(getMapType())

            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

            val currProjectDataMap = loadProjectSpecificData(context).toMutableMap()
            currProjectDataMap[projectName] = projectData

            val wakaDataHandler = WakaDataHandler(null, currProjectDataMap)

            val newDailyStreakCount = wakaDataHandler.calculateUpdatedStreak(
                DataRequest.ProjectSpecific(projectName),
                TimePeriod.DAY
            )
            val newWeeklyStreakCount = wakaDataHandler.calculateUpdatedStreak(
                DataRequest.ProjectSpecific(projectName),
                TimePeriod.WEEK
            )

            currProjectDataMap[projectName] = projectData.copy(
                dailyStreak = StreakData(newDailyStreakCount, LocalDate.now().toString()),
                weeklyStreak = StreakData(newWeeklyStreakCount, LocalDate.now().toString())
            )

            val projectDataStringMap = currProjectDataMap.mapValues {
                projectSpecificDataAdapter.toJson(it.value)
            }

            prefs.edit {
                putString(
                    WakaHelpers.Companion.PROJECT_SPECIFIC_DATA_KEY,
                    mapAdapter.toJson(projectDataStringMap)
                )
            }
        }

        fun saveSettingsData(context: Context,settingsData: SettingsData) {
            val prefs =
                context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

            val aggregateData = loadAggregateData(context)
            val newAggregateData = aggregateData.copy(
                dailyTargetHours = settingsData.dailyTargetHours,
                weeklyTargetHours = settingsData.weeklyTargetHours,
                excludedDaysFromDailyStreak = settingsData.dailyStreakExcludedDays
            )
            saveAggregateData(context, newAggregateData)
            prefs.edit {
                putString(WakaHelpers.WAKATIME_API, settingsData.wakatimeAPIKey)
            }
        }


        fun updateWakaStatistics(
            context: Context,
            aggregateData: AggregateData,
            projectData: Map<String, ProjectSpecificData>
        ) {
            val moshi = getMoshi()
            val prefs = context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

            val wakaStatisticsAdapter = moshi.adapter(WakaStatistics::class.java)

            val wakaStatistics = WakaStatistics(
                calculateDurationStats(aggregateData.dailyRecords.mapValues { it.value.totalSeconds }),
                projectData.mapValues {
                    calculateDurationStats(it.value.dailyDurationInSeconds)
                }
            )

            prefs.edit {
                putString(
                    WakaHelpers.Companion.WAKA_STATISTICS_KEY,
                    wakaStatisticsAdapter.toJson(wakaStatistics)
                )
            }
        }

        //endregion
    }
}
