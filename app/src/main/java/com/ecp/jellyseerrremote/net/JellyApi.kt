package com.ecp.jellyseerrremote.net

import com.ecp.jellyseerrremote.data.CreateRequestDto
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

    @POST("/api/v1/request")
    suspend fun createRequest(@Body body: CreateRequestDto): Response<Any>
}
