package com.example.stride.domain.usecase

import com.example.stride.data.repository.GaitRepository
import com.example.stride.domain.model.TestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * A use case for retrieving all past test results.
 */
class GetTestHistoryUseCase @Inject constructor(
    private val gaitRepository: GaitRepository
) {
    operator fun invoke(): Flow<List<TestResult>> {
        // In the future, this will call a method on the repository
        // that retrieves all results from the local database.
        // For now, it returns an empty list.
        return flowOf(emptyList())
    }
}
