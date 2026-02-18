package com.ecp.jellyseerrremote.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.io.IOException

/** Nested media info from Jellyseerr; status is typically int (4 = available in library). */
data class MediaInfoDto(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "statusCode") val statusCode: Int? = null
)

/** Single search result item (movie or TV). */
data class SearchResultDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "tmdbId") val tmdbId: Int? = null,
    @Json(name = "mediaType") val mediaType: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "firstAirDate") val firstAirDate: String? = null,
    @Json(name = "posterPath") val posterPath: String? = null,
    @Json(name = "status") val status: Int? = null,
    @Json(name = "mediaInfo") val mediaInfo: MediaInfoDto? = null
)

/** Seasons for TV request: all seasons or a specific list. Serializes as "all" or [1,2,3,...]. */
sealed class SeasonsSpec {
    data object All : SeasonsSpec()
    data class Some(val numbers: List<Int>) : SeasonsSpec()
}

/** Body for POST /api/v1/request (Jellyseerr/Seerr create request). */
data class CreateRequestDto(
    @Json(name = "mediaType") val mediaType: String,
    @Json(name = "mediaId") val mediaId: Int,
    @Json(name = "seasons") val seasons: SeasonsSpec? = null
)

/** Moshi adapter so seasons serializes as JSON "all" or [1,2,3]. */
class SeasonsSpecJsonAdapter : JsonAdapter<SeasonsSpec>() {
    @Throws(IOException::class)
    override fun fromJson(reader: JsonReader): SeasonsSpec {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> {
                reader.nextString()
                SeasonsSpec.All
            }
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<Int>()
                reader.beginArray()
                while (reader.hasNext()) list.add(reader.nextInt())
                reader.endArray()
                SeasonsSpec.Some(list)
            }
            else -> throw IOException("Expected string \"all\" or array of season numbers")
        }
    }

    @Throws(IOException::class)
    override fun toJson(writer: JsonWriter, value: SeasonsSpec?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        when (value) {
            is SeasonsSpec.All -> writer.value("all")
            is SeasonsSpec.Some -> {
                writer.beginArray()
                value.numbers.forEach { writer.value(it.toLong()) }
                writer.endArray()
            }
        }
    }
}

/** Wrapper for search API response. */
data class SearchResponseDto(
    @Json(name = "results") val results: List<SearchResultDto>? = null,
    @Json(name = "movies") val movies: List<SearchResultDto>? = null,
    @Json(name = "tv") val tv: List<SearchResultDto>? = null
)

/** UI-friendly search result. */
data class SearchResult(
    val id: String,
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val year: String,
    val posterPath: String?,
    val statusLabel: String,
    val isInLibrary: Boolean = false,
    /** True when already available or already requested — show green check and do not allow request. */
    val isRequestedOrInLibrary: Boolean = false
)

/** True if this item is already in the Plex/library (available). */
private fun SearchResultDto.isAvailable(): Boolean {
    val s = status ?: mediaInfo?.status ?: mediaInfo?.statusCode ?: return false
    return when (s) {
        1, 2, 4 -> true   // Available / PartiallyAvailable in Jellyseerr/Overseerr enums
        3, 5 -> false    // Pending, Declined/Failed
        else -> false
    }
}

fun SearchResultDto.toSearchResult(): SearchResult {
    val tid = tmdbId ?: id ?: 0
    val type = mediaType?.lowercase() ?: "movie"
    val t = title ?: name ?: ""
    val date = releaseDate ?: firstAirDate ?: ""
    val year = date.take(4).takeIf { it.length == 4 } ?: ""
    val available = isAvailable()
    val requested = run {
        val s = status ?: mediaInfo?.status ?: mediaInfo?.statusCode
        s == 3 || s == 5
    }
    val statusLabel = when {
        available -> "Available"
        requested -> "Requested"
        else -> "Unknown"
    }
    return SearchResult(
        id = "${type}_$tid",
        tmdbId = tid,
        mediaType = type,
        title = t,
        year = year,
        posterPath = posterPath,
        statusLabel = statusLabel,
        isInLibrary = available,
        isRequestedOrInLibrary = available || requested
    )
}
