package com.usecase

import com.repository.DiagnosticsRepository
import javax.inject.Inject

class GetDiagnostics @Inject constructor(private val repository: DiagnosticsRepository) {
    suspend operator fun invoke() = repository.report()
}
