package com.usecase

import com.repository.LibriVoxRepository
import javax.inject.Inject

class GetPopularLibriVoxBooks @Inject constructor(private val repository: LibriVoxRepository) {
    suspend operator fun invoke() = repository.popular()
}
