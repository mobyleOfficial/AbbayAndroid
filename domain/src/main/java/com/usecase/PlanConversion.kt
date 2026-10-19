package com.usecase

import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.repository.ConverterRepository
import javax.inject.Inject

class PlanConversion @Inject constructor(private val repository: ConverterRepository) {
    suspend operator fun invoke(scope: ConversionScope, options: ConverterOptions) = repository.plan(scope, options)
}
