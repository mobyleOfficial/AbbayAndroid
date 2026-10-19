package com.usecase

import com.model.DataResult
import com.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** "Change folder": the picked folder replaces every root, then the library is rescanned. */
class ReplaceLibraryRoots @Inject constructor(private val repository: LibraryRepository) {
    suspend operator fun invoke(treeUri: String): DataResult<Unit> {
        val previousRoots = repository.observeRoots().first()
        return when (val result = repository.addRoot(treeUri)) {
            is DataResult.Failure -> result
            is DataResult.Success -> {
                previousRoots.filter { it.id != result.value.id }.forEach { repository.removeRoot(it.id) }
                repository.requestFullScan()
                DataResult.Success(Unit)
            }
        }
    }
}
