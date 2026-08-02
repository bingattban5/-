package com.agon.app.domain.usecase

import com.agon.app.data.DownloadItem
import com.agon.app.data.DownloadRepository
import javax.inject.Inject

class UpdateDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(item: DownloadItem) {
        repository.updateDownload(item)
    }
}
