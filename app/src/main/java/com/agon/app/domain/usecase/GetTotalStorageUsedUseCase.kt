package com.agon.app.domain.usecase

import com.agon.app.data.AiModelRepository
import javax.inject.Inject

class GetTotalStorageUsedUseCase @Inject constructor(
    private val repository: AiModelRepository
) {
    suspend operator fun invoke(): Long {
        return repository.getTotalStorageUsed()
    }
}
