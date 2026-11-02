package com.usecase

import com.repository.ReadAlongRepository
import javax.inject.Inject

/** Read-along was turned off: every transcript and every picked language goes. */
class ClearReadAlong @Inject constructor(private val repository: ReadAlongRepository) {
    suspend operator fun invoke() = repository.deleteAll()
}
