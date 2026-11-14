package com.usecase

import com.repository.ConverterRepository
import javax.inject.Inject

class GetOpusEncoder @Inject constructor(private val repository: ConverterRepository) {
    operator fun invoke(): String? = repository.encoderName()
}
