package com.mobyle.abbay.fakes

import com.converter.ConversionPlan
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.repository.ConverterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeConverterRepository : ConverterRepository {
    val started = mutableListOf<Pair<ConversionScope, ConverterOptions>>()
    val progress = MutableStateFlow(ConversionProgress())

    override fun encoderName(): String = "fake.opus.encoder"

    override suspend fun plan(scope: ConversionScope, options: ConverterOptions) = ConversionPlan(0, 0, 0)

    override fun start(scope: ConversionScope, options: ConverterOptions) {
        started += scope to options
    }

    override fun cancel() = Unit

    override fun observeProgress(): Flow<ConversionProgress> = progress

    override fun observeTotalSaved(): Flow<Long> = MutableStateFlow(0L)
}
