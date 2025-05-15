package com.aught.wakawaka.utils

import com.aught.wakawaka.data.WakaHelpers
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

class JSONDateAdapter : JsonAdapter<Date>() {
    private val yyyyMMddRegex = Regex("""\d{4}-\d{2}-\d{2}""")

    @FromJson
    override fun fromJson(reader: JsonReader): Date? {
        val dateAsString = reader.nextString()
        return try {
            when {
                yyyyMMddRegex.matches(dateAsString) -> {
                    Date.from(WakaHelpers.yyyyMMDDToDate(dateAsString).atStartOfDay().toInstant(ZoneOffset.UTC))
                }

                else -> {
                    Date.from(Instant.parse(dateAsString))
                }
            }
        } catch (e: Exception) {
            println("Error parsing date: $dateAsString, exception: $e")
            return null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value != null) {
            writer.value(value.toInstant().toString())
        }
    }
}
