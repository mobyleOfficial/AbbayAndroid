package com.usecase

import com.repository.ConverterRepository
import javax.inject.Inject

/** The platform Opus encoder, or null when the converter must stay hidden. */
class GetOpusEncoder @Inject constructor(private val repository: ConverterRepository) {
    operator fun invoke(): String? = repository.encoderName()
}
