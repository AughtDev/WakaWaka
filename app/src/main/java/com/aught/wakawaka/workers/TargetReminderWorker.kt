package com.aught.wakawaka.workers

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.NotificationData
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.extras.WakaNotifications
import com.aught.wakawaka.utils.getMoshi
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

class TargetReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val moshi = getMoshi()
    private val wakaNotificationManager = WakaNotifications(appContext)

    companion object {
        const val REMINDER_TYPE_KEY = "reminder_type"
        const val REMINDER_6PM = "6pm"
        const val REMINDER_10PM = "10pm"
    }

    override suspend fun doWork(): Result {
        val reminderType = inputData.getString(REMINDER_TYPE_KEY) ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences(
            WakaHelpers.PREFS,
            Context.MODE_PRIVATE
        )

        return withContext(Dispatchers.IO) {
            try {
                val today = LocalDate.now()
                val dayOfWeek = today.dayOfWeek.value

                // Load data
                val aggregateData = WakaDataFetchWorker.loadAggregateData(applicationContext)
                val projectData = WakaDataFetchWorker.loadProjectSpecificData(applicationContext)
                val notificationData = loadNotificationData(prefs)

                // Check if already shown today
                if (reminderType == REMINDER_6PM && notificationData.last6pmReminderDate == today.toString()) {
                    return@withContext Result.success()
                }
                if (reminderType == REMINDER_10PM && notificationData.last10pmReminderDate == today.toString()) {
                    return@withContext Result.success()
                }

                // For 10pm, only show if target still not hit
                if (reminderType == REMINDER_10PM) {
                    val isExcludedDay = aggregateData.excludedDaysFromDailyStreak.contains(dayOfWeek)
                    val hasAggregateTarget = aggregateData.dailyTargetHours != null
                    val aggregateTargetHit = if (hasAggregateTarget && !isExcludedDay) {
                        dailyTargetHit(aggregateData)
                    } else true

                    val allProjectsHit = projectData.values.all { project ->
                        if (project.dailyTargetHours == null) true
                        else if (project.excludedDaysFromDailyStreak.contains(dayOfWeek)) true
                        else dailyTargetHit(project)
                    }

                    if (aggregateTargetHit && allProjectsHit) {
                        // Target was hit, no need for 10pm reminder
                        updateReminderDate(prefs, notificationData, reminderType, today)
                        return@withContext Result.success()
                    }
                }

                // Build reminder notifications
                sendReminderNotifications(aggregateData, projectData, dayOfWeek, reminderType)

                // Update last reminder date
                updateReminderDate(prefs, notificationData, reminderType, today)

                // Reschedule for tomorrow
                rescheduleReminder(reminderType)

                Result.success()
            } catch (e: Exception) {
                // Reschedule even on failure to ensure we don't lose the schedule
                rescheduleReminder(reminderType)
                Result.failure()
            }
        }
    }

    private fun sendReminderNotifications(
        aggregateData: AggregateData,
        projectData: Map<String, ProjectSpecificData>,
        dayOfWeek: Int,
        reminderType: String
    ) {
        val today = LocalDate.now()
        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
        val todayStr = today.format(dateFormatter)

        // Check aggregate target
        val isAggregateExcluded = aggregateData.excludedDaysFromDailyStreak.contains(dayOfWeek)
        val aggregateIncomplete = if (!isAggregateExcluded && aggregateData.dailyTargetHours != null) {
            val todaySeconds = aggregateData.dailyRecords[todayStr]?.totalSeconds ?: 0
            val targetSeconds = (aggregateData.dailyTargetHours * 3600).roundToInt()
            if (todaySeconds < targetSeconds) {
                val hoursNeeded = ceil((targetSeconds - todaySeconds) / 3600f).toInt()
                val minutesNeeded = ceil(((targetSeconds - todaySeconds) % 3600) / 60f).toInt()
                val timeNeeded = if (hoursNeeded > 0) "${hoursNeeded}h" else "${minutesNeeded}m"
                Pair(true, timeNeeded)
            } else null
        } else null

        // Check project targets
        val incompleteProjects = mutableListOf<Pair<String, String>>()
        projectData.values.forEach { project ->
            if (project.dailyTargetHours != null && !project.excludedDaysFromDailyStreak.contains(dayOfWeek)) {
                val todaySeconds = project.dailyDurationInSeconds[todayStr] ?: 0
                val targetSeconds = (project.dailyTargetHours * 3600).roundToInt()
                if (todaySeconds < targetSeconds) {
                    val hoursNeeded = ceil((targetSeconds - todaySeconds) / 3600f).toInt()
                    val minutesNeeded = ceil(((targetSeconds - todaySeconds) % 3600) / 60f).toInt()
                    val timeNeeded = if (hoursNeeded > 0) "${hoursNeeded}h" else "${minutesNeeded}m"
                    incompleteProjects.add(Pair(project.name, timeNeeded))
                }
            }
        }

        // Send aggregate notification if needed
        if (aggregateIncomplete != null) {
            val title = if (reminderType == REMINDER_6PM) "⏰ Daily Target Check" else "🌙 Final Call"
            val text = "Aggregate: Need ${aggregateIncomplete.second} more to hit your daily goal!"
            wakaNotificationManager.showReminderNotification(
                title,
                text,
                WakaHelpers.REMINDER_AGGREGATE_NOTIFICATION_ID
            )
        }

        // Send projects notification if there are incomplete projects
        if (incompleteProjects.isNotEmpty()) {
            val title = if (reminderType == REMINDER_6PM) "⏰ Project Targets" else "🌙 Project Reminders"
            val text = if (incompleteProjects.size == 1) {
                "${incompleteProjects[0].first}: Need ${incompleteProjects[0].second} more"
            } else {
                incompleteProjects.joinToString(", ") { "${it.first} (${it.second})" }
                    .let { "Projects needing work: $it" }
            }
            wakaNotificationManager.showReminderNotification(
                title,
                text,
                WakaHelpers.REMINDER_PROJECTS_NOTIFICATION_ID
            )
        }
    }

    private fun dailyTargetHit(aggregateData: AggregateData): Boolean {
        val today = LocalDate.now()
        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
        val todayStr = today.format(dateFormatter)
        val todaySeconds = aggregateData.dailyRecords[todayStr]?.totalSeconds ?: 0
        val targetSeconds = (aggregateData.dailyTargetHours!! * 3600).roundToInt()
        return todaySeconds >= targetSeconds
    }

    private fun dailyTargetHit(projectData: ProjectSpecificData): Boolean {
        val today = LocalDate.now()
        val dateFormatter = WakaHelpers.getYYYYMMDDDateFormatter()
        val todayStr = today.format(dateFormatter)
        val todaySeconds = projectData.dailyDurationInSeconds[todayStr] ?: 0
        val targetSeconds = (projectData.dailyTargetHours!! * 3600).roundToInt()
        return todaySeconds >= targetSeconds
    }

    private fun loadNotificationData(prefs: SharedPreferences): NotificationData {
        val notificationDataAdapter = moshi.adapter(NotificationData::class.java)
        val notificationDataString = prefs.getString(WakaHelpers.NOTIFICATION_DATA_KEY, null)

        return notificationDataString?.let {
            (runCatching { notificationDataAdapter.fromJson(it) }.getOrNull()
                ?: WakaHelpers.INITIAL_NOTIFICATION_DATA)
        } ?: WakaHelpers.INITIAL_NOTIFICATION_DATA
    }

    private fun updateReminderDate(
        prefs: SharedPreferences,
        notificationData: NotificationData,
        reminderType: String,
        today: LocalDate
    ) {
        val notificationDataAdapter = moshi.adapter(NotificationData::class.java)
        val updatedNotificationData = when (reminderType) {
            REMINDER_6PM -> notificationData.copy(
                last6pmReminderDate = today.toString(),
                reminder6pmShownToday = true
            )
            REMINDER_10PM -> notificationData.copy(
                last10pmReminderDate = today.toString(),
                reminder10pmShownToday = true
            )
            else -> notificationData
        }

        prefs.edit {
            putString(
                WakaHelpers.NOTIFICATION_DATA_KEY,
                notificationDataAdapter.toJson(updatedNotificationData)
            )
        }
    }

    private fun rescheduleReminder(reminderType: String) {
        val tomorrow = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(1)
        val hour = when (reminderType) {
            REMINDER_6PM -> WakaHelpers.REMINDER_6PM_HOUR
            REMINDER_10PM -> WakaHelpers.REMINDER_10PM_HOUR
            else -> return
        }

        val targetTime = tomorrow.withHour(hour).withMinute(0).withSecond(0)
        val delay = java.time.Duration.between(ZonedDateTime.now(ZoneId.systemDefault()), targetTime).toMillis()

        val workRequest = OneTimeWorkRequestBuilder<TargetReminderWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(REMINDER_TYPE_KEY to reminderType))
            .addTag("reminder_${reminderType}")
            .build()

        val workName = "reminder_${reminderType}"
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
