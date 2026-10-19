package com.usecase

import com.repository.ConverterRepository
import javax.inject.Inject

class CancelConversion @Inject constructor(private val repository: ConverterRepository) {
    operator fun invoke() = repository.cancel()
}
