package com.aught.wakawaka.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WakaService {
    @GET("users/current/summaries")
    suspend fun getSummaries(
        @Query("range") range: String? = "Last 30 Days",
    ): SummariesResponse

    @GET("users/current/data_dumps")
    suspend fun getDataDumps(): DataDumpsResponse

    @POST("users/current/data_dumps")
    suspend fun postDataDumps(
        @Body request: PostDataDumpRequest
    ): DataDumpsResponse

}

