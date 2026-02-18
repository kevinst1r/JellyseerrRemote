package com.ecp.jellyseerrremote.repo

import com.ecp.jellyseerrremote.data.CreateRequestDto
import com.ecp.jellyseerrremote.data.DiscoverResponseDto
import com.ecp.jellyseerrremote.data.RemoteMode
import com.ecp.jellyseerrremote.data.SeasonsSpec
import com.ecp.jellyseerrremote.data.SecurePrefs
import com.ecp.jellyseerrremote.data.SearchResponseDto
import com.ecp.jellyseerrremote.data.SearchResult
import com.ecp.jellyseerrremote.data.toSearchResult
import com.ecp.jellyseerrremote.net.JellyApi
import com.ecp.jellyseerrremote.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

enum class DiscoverCategory(val label: String) {
    TRENDING("Trending"),
    MOVIES("Popular Movies"),
    TV("Popular TV")
}

data class ConnectionResult(
    val ok: Boolean,
    val baseUrlUsed: String,
    val isAuthed: Boolean,
    val lastError: String = "",
    /** When we tried multiple URLs: message about preferred attempt (e.g. "Remote failed: …" or "Tried remote first: …"). */
    val preferredAttemptMessage: String = ""
)

class JellyRepo(private val prefs: SecurePrefs) {

    fun localBaseUrl(): String = prefs.localUrl.trim()

    /** Remote URL: Cloudflare tunnel or custom, only when remote enabled and mode matches. */
    fun remoteBaseUrl(): String {
        if (!prefs.remoteEnabled) return ""
        return when (prefs.remoteMode) {
            RemoteMode.CLOUDFLARE -> {
                val id = prefs.tunnelId.trim()
                if (id.isBlank()) "" else "https://$id.trycloudflare.com"
            }
            RemoteMode.CUSTOM -> prefs.customRemoteUrl.trim()
        }
    }

    /** Ordered list: [preferred, fallback] for connection attempts. Prefer local first when enabled. */
    fun baseUrlCandidates(): List<String> {
        val local = localBaseUrl()
        val remote = remoteBaseUrl()
        return if (prefs.preferLocalFirst) {
            buildList {
                if (local.isNotBlank()) add(local)
                if (remote.isNotBlank()) add(remote)
            }
        } else {
            buildList {
                if (remote.isNotBlank()) add(remote)
                if (local.isNotBlank()) add(local)
            }
        }
    }

    /** Resolve the base URL to use: try preferred first, then fallback. Caller should call checkConnection for actual status. */
    fun resolveBaseUrl(): String {
        val candidates = baseUrlCandidates()
        return candidates.firstOrNull()?.let { normalizeBase(it) } ?: ""
    }

    suspend fun checkConnection(): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            val candidates = baseUrlCandidates()
            if (candidates.isEmpty()) {
                return@withContext ConnectionResult(false, "", false, "Set Local or Remote URL in Settings")
            }

            var lastError = ""
            var preferredAttemptMessage = ""

            for ((index, base) in candidates.withIndex()) {
                val normalizedBase = normalizeBase(base)
                val client = Network.buildOkHttp(prefs)
                val api = Network.buildRetrofit(base, client).create(JellyApi::class.java)
                try {
                    val statusResp = api.status()
                    if (statusResp.isSuccessful) {
                        val meResp = try { api.me() } catch (_: Exception) { null }
                        val authed = meResp?.isSuccessful == true
                        return@withContext ConnectionResult(
                            ok = true,
                            baseUrlUsed = normalizedBase,
                            isAuthed = authed,
                            lastError = "",
                            preferredAttemptMessage = preferredAttemptMessage.trim() + (if (preferredAttemptMessage.isNotBlank()) " Using fallback." else "")
                        )
                    }
                    lastError = "API returned ${statusResp.code()}"
                } catch (e: Exception) {
                    lastError = e.message ?: "Connection failed"
                }

                // Fallback: some instances use different API paths; try root URL to confirm server is reachable
                try {
                    val rootUrl = if (base.endsWith("/")) base else "$base/"
                    val request = Request.Builder().url(rootUrl).build()
                    val response = client.newCall(request).execute()
                    val responseCode = response.code
                    if (response.isSuccessful || responseCode in 300..399) {
                        return@withContext ConnectionResult(
                            ok = true,
                            baseUrlUsed = normalizedBase,
                            isAuthed = false,
                            lastError = "",
                            preferredAttemptMessage = preferredAttemptMessage.trim() + (if (preferredAttemptMessage.isNotBlank()) " Using fallback." else "")
                        )
                    }
                    lastError = "Server returned $responseCode"
                } catch (e: Exception) {
                    lastError = e.message ?: "Unable to reach server"
                }

                // First (preferred) attempt failed; record it so UI can show "tried X first"
                if (index == 0) {
                    val friendlyError = friendlyRemoteError(lastError)
                    preferredAttemptMessage = "Preferred ($normalizedBase) failed: $friendlyError."
                }
            }

