package com.usecase

import com.repository.ConverterRepository
import javax.inject.Inject

class ObserveConversion @Inject constructor(private val repository: ConverterRepository) {
    fun progress() = repository.observeProgress()

    fun totalSaved() = repository.observeTotalSaved()
}
