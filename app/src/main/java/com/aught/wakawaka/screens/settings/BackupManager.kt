import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aught.wakawaka.data.AggregateData
import com.aught.wakawaka.data.NotificationData
import com.aught.wakawaka.data.ProjectSpecificData
import com.aught.wakawaka.data.WakaHelpers
import com.aught.wakawaka.data.WakaStatistics
import com.aught.wakawaka.utils.JSONDateAdapter
import com.aught.wakawaka.workers.WakaDataFetchWorker
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream

@JsonClass(generateAdapter = true)
data class BackupData(
    val aggregateData: AggregateData,
    val projectSpecificData: Map<String, ProjectSpecificData>,
    val statistics: WakaStatistics,
    val notificationData: NotificationData,
    val wakatimeApi: String,
)

object BackupManager {

    fun createBackup(context: Context): File? {
        val backupData = BackupData(
            aggregateData = WakaDataFetchWorker.loadAggregateData(context)
                ?: WakaHelpers.INITIAL_AGGREGATE_DATA,
            projectSpecificData = WakaDataFetchWorker.loadProjectSpecificData(context),
            statistics = WakaDataFetchWorker.loadWakaStatistics(context),
            notificationData = WakaDataFetchWorker.loadNotificationData(context),
            wakatimeApi = WakaDataFetchWorker.loadWakatimeAPI(context)
        )

        // convert to json
        val moshi = Moshi.Builder()
            .add(JSONDateAdapter())
            .addLast(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(BackupData::class.java)
        val jsonString = adapter.toJson(backupData)

        // save to file
        return try {
            val backupFile = File(context.filesDir, "wakawaka_backup.json")
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
}
