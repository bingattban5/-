package com.agon.app.domain.usecase

import com.agon.app.data.DownloadItem
import com.agon.app.data.DownloadRepository
import javax.inject.Inject

class AddDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(item: DownloadItem) {
        repository.addDownload(item)
    }
}
