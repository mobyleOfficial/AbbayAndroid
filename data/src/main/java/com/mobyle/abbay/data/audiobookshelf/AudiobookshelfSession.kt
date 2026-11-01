package com.mobyle.abbay.data.audiobookshelf

import android.content.Context
import androidx.core.content.edit
import com.mobyle.abbay.data.librivox.HostAccess
import com.model.AudiobookshelfAccount
import com.repository.RemoteMediaAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in Audiobookshelf server and its tokens. They live in their own private preferences file, which backups
 * leave out: a restored library keeps its books and asks to sign in again.
 */
@Singleton
class AudiobookshelfSession @Inject constructor(@ApplicationContext context: Context) : RemoteMediaAuth {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val lock = Any()

    @Volatile
    private var current: Credentials? = read()

    private val mutableAccount = MutableStateFlow(accountOf(current, preferences.getString(KEY_USERNAME, null)))
    val account: StateFlow<AudiobookshelfAccount?> = mutableAccount.asStateFlow()

    data class Credentials(val serverUrl: String, val accessToken: String, val refreshToken: String?)

    fun credentials(): Credentials? = current

    fun signedIn(serverUrl: String, username: String, accessToken: String, refreshToken: String?) = synchronized(lock) {
        preferences.edit {
            putString(KEY_SERVER, serverUrl)
            putString(KEY_USERNAME, username)
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
        }
        current = Credentials(serverUrl, accessToken, refreshToken)
        mutableAccount.value = AudiobookshelfAccount(serverUrl, username, isSignedIn = true)
    }

    fun refreshed(accessToken: String, refreshToken: String?) = synchronized(lock) {
        val credentials = current ?: return@synchronized
        val kept = refreshToken ?: credentials.refreshToken
        preferences.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, kept)
        }
        current = credentials.copy(accessToken = accessToken, refreshToken = kept)
    }

    /** Ends the session but remembers the server and user name, to sign in again. */
    fun expire() = synchronized(lock) {
        preferences.edit {
            remove(KEY_ACCESS)
            remove(KEY_REFRESH)
        }
        current = null
        mutableAccount.value = mutableAccount.value?.copy(isSignedIn = false)
    }

    fun signOut() = expire()

    fun access(credentials: Credentials? = current): HostAccess = ServerAccess(credentials?.serverUrl, credentials?.accessToken)

    fun anonymousAccess(serverUrl: String): HostAccess = ServerAccess(serverUrl, token = null)

    override fun headersFor(url: String): Map<String, String> {
        val credentials = current ?: return emptyMap()
        val target = runCatching { URL(url) }.getOrNull() ?: return emptyMap()
        return if (isSameServer(credentials.serverUrl, target)) bearer(credentials.accessToken) else emptyMap()
    }

    private fun read(): Credentials? {
        val server = preferences.getString(KEY_SERVER, null) ?: return null
        val access = preferences.getString(KEY_ACCESS, null) ?: return null
        return Credentials(server, access, preferences.getString(KEY_REFRESH, null))
    }

    private fun accountOf(credentials: Credentials?, username: String?): AudiobookshelfAccount? {
        val server = credentials?.serverUrl ?: preferences.getString(KEY_SERVER, null) ?: return null
        return AudiobookshelfAccount(server, username.orEmpty(), isSignedIn = credentials != null)
    }

    private class ServerAccess(private val serverUrl: String?, private val token: String?) : HostAccess {
        override val headers: Map<String, String> = token?.let(::bearer).orEmpty()

        override fun isAllowed(url: URL): Boolean = serverUrl != null && isSameServer(serverUrl, url)
    }

    companion object {
        private const val PREFERENCES = "audiobookshelf"
        private const val KEY_SERVER = "server"
        private const val KEY_USERNAME = "username"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"

        fun bearer(token: String) = mapOf("Authorization" to "Bearer $token")

        /** Same host and port, over http or https, under the server's path (it may sit behind a reverse proxy). */
        fun isSameServer(serverUrl: String, url: URL): Boolean {
            val server = runCatching { URL(serverUrl) }.getOrNull() ?: return false
            if (url.protocol != "http" && url.protocol != "https") return false
            if (!url.host.equals(server.host, ignoreCase = true)) return false
            val isSamePort = url.port == server.port
            val isUpgrade = server.protocol == "http" && url.protocol == "https" && url.port == -1
            if (!isSamePort && !isUpgrade) return false
            val basePath = server.path.trimEnd('/')
            return basePath.isEmpty() || url.path == basePath || url.path.startsWith("$basePath/")
        }
    }
}
