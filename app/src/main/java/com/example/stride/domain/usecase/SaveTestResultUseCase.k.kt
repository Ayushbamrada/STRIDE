package com.example.stride.domain.usecase

import com.example.stride.data.repository.GaitRepository
import com.example.stride.domain.model.TestResult
import javax.inject.Inject

/**
 * A use case dedicated to saving a completed test result.
 * It will interact with the repository to persist the data.
 */
class SaveTestResultUseCase @Inject constructor(
    private val gaitRepository: GaitRepository
) {
    suspend operator fun invoke(result: TestResult) {
        // In the future, this will call a method on the repository
        // to save the result to a local database.
        // e.g., gaitRepository.saveTestResult(result)
        println("Simulating save of test result: $result")
    }
}
