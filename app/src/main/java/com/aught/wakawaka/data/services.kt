import retrofit2.http.GET
import retrofit2.http.Query

interface WakapiService {
    @GET("users/current/summaries")
    suspend fun getSummaries(
        @Query("range") range: String? = "Last 7 Days",
    ): SummariesResponse

}
