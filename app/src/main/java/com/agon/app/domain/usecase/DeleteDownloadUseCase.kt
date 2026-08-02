package com.agon.app.domain.usecase

import com.agon.app.data.DownloadRepository
import javax.inject.Inject

class DeleteDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(id: String) {
        repository.deleteDownload(id)
    }
}
