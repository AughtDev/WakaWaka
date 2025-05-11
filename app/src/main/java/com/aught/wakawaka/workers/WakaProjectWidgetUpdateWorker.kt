package com.aught.wakawaka.workers

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aught.wakawaka.widget.project.WakaProjectWidget

class WakaProjectWidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Update your widget here
        WakaProjectWidget().updateAll(applicationContext)
        return Result.success()
    }
}
