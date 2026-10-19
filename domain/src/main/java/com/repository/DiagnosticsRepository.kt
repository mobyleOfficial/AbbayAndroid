package com.repository

import com.diagnostics.DiagnosticsReport

interface DiagnosticsRepository {
    suspend fun report(): DiagnosticsReport
}
