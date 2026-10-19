package com.usecase

import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.repository.ConverterRepository
import javax.inject.Inject

class StartConversion @Inject constructor(private val repository: ConverterRepository) {
    operator fun invoke(scope: ConversionScope, options: ConverterOptions) = repository.start(scope, options)
}
