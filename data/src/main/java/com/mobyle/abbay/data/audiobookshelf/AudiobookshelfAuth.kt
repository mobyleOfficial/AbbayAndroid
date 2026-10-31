package com.mobyle.abbay.data.audiobookshelf

import com.mobyle.abbay.data.librivox.HostAccess
import com.mobyle.abbay.data.librivox.HttpStatusException
import com.mobyle.abbay.data.librivox.LibriVoxHttp
import com.mobyle.abbay.data.librivox.withHeaders
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The session ended (signed out, or the server refused its tokens): the user signs in again. */
class AudiobookshelfSignedOutException : IOException("Signed out of Audiobookshelf")

/**
 * Requests on the signed-in server with its access token, renewed with the refresh token when the server refuses
 * it. A server that refuses the refresh token too ends the session.
 */
@Singleton
class AudiobookshelfAuth @Inject constructor(private val http: LibriVoxHttp, private val session: AudiobookshelfSession) {
    private val refreshLock = Any()

    fun <T> authorized(request: (serverUrl: String, access: HostAccess) -> T): T {
        val credentials = session.credentials() ?: throw AudiobookshelfSignedOutException()
        val first = runCatching { request(credentials.serverUrl, session.access(credentials)) }
        if ((first.exceptionOrNull() as? HttpStatusException)?.status != HTTP_UNAUTHORIZED) return first.getOrThrow()
        val renewed = refresh(credentials) ?: throw AudiobookshelfSignedOutException()
        return request(renewed.serverUrl, session.access(renewed))
    }

    /** What a download of the server's media needs, or null when signed out. */
    fun mediaAccess(): HostAccess? = session.credentials()?.let(session::access)

    /** After the server refused the token in [stale]: true when there is a newer one to try. */
    fun renew(stale: HostAccess): Boolean {
        val credentials = session.credentials() ?: return false
        if (stale.headers != session.access(credentials).headers) return true
        return refresh(credentials) != null
    }

    /** Renews the access token once for everyone holding [stale]. */
    private fun refresh(stale: AudiobookshelfSession.Credentials): AudiobookshelfSession.Credentials? = synchronized(refreshLock) {
        val current = session.credentials() ?: return null
        if (current.accessToken != stale.accessToken) return current
        val refreshToken = current.refreshToken ?: return null.also { session.expire() }
        val access = session.anonymousAccess(current.serverUrl).withHeaders(mapOf(REFRESH_HEADER to refreshToken))
        val answer = runCatching { http.postJson("${current.serverUrl}/auth/refresh", "{}", access) }
        val tokens = answer.getOrNull()?.let(AudiobookshelfJson::tokens)
        val isRefused = (answer.exceptionOrNull() as? HttpStatusException)?.status in REFUSED || (answer.isSuccess && tokens == null)
        when {
            tokens != null -> session.refreshed(tokens.accessToken, tokens.refreshToken)
            isRefused -> session.expire()
        }
        // Unreachable, the session stays: a later request renews it.
        tokens?.let { session.credentials() }
    }

    companion object {
        const val HTTP_UNAUTHORIZED = 401
        val REFUSED = setOf(400, 401, 403)
        private const val REFRESH_HEADER = "x-refresh-token"
    }
}
