package com.agon.app.domain.usecase

import com.agon.app.data.DownloadItem
import com.agon.app.data.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadItem>> {
        return repository.getAllDownloads()
    }
}
