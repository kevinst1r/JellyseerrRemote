package com.ecp.jellyseerrremote.net

import com.ecp.jellyseerrremote.data.SeasonsSpec
import com.ecp.jellyseerrremote.data.SeasonsSpecJsonAdapter
import com.ecp.jellyseerrremote.data.SecurePrefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class LocalLoginRequest(val email: String, val password: String)

object Network {

    fun buildOkHttpNoAuth(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun buildOkHttp(prefs: SecurePrefs): OkHttpClient {
        val cookieInterceptor = Interceptor { chain ->
            val original: Request = chain.request()
            val cookie = prefs.cookieHeader.trim()
            val req = if (cookie.isNotEmpty()) {
                original.newBuilder().header("Cookie", cookie).build()
            } else original
            chain.proceed(req)
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(cookieInterceptor)
            .build()
    }

    private val moshi = Moshi.Builder()
        .add(SeasonsSpec::class.java, SeasonsSpecJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    fun localLoginRequestBody(email: String, password: String): okhttp3.RequestBody {
        val json = moshi.adapter(LocalLoginRequest::class.java).toJson(LocalLoginRequest(email, password))
        return json.toRequestBody("application/json; charset=utf-8".toMediaType())
    }

    fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        // Retrofit requires baseUrl end with /
        val fixed = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(fixed)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
