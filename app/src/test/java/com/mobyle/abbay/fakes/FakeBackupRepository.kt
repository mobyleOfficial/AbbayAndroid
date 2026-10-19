package com.mobyle.abbay.fakes

import com.model.BackupSummary
import com.model.DataResult
import com.repository.BackupRepository

class FakeBackupRepository : BackupRepository {
    var result: DataResult<BackupSummary> = DataResult.Success(BackupSummary(books = 0))
    val calls = mutableListOf<String>()

    override suspend fun export(destinationUri: String): DataResult<BackupSummary> = result.also { calls += "export($destinationUri)" }

    override suspend fun import(sourceUri: String): DataResult<BackupSummary> = result.also { calls += "import($sourceUri)" }
}
