package com.mobyle.abbay.data.audiobookshelf

import com.mobyle.abbay.data.librivox.HostAccess
import com.mobyle.abbay.data.librivox.HttpStatusException
import com.mobyle.abbay.data.librivox.LibriVoxHttp
import com.mobyle.abbay.data.librivox.withHeaders
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfSignIn
import com.model.AudiobookshelfSignInError
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Finds the Audiobookshelf server at the address the user typed and signs in to it. Blocking. */
@Singleton
class AudiobookshelfSignInClient @Inject constructor(private val http: LibriVoxHttp, private val session: AudiobookshelfSession) {
    private enum class Reach { SERVER, NOT_A_SERVER, UNREACHABLE }

    private sealed interface Probe {
        data class Answered(val body: String) : Probe

        data object NotFound : Probe

        data object Refused : Probe

        data object Unreachable : Probe
    }

    fun signIn(typedServer: String, username: String, password: String): AudiobookshelfSignIn {
        val candidates = AudiobookshelfUrls.candidates(typedServer)
        if (candidates.isEmpty() || username.isBlank()) return AudiobookshelfSignIn.Failure(AudiobookshelfSignInError.INVALID_ADDRESS)
        val reaches = mutableListOf<Reach>()
        val serverUrl = candidates.firstOrNull { candidate -> reach(candidate).also(reaches::add) == Reach.SERVER }
        return when {
            serverUrl != null -> logIn(serverUrl, username.trim(), password)
            Reach.NOT_A_SERVER in reaches -> AudiobookshelfSignIn.Failure(AudiobookshelfSignInError.NOT_A_SERVER)
            else -> AudiobookshelfSignIn.Failure(AudiobookshelfSignInError.UNREACHABLE)
        }
    }

    /** Whether Audiobookshelf answers at [serverUrl]: its status page, or the ping of older servers. */
    private fun reach(serverUrl: String): Reach {
        val access = session.anonymousAccess(serverUrl)
        return when (val status = probe(serverUrl + STATUS_PATH, access)) {
            is Probe.Answered -> if (AudiobookshelfJson.isServerStatus(status.body)) Reach.SERVER else Reach.NOT_A_SERVER
            Probe.NotFound -> when (val ping = probe(serverUrl + PING_PATH, access)) {
                is Probe.Answered -> if (AudiobookshelfJson.isPing(ping.body)) Reach.SERVER else Reach.NOT_A_SERVER
                Probe.Unreachable -> Reach.UNREACHABLE
                Probe.NotFound, Probe.Refused -> Reach.NOT_A_SERVER
            }
            Probe.Refused -> Reach.NOT_A_SERVER
            Probe.Unreachable -> Reach.UNREACHABLE
        }
    }

    @Suppress("SwallowedException")
    private fun probe(url: String, access: HostAccess): Probe = try {
        Probe.Answered(http.getText(url, access = access, timeoutMs = REACH_TIMEOUT_MS))
    } catch (status: HttpStatusException) {
        if (status.status == HTTP_NOT_FOUND) Probe.NotFound else Probe.Refused
    } catch (unreachable: IOException) {
        Probe.Unreachable
    }

    private fun logIn(serverUrl: String, username: String, password: String): AudiobookshelfSignIn {
        val json = buildJsonObject {
            put("username", username)
            put("password", password)
        }.toString()
        val access = session.anonymousAccess(serverUrl).withHeaders(mapOf(RETURN_TOKENS_HEADER to "true"))
        val answer = runCatching { http.postJson("$serverUrl/login", json, access) }
        val tokens = answer.getOrNull()?.let(AudiobookshelfJson::tokens)
        return if (tokens != null) {
            session.signedIn(serverUrl, username, tokens.accessToken, tokens.refreshToken)
            AudiobookshelfSignIn.Success(AudiobookshelfAccount(serverUrl, username, isSignedIn = true))
        } else {
            AudiobookshelfSignIn.Failure(errorOf(answer.exceptionOrNull()))
        }
    }

    private fun errorOf(error: Throwable?): AudiobookshelfSignInError = when {
        error is HttpStatusException && error.status in AudiobookshelfAuth.REFUSED -> AudiobookshelfSignInError.WRONG_CREDENTIALS
        error == null || error is HttpStatusException -> AudiobookshelfSignInError.NOT_A_SERVER
        else -> AudiobookshelfSignInError.UNREACHABLE
    }

    private companion object {
        const val STATUS_PATH = "/status"
        const val PING_PATH = "/ping"
        const val RETURN_TOKENS_HEADER = "x-return-tokens"
        const val REACH_TIMEOUT_MS = 10_000
        const val HTTP_NOT_FOUND = 404
    }
}
