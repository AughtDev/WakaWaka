import com.aught.wakawaka.data.SummariesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WakaService {
    @GET("users/current/summaries")
    suspend fun getSummaries(
        @Query("range") range: String? = "Last 30 Days",
    ): SummariesResponse

}

