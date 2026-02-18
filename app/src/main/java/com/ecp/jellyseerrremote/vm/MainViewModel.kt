package com.ecp.jellyseerrremote.vm

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecp.jellyseerrremote.data.RemoteMode
import com.ecp.jellyseerrremote.data.SecurePrefs
import com.ecp.jellyseerrremote.data.SearchResult
import com.ecp.jellyseerrremote.repo.ConnectionResult
import com.ecp.jellyseerrremote.repo.JellyRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class DotStatus { GRAY, YELLOW, GREEN, RED }

enum class SearchStatus { IDLE, LOADING, RESULTS, EMPTY, ERROR }

sealed class LoginResult {
    data object Success : LoginResult()
    data class Failure(val message: String) : LoginResult()
}

sealed class RequestResult {
    /** Set immediately on request click; UI shows overlay + "Request Added" until cleared. */
    data object Loading : RequestResult()
    data object Success : RequestResult()
    data class Error(val message: String) : RequestResult()
}

data class UiState(
    val localUrl: String = "",
    val remoteEnabled: Boolean = false,
    val remoteMode: RemoteMode = RemoteMode.CLOUDFLARE,
    val tunnelId: String = "",
    val customRemoteUrl: String = "",
    val preferLocalFirst: Boolean = true,
    val derivedRemoteUrl: String = "",
    val dot: DotStatus = DotStatus.GRAY,
    val baseUsed: String = "",
    val isAuthed: Boolean = false,
    val lastError: String = "",
    /** When we tried preferred URL first and it failed (or we used fallback). */
    val connectionDetail: String = "",
    // Search
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val searchStatus: SearchStatus = SearchStatus.IDLE,
    val searchError: String = ""
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = SecurePrefs(app.applicationContext)
    private val repo = JellyRepo(prefs)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<UiState> = _state

    private var checkJob: Job? = null
    private var searchJob: Job? = null
    private val searchCache = mutableMapOf<String, List<SearchResult>>()
    private val _loginResult = MutableStateFlow<LoginResult?>(null)
    val loginResult: StateFlow<LoginResult?> = _loginResult
    private val _requestResult = MutableStateFlow<RequestResult?>(null)
    val requestResult: StateFlow<RequestResult?> = _requestResult
    private val minSearchChars = 2
    private val searchDebounceMs = 300L

    private fun loadInitialState(): UiState {
        return UiState(
            localUrl = prefs.localUrl,
            remoteEnabled = prefs.remoteEnabled,
            remoteMode = prefs.remoteMode,
            tunnelId = prefs.tunnelId,
            customRemoteUrl = prefs.customRemoteUrl,
            preferLocalFirst = prefs.preferLocalFirst,
            derivedRemoteUrl = repo.remoteBaseUrl()
        )
    }

    init {
        requestCheck()
    }

    fun setLocalUrl(v: String) {
        prefs.localUrl = v
        _state.value = _state.value.copy(localUrl = v.trim())
        requestCheckDebounced()
    }

    fun setRemoteEnabled(enabled: Boolean) {
        prefs.remoteEnabled = enabled
        _state.value = _state.value.copy(
            remoteEnabled = enabled,
            derivedRemoteUrl = repo.remoteBaseUrl()
        )
        requestCheckDebounced()
    }

    fun setRemoteMode(mode: RemoteMode) {
        prefs.remoteMode = mode
        _state.value = _state.value.copy(
            remoteMode = mode,
            derivedRemoteUrl = repo.remoteBaseUrl()
        )
        requestCheckDebounced()
    }

    fun setTunnelId(v: String) {
        prefs.tunnelId = v
        _state.value = _state.value.copy(
            tunnelId = v.trim(),
            derivedRemoteUrl = repo.remoteBaseUrl()
        )
        requestCheckDebounced()
    }

    fun setCustomRemoteUrl(v: String) {
        prefs.customRemoteUrl = v
        _state.value = _state.value.copy(
            customRemoteUrl = v.trim(),
            derivedRemoteUrl = repo.remoteBaseUrl()
        )
        requestCheckDebounced()
    }

    fun setPreferLocalFirst(prefer: Boolean) {
        prefs.preferLocalFirst = prefer
        _state.value = _state.value.copy(preferLocalFirst = prefer)
        requestCheckDebounced()
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            searchError = ""
        )
        if (query.length < minSearchChars) {
            searchJob?.cancel()
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                searchStatus = if (query.isEmpty()) SearchStatus.IDLE else SearchStatus.IDLE
            )
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(searchDebounceMs)
            val q = query.trim()
            if (q.length < minSearchChars) return@launch
            val baseUrl = _state.value.baseUsed.ifBlank { repo.resolveBaseUrl() }
            if (baseUrl.isBlank()) {
                _state.value = _state.value.copy(
                    searchStatus = SearchStatus.ERROR,
                    searchError = "Can't reach server"
                )
                return@launch
            }
            if (!_state.value.isAuthed && !hasAuthCookie()) {
                _state.value = _state.value.copy(
                    searchResults = emptyList(),
                    searchStatus = SearchStatus.ERROR,
                    searchError = "Login required"
                )
                return@launch
            }
            searchCache[q]?.let { cached ->
                _state.value = _state.value.copy(
                    searchResults = cached,
                    searchStatus = SearchStatus.RESULTS,
                    searchError = ""
                )
                return@launch
            }
            _state.value = _state.value.copy(searchStatus = SearchStatus.LOADING, searchError = "")
            repo.search(baseUrl, q)
                .onSuccess { list ->
                    searchCache[q] = list
                    _state.value = _state.value.copy(
                        searchResults = list,
                        searchStatus = if (list.isEmpty()) SearchStatus.EMPTY else SearchStatus.RESULTS,
                        searchError = ""
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        searchResults = emptyList(),
                        searchStatus = SearchStatus.ERROR,
                        searchError = e.message ?: "Can't reach server"
                    )
                }
        }
    }

    fun requestCheck() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.value = _state.value.copy(dot = DotStatus.YELLOW, lastError = "", connectionDetail = "")
            try {
                val result = repo.checkConnection()
                applyConnectionResult(result)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = _state.value.copy(
                    dot = DotStatus.RED,
                    baseUsed = _state.value.baseUsed,
                    lastError = e.message ?: "Connection check failed",
                    connectionDetail = ""
                )
            }
        }
    }

    private fun requestCheckDebounced() {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            _state.value = _state.value.copy(dot = DotStatus.YELLOW, lastError = "", connectionDetail = "")
            try {
                delay(450)
                val result = repo.checkConnection()
                applyConnectionResult(result)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.value = _state.value.copy(
                    dot = DotStatus.RED,
                    baseUsed = _state.value.baseUsed,
                    lastError = e.message ?: "Connection check failed"
                )
            }
        }
    }

    private fun applyConnectionResult(result: ConnectionResult) {
        _state.value = _state.value.copy(
            dot = if (result.ok) DotStatus.GREEN else DotStatus.RED,
            baseUsed = result.baseUrlUsed,
            isAuthed = result.isAuthed,
            lastError = result.lastError,
            connectionDetail = result.preferredAttemptMessage
        )
    }

    fun preferredBaseUrlForLogin(): String {
        val s = _state.value
        return when {
            s.baseUsed.isNotBlank() -> s.baseUsed
            prefs.localUrl.isNotBlank() -> prefs.localUrl.trim().trimEnd('/')
            repo.remoteBaseUrl().isNotBlank() -> repo.remoteBaseUrl().trim().trimEnd('/')
            repo.localBaseUrl().isNotBlank() -> repo.localBaseUrl().trim().trimEnd('/')
            else -> ""
        }
    }

    fun loginWithPassword(baseUrl: String, email: String, password: String) {
        if (baseUrl.isBlank()) {
            _loginResult.value = LoginResult.Failure("Set server URL in Settings first.")
            return
        }
        _loginResult.value = null
        viewModelScope.launch {
            _loginResult.value = repo.loginLocal(baseUrl, email.trim(), password).fold(
                onSuccess = { requestCheck(); LoginResult.Success },
                onFailure = { LoginResult.Failure(it.message ?: "Login failed") }
            )
        }
    }

    fun clearLoginResult() { _loginResult.value = null }

    fun requestMedia(result: SearchResult) {
        _requestResult.value = RequestResult.Loading
        viewModelScope.launch {
            val baseUrl = _state.value.baseUsed.ifBlank { repo.resolveBaseUrl() }
            if (baseUrl.isBlank()) {
                _requestResult.value = RequestResult.Error("Can't reach server")
                return@launch
            }
            if (!_state.value.isAuthed && !hasAuthCookie()) {
                _requestResult.value = RequestResult.Error("Login required")
                return@launch
            }
            repo.createRequest(baseUrl, result)
                .onSuccess {
                    _requestResult.value = RequestResult.Success
                    searchCache.remove(_state.value.searchQuery.trim())
                    setSearchQuery(_state.value.searchQuery)
                }
                .onFailure { e ->
                    _requestResult.value = RequestResult.Error(e.message ?: "Request failed")
                }
        }
    }

    fun clearRequestResult() { _requestResult.value = null }

    fun logout() {
        prefs.clearAuth()
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
        requestCheck()
    }

    fun hasAuthCookie(): Boolean = prefs.cookieHeader.isNotBlank()
}
