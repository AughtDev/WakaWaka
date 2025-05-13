package com.aught.wakawaka.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WakaDataDumpWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return Result.success()
    }
}
