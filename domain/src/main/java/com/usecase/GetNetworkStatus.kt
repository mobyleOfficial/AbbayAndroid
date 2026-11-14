package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class GetNetworkStatus @Inject constructor(private val repository: LibriVoxRepository) {
    operator fun invoke() = repository.networkStatus()
}
