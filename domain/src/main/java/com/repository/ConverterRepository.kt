package com.repository

import com.converter.ConversionPlan
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import kotlinx.coroutines.flow.Flow

interface ConverterRepository {
    /** The platform Opus encoder, or null: without one the converter is hidden. */
    fun encoderName(): String?

    suspend fun plan(scope: ConversionScope, options: ConverterOptions): ConversionPlan

    /** Starts the background job (WorkManager, foreground notification). */
    fun start(scope: ConversionScope, options: ConverterOptions)

    fun cancel()

    fun observeProgress(): Flow<ConversionProgress>

    /** Bytes saved by every conversion so far. */
    fun observeTotalSaved(): Flow<Long>
}
