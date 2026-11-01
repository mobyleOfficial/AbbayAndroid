package com.repository

/** Headers a request to [url] needs to reach media on a server the user signed in to; empty for any other url. */
fun interface RemoteMediaAuth {
    fun headersFor(url: String): Map<String, String>
}
