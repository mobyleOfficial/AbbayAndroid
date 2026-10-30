package com.repository

import com.converter.ConversionPlan
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import kotlinx.coroutines.flow.Flow

interface ConverterRepository {
    fun encoderName(): String?

    suspend fun plan(scope: ConversionScope, options: ConverterOptions): ConversionPlan

    fun start(scope: ConversionScope, options: ConverterOptions)

    fun cancel()

    fun observeProgress(): Flow<ConversionProgress>

    fun observeTotalSaved(): Flow<Long>
}
