package com.aught.wakawaka.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

// region DATA CLASSES FOR DATA DUMPS

enum class DataDumpType(val type: String) {
    @Json(name = "daily")
    DAILY("daily"),

    @Json(name = "heartbeats")
    HEARTBEATS("heartbeats"),
}

enum class DataDumpStatus(val status: String) {
    @Json(name = "Pending...")
    PENDING("Pending..."),

    @Json(name = "Processing coding activity...")
    PROCESSING("Processing coding activity..."),

    @Json(name = "Uploading...")
    UPLOADING("Uploading..."),

    @Json(name = "Completed")
    COMPLETED("Completed"),
}

@JsonClass(generateAdapter = true)
data class DataDumpSpecs(
    val id: String,
    val type: DataDumpType,
    @Json(name = "download_url") val downloadUrl: String,
    val status: DataDumpStatus,
    @Json(name = "percent_complete") val percentComplete: Float,
    val expires: Date, // in the format YYYY-MM-DD
    @Json(name = "created_at") val createdAt: Date, // in the format YYYY-MM-DD
    @Json(name = "is_stuck") val isStuck: Boolean,
    @Json(name = "has_failed") val hasFailed: Boolean,
    @Json(name = "is_processing") val isProcessing: Boolean
)

@JsonClass(generateAdapter = true)
data class DataDumpsResponse(
    val data: List<DataDumpSpecs>,
    val total: Int,
    @Json(name = "total_pages") val totalPages: Int
)

@JsonClass(generateAdapter = true)
data class PostDataDumpRequest(
    val type: DataDumpType,
    @Json(name = "email_when_finished") val emailWhenFinished: Boolean,
)

// Main Data Dump Structure
@JsonClass(generateAdapter = true)
data class DataDump(
    @Json(name = "user") val user: DataDumpUser,
    @Json(name = "range") val range: DataDumpRange,
    @Json(name = "days") val days: List<DataDumpDailySummary>
)

@JsonClass(generateAdapter = true)
data class DataDumpUser(
    @Json(name = "bio") val bio: String?,
    @Json(name = "categories_used_public") val categoriesUsedPublic: Boolean,
    @Json(name = "city") val city: String?, // Assuming this can be a Location object if more complex
    @Json(name = "color_scheme") val colorScheme: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "date_format") val dateFormat: String,
    @Json(name = "default_dashboard_range") val defaultDashboardRange: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "durations_slice_by") val durationsSliceBy: String,
    @Json(name = "editors_used_public") val editorsUsedPublic: Boolean,
    @Json(name = "email") val email: String?, // Nullable based on some WakaTime contexts, though present here
    @Json(name = "full_name") val fullName: String?,
    @Json(name = "github_username") val githubUsername: String?,
    @Json(name = "has_basic_features") val hasBasicFeatures: Boolean,
    @Json(name = "has_premium_features") val hasPremiumFeatures: Boolean,
    @Json(name = "human_readable_website") val humanReadableWebsite: String?,
    @Json(name = "id") val id: String,
    @Json(name = "invoice_counter") val invoiceCounter: Int?, // Changed to Int? as it's 0 here
    @Json(name = "invoice_id_format") val invoiceIdFormat: String?,
    @Json(name = "is_email_confirmed") val isEmailConfirmed: Boolean,
    @Json(name = "is_email_public") val isEmailPublic: Boolean,
    @Json(name = "is_hireable") val isHireable: Boolean,
    @Json(name = "is_onboarding_finished") val isOnboardingFinished: Boolean,
    @Json(name = "languages_used_public") val languagesUsedPublic: Boolean,
    @Json(name = "last_branch") val lastBranch: String?,
    @Json(name = "last_heartbeat_at") val lastHeartbeatAt: String?,
    @Json(name = "last_language") val lastLanguage: String?,
    @Json(name = "last_plugin") val lastPlugin: String?,
    @Json(name = "last_plugin_name") val lastPluginName: String?,
    @Json(name = "last_project") val lastProject: String?,
    @Json(name = "linkedin_username") val linkedinUsername: String?,
    @Json(name = "location") val location: String?,
    @Json(name = "logged_time_public") val loggedTimePublic: Boolean,
    @Json(name = "modified_at") val modifiedAt: String?,
    @Json(name = "needs_payment_method") val needsPaymentMethod: Boolean,
    @Json(name = "os_used_public") val osUsedPublic: Boolean,
    @Json(name = "photo") val photo: String,
    @Json(name = "photo_public") val photoPublic: Boolean,
    @Json(name = "plan") val plan: String,
    @Json(name = "profile_url") val profileUrl: String,
    @Json(name = "profile_url_escaped") val profileUrlEscaped: String,
    @Json(name = "public_email") val publicEmail: String?,
    @Json(name = "public_profile_time_range") val publicProfileTimeRange: String,
    @Json(name = "share_all_time_badge") val shareAllTimeBadge: Boolean?, // Assuming can be boolean or null
    @Json(name = "share_last_year_days") val shareLastYearDays: Int?, // Assuming can be int or null
    @Json(name = "show_machine_name_ip") val showMachineNameIp: Boolean,
    @Json(name = "suggest_dangling_branches") val suggestDanglingBranches: Boolean,
    @Json(name = "time_format_24hr") val timeFormat24hr: Boolean?, // Assuming can be boolean or null
    @Json(name = "time_format_display") val timeFormatDisplay: String,
    @Json(name = "timeout") val timeout: Int,
    @Json(name = "timezone") val timezone: String,
    @Json(name = "twitter_username") val twitterUsername: String?,
    @Json(name = "username") val username: String?,
    @Json(name = "website") val website: String?,
    @Json(name = "weekday_start") val weekdayStart: Int,
    @Json(name = "wonderfuldev_username") val wonderfuldevUsername: String?,
    @Json(name = "writes_only") val writesOnly: Boolean
)