            val usedBase = candidates.firstOrNull()?.let { normalizeBase(it) } ?: ""
            val finalMessage = if (preferredAttemptMessage.isNotBlank())
                "$preferredAttemptMessage Then: $lastError"
            else
                lastError
            return@withContext ConnectionResult(false, usedBase, false, finalMessage, "")
        } catch (e: Exception) {
            return@withContext ConnectionResult(
                false,
                "",
                false,
                e.message ?: "Connection check failed",
                ""
            )
        }
    }

    /** Sign in with Seerr email/password (local login). Saves session cookie to prefs on success. */
    suspend fun loginLocal(baseUrl: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        val base = baseUrl.trim().trimEnd('/')
        if (base.isBlank()) return@withContext Result.failure(Exception("No server URL"))
        val url = "$base/api/v1/auth/local"
        val client = Network.buildOkHttpNoAuth()
        val body = Network.localLoginRequestBody(email, password)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val msg = response.body?.string()?.take(200) ?: response.message
                return@withContext Result.failure(Exception("Login failed: ${response.code} $msg"))
            }
            val setCookieHeaders = response.headers("Set-Cookie")
            val cookieParts = setCookieHeaders.mapNotNull { header ->
                header.substringBefore(";").trim().takeIf { it.isNotEmpty() }
            }
            val cookieHeader = cookieParts.joinToString("; ")
            if (cookieHeader.isNotEmpty()) {
                prefs.cookieHeader = cookieHeader
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val message = friendlyRemoteError(e.message ?: "Connection failed")
            Result.failure(Exception(message))
        }
    }

    suspend fun search(baseUrl: String, query: String): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext Result.failure(Exception("No server URL"))
        val client = Network.buildOkHttp(prefs)
        val api = Network.buildRetrofit(baseUrl, client).create(JellyApi::class.java)
        try {
            val resp = api.search(query)
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Search failed: ${resp.code()}"))
            }
            val body = resp.body() ?: SearchResponseDto()
            val list = (body.results ?: emptyList()) +
                (body.movies ?: emptyList()) +
                (body.tv ?: emptyList())
            val results = list.map { it.toSearchResult() }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Fetch discover list for a category. page is 1-based. */
    suspend fun discover(baseUrl: String, category: DiscoverCategory, page: Int = 1): Result<List<SearchResult>> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext Result.failure(Exception("No server URL"))
        val client = Network.buildOkHttp(prefs)
        val api = Network.buildRetrofit(baseUrl, client).create(JellyApi::class.java)
        try {
            val resp = when (category) {
                DiscoverCategory.TRENDING -> api.discoverTrending(page)
                DiscoverCategory.MOVIES -> api.discoverMovies(page)
                DiscoverCategory.TV -> api.discoverTv(page)
            }
            if (!resp.isSuccessful) {
                return@withContext Result.failure(Exception("Discover failed: ${resp.code()}"))
            }
            val body = resp.body() ?: DiscoverResponseDto()
            val raw = body.results ?: emptyList()
            // Trending can include Person/Collection; only show movie/tv
            val list = raw.filter { it.mediaType?.lowercase() in listOf("movie", "tv") }
            Result.success(list.map { it.toSearchResult() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Create a media request (movie or TV). For TV, requests all seasons. */
    suspend fun createRequest(baseUrl: String, result: SearchResult): Result<Unit> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext Result.failure(Exception("No server URL"))
        val client = Network.buildOkHttp(prefs)
        val api = Network.buildRetrofit(baseUrl, client).create(JellyApi::class.java)
        val body = CreateRequestDto(
            mediaType = result.mediaType,
            mediaId = result.tmdbId,
            seasons = if (result.mediaType == "tv") SeasonsSpec.All else null
        )
        try {
            val resp = api.createRequest(body)
            when {
                resp.isSuccessful -> Result.success(Unit)
                resp.code() == 409 -> Result.failure(Exception("Already requested"))
                resp.code() == 403 -> Result.failure(Exception(resp.message() ?: "Not allowed to request"))
                else -> Result.failure(Exception(resp.message() ?: "Request failed: ${resp.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeBase(base: String): String = base.trim().trimEnd('/')

    /** Turn DNS/network errors into a short message with a hint. */
    private fun friendlyRemoteError(raw: String): String {
        val lower = raw.lowercase()
        return when {
            "failed to connect" in lower || "unable to resolve host" in lower ->
                "Can't reach server. If using a local address (e.g. 192.168.x.x), ensure this device is on the same Wi‑Fi as the server, or use a Remote URL in Settings (e.g. Cloudflare tunnel)."
            "resolve host" in lower || "no address associated" in lower || "unknown host" in lower ->
                "can't resolve host. Is the Cloudflare tunnel running? Is this device on a network that can reach the internet?"
            "timeout" in lower || "timed out" in lower ->
                "connection timed out. If using a local address, ensure same Wi‑Fi; otherwise tunnel may be down or network blocking."
            "connection refused" in lower ->
                "connection refused. Server may not be running or not listening on that port."
            else -> raw
        }
    }
}
