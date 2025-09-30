import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.NotificationData
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaStatistics
import com.aught.wakawaka.utils.JSONDateAdapter
import com.aught.wakawaka.utils.getMoshi
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.time.Duration

@JsonClass(generateAdapter = true)
data class BackupData(
    val aggregateData: AggregateData,
    val projectSpecificDataMap: Map<String, ProjectSpecificData>,
    val statistics: WakaStatistics,
    val notificationData: NotificationData,
    val wakatimeApi: String,
    val projectAssignedToWidget: String?
)

object BackupManager {

    private fun generateTimeStamp(): String {
        val currentTime = System.currentTimeMillis()
        return java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date(currentTime))
    }

    fun createBackup(context: Context): File? {
        val backupData = BackupData(
            aggregateData = WakaDataFetchWorker.loadAggregateData(context)
                ?: WakaHelpers.INITIAL_AGGREGATE_DATA,
            projectSpecificDataMap = WakaDataFetchWorker.loadProjectSpecificData(context),
            statistics = WakaDataFetchWorker.loadWakaStatistics(context),
            notificationData = WakaDataFetchWorker.loadNotificationData(context),
            wakatimeApi = WakaDataFetchWorker.loadWakatimeAPI(context),
            projectAssignedToWidget = WakaDataFetchWorker.loadProjectAssignedToWidget(context)
        )

        // convert to json
        val moshi = getMoshi()
        val adapter = moshi.adapter(BackupData::class.java)
        val jsonString = adapter.toJson(backupData)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        // save to file
        return try {
            val backupFile = File(downloadsDir, "wakawaka_backup_${generateTimeStamp()}.json")
            FileOutputStream(backupFile).use {
                it.write(jsonString.toByteArray())
                backupFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBackup(context: Context, backupFile: File?) {
        backupFile?.let {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "WakaWaka Backup")
//                putExtra(Intent.EXTRA_TEXT,"")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
        }
    }

    fun restoreBackup(context: Context, backupData: BackupData) {
        val moshi = getMoshi()
        val prefs =
            context.getSharedPreferences(WakaHelpers.Companion.PREFS, Context.MODE_PRIVATE)

        val aggregateDataAdapter = moshi.adapter(AggregateData::class.java)
        val notificationDataAdapter = moshi.adapter(NotificationData::class.java)
        val statisticsAdapter = moshi.adapter(WakaStatistics::class.java)

        WakaDataFetchWorker.saveProjectDataMap(context, backupData.projectSpecificDataMap)

        prefs.edit {
            putString(WakaHelpers.AGGREGATE_DATA_KEY, aggregateDataAdapter.toJson(backupData.aggregateData))
            putString(WakaHelpers.NOTIFICATION_DATA_KEY, notificationDataAdapter.toJson(backupData.notificationData))
            putString(WakaHelpers.WAKA_STATISTICS_KEY, statisticsAdapter.toJson(backupData.statistics))
            putString(WakaHelpers.WAKATIME_API, backupData.wakatimeApi)
            putString(WakaHelpers.PROJECT_ASSIGNED_TO_PROJECT_WIDGET, backupData.projectAssignedToWidget)
        }

        // start the worker to fetch data
        // Schedule the periodic
        val workRequest = PeriodicWorkRequestBuilder<WakaDataFetchWorker>(
            // every 15 minutes
            repeatInterval = Duration.ofMinutes(15),
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WakaWakaDataFetch",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )

    }
}
