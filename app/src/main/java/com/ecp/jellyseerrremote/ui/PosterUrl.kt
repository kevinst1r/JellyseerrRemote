package com.ecp.jellyseerrremote.ui

object PosterUrl {
    private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500"

    fun forPath(posterPath: String?): String? {
        if (posterPath.isNullOrBlank()) return null
        return if (posterPath.startsWith("http")) posterPath else "$TMDB_IMAGE_BASE$posterPath"
    }
}