@JsonClass(generateAdapter = true)
data class DataDumpRange(
    @Json(name = "start") val start: Long, // Timestamps are usually Long
    @Json(name = "end") val end: Long
)

@JsonClass(generateAdapter = true)
data class DataDumpDailySummary(
    @Json(name = "categories") val categories: List<DailyCategorySummary>,
    @Json(name = "date") val date: Date,
    @Json(name = "dependencies") val dependencies: List<DailyDependencySummary>,
    @Json(name = "editors") val editors: List<DailyEditorSummary>,
    @Json(name = "grand_total") val grandTotal: DailyGrandTotalDMP,
    @Json(name = "languages") val languages: List<DailyLanguageSummary>,
    @Json(name = "machines") val machines: List<DailyMachineSummary>,
    @Json(name = "operating_systems") val operatingSystems: List<DailyOperatingSystemSummary>,
    @Json(name = "projects") val projects: List<DailyProjectSummary>
)

// Common structure for timed items (Categories, Dependencies, Editors, etc. at the daily level)
@JsonClass(generateAdapter = true)
data class DailyTimeStatItem(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyCategorySummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyDependencySummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyEditorSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyLanguageSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyMachineSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "machine_name_id") val machineNameId: String,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyOperatingSystemSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyGrandTotalDMP(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class DailyProjectSummary(
    @Json(name = "branches") val branches: List<ProjectBranchSummary>,
    @Json(name = "categories") val categories: List<ProjectCategorySummary>,
    @Json(name = "dependencies") val dependencies: List<ProjectDependencySummary>,
    @Json(name = "editors") val editors: List<ProjectEditorSummary>,
    @Json(name = "entities") val entities: List<ProjectEntitySummary>,
    @Json(name = "grand_total") val grandTotal: ProjectGrandTotalSummary,
    @Json(name = "languages") val languages: List<ProjectLanguageSummary>,
    @Json(name = "machines") val machines: List<ProjectMachineSummary>,
    @Json(name = "name") val name: String,
    @Json(name = "operating_systems") val operatingSystems: List<ProjectOperatingSystemSummary>
)

// Structures for items within a Project's daily summary
// These often mirror the DailyTimeStatItem structure
@JsonClass(generateAdapter = true)
data class ProjectBranchSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectCategorySummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectDependencySummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double, // Note: In your example, project dependencies had varying percentages
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectEditorSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectEntitySummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String, // This is often a file path
    @Json(name = "percent") val percent: Double,
    @Json(name = "project_root_count") val projectRootCount: Int?,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double,
    @Json(name = "type") val type: String // e.g., "file"
)

@JsonClass(generateAdapter = true)
data class ProjectGrandTotalSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "percent") val percent: Double,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectLanguageSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectMachineSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "machine_name_id") val machineNameId: String,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)

@JsonClass(generateAdapter = true)
data class ProjectOperatingSystemSummary(
    @Json(name = "decimal") val decimal: String,
    @Json(name = "digital") val digital: String,
    @Json(name = "hours") val hours: Int,
    @Json(name = "minutes") val minutes: Int,
    @Json(name = "name") val name: String,
    @Json(name = "percent") val percent: Double,
    @Json(name = "seconds") val seconds: Int,
    @Json(name = "text") val text: String,
    @Json(name = "total_seconds") val totalSeconds: Double
)


// endregion
