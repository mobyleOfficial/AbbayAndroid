package com.mobyle.abbay.presentation.playback

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Runs [block] in this scope and exposes it as the future Media3's session callbacks expect. */
internal fun <T : Any> CoroutineScope.future(block: suspend () -> T): ListenableFuture<T> {
    val result = SettableFuture.create<T>()
    launch { runCatching { block() }.onSuccess { result.set(it) }.onFailure { result.setException(it) } }
    return result
}

/** One page of a browse result; Media3 passes `Int.MAX_VALUE` (or 0) for "everything". */
internal fun <T> List<T>.page(page: Int, pageSize: Int): List<T> =
    if (pageSize <= 0 || pageSize == Int.MAX_VALUE) this else drop(page * pageSize).take(pageSize)
