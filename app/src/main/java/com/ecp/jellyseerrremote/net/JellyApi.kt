package com.ecp.jellyseerrremote.net

import com.ecp.jellyseerrremote.data.CreateRequestDto
import com.ecp.jellyseerrremote.data.DiscoverResponseDto
import com.ecp.jellyseerrremote.data.SearchResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface JellyApi {
    @GET("/api/v1/status")
    suspend fun status(): Response<Unit>

    @GET("/api/v1/auth/me")
    suspend fun me(): Response<Any>

    @GET("/api/v1/search")
    suspend fun search(@Query("query") query: String): Response<SearchResponseDto>

    /** Seerr: discover movies (popular by default). */
    @GET("/api/v1/discover/movies")
    suspend fun discoverMovies(@Query("page") page: Int = 1): Response<DiscoverResponseDto>

    /** Seerr: discover TV (popular by default). */
    @GET("/api/v1/discover/tv")
    suspend fun discoverTv(@Query("page") page: Int = 1): Response<DiscoverResponseDto>

    /** Seerr: trending (movies + TV mixed). */
    @GET("/api/v1/discover/trending")
    suspend fun discoverTrending(@Query("page") page: Int = 1): Response<DiscoverResponseDto>

    @POST("/api/v1/request")
    suspend fun createRequest(@Body body: CreateRequestDto): Response<Any>
}
